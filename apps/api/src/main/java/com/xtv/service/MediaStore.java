package com.xtv.service;

import com.xtv.config.XtvProperties;
import com.xtv.domain.PlatformConfig;
import com.xtv.domain.VideoAsset;
import com.xtv.domain.Vocab;
import com.xtv.repo.PlatformConfigRepository;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class MediaStore {
  private final XtvProperties properties;
  private final PlatformConfigRepository configs;

  public MediaStore(XtvProperties properties, PlatformConfigRepository configs) {
    this.properties = properties;
    this.configs = configs;
  }

  public Path root() {
    return Path.of(properties.getStorageRoot()).toAbsolutePath().normalize();
  }

  public boolean objectMode() {
    return config().getStorageMode() == Vocab.StorageMode.OBJECT;
  }

  public boolean objectConfigured() {
    S3Settings s3 = s3();
    return !s3.endpoint().isBlank() && !s3.bucket().isBlank()
        && !s3.accessKey().isBlank() && !s3.secretKey().isBlank();
  }

  public S3Settings s3() {
    PlatformConfig cfg = config();
    String region = text(cfg.getS3Region());
    return new S3Settings(text(cfg.getS3Endpoint()), region.isBlank() ? "us-east-1" : region,
        text(cfg.getS3Bucket()), text(cfg.getS3AccessKey()), text(cfg.getS3SecretKey()));
  }

  private static String text(String value) {
    return value == null ? "" : value.trim();
  }

  public PlatformConfig config() {
    return configs.findById(1L).orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "缺少平台配置"));
  }

  public Path publicFile(String key) {
    Path path = root().resolve("public").resolve(key).normalize();
    if (!path.startsWith(root().resolve("public"))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "非法路径");
    }
    return path;
  }

  public Path hlsDir(Long videoId) {
    return publicFile(String.valueOf(videoId));
  }

  public String originalKey(Long videoId) {
    return "originals/" + videoId + "/source.mp4";
  }

  public void deleteVideoMedia(Long videoId) {
    deleteTree(root().resolve("originals").resolve(String.valueOf(videoId)));
    deleteTree(hlsDir(videoId));
    try {
      Files.deleteIfExists(publicFile("covers/" + videoId + ".jpg"));
    } catch (IOException ignored) {
      // 封面不存在时不影响删除影片记录
    }
    if (objectConfigured()) {
      deleteObjectPrefix(originalKey(videoId).replace("/source.mp4", "/"));
      deleteObjectPrefix(videoId + "/");
      deleteObject("covers/" + videoId + ".jpg");
    }
  }

  public InputStream openObject(String key) {
    requireObject();
    S3Client client = client();
    return client.getObject(GetObjectRequest.builder().bucket(s3().bucket()).key(key).build());
  }

  private void deleteObject(String key) {
    try (S3Client client = client()) {
      client.deleteObject(b -> b.bucket(s3().bucket()).key(key));
    } catch (Exception ignored) {
      // 对象不存在时跳过
    }
  }

  private void deleteObjectPrefix(String prefix) {
    try (S3Client client = client()) {
      var listed = client.listObjectsV2(b -> b.bucket(s3().bucket()).prefix(prefix));
      if (listed.contents() == null) {
        return;
      }
      for (var obj : listed.contents()) {
        client.deleteObject(b -> b.bucket(s3().bucket()).key(obj.key()));
      }
    } catch (Exception ignored) {
      // 清理失败不阻断业务删除
    }
  }

  private void deleteTree(Path path) {
    Path normalized = path.toAbsolutePath().normalize();
    if (!normalized.startsWith(root()) || !Files.exists(normalized)) {
      return;
    }
    try (Stream<Path> walk = Files.walk(normalized)) {
      walk.sorted(java.util.Comparator.reverseOrder()).forEach(file -> {
        try {
          Files.deleteIfExists(file);
        } catch (IOException ignored) {
          // 残留文件不阻断后台删除
        }
      });
    } catch (IOException ignored) {
      // 目录不存在时跳过
    }
  }

  public SavedFile saveOriginal(Long videoId, InputStream input) throws IOException {
    Path path = root().resolve(originalKey(videoId));
    Files.createDirectories(path.getParent());
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (Exception e) {
      throw new IOException(e);
    }
    long size = 0;
    try (InputStream in = input; var out = Files.newOutputStream(path)) {
      byte[] buf = new byte[8192];
      int n;
      while ((n = in.read(buf)) >= 0) {
        out.write(buf, 0, n);
        digest.update(buf, 0, n);
        size += n;
      }
    }
    if (objectMode()) {
      putObject(originalKey(videoId), path);
    }
    return new SavedFile(originalKey(videoId), size, HexFormat.of().formatHex(digest.digest()));
  }

  public void writePublic(String key, InputStream input) throws IOException {
    Path path = publicFile(key);
    Files.createDirectories(path.getParent());
    Files.copy(input, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    if (objectMode()) {
      putObject(key, path);
    }
  }

  public Path materializeOriginal(VideoAsset asset) throws IOException {
    Path local = root().resolve(asset.getStorageKey());
    if (Files.exists(local)) {
      return local;
    }
    if (!objectMode()) {
      throw new IOException("找不到原片");
    }
    Files.createDirectories(local.getParent());
    try (S3Client client = client(); var in = client.getObject(GetObjectRequest.builder()
        .bucket(s3().bucket())
        .key(asset.getStorageKey())
        .build())) {
      Files.copy(in, local, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
    return local;
  }

  public void uploadPublicTree(Long videoId) throws IOException {
    if (!objectMode()) {
      return;
    }
    uploadTree(hlsDir(videoId), videoId + "/");
    Path cover = publicFile("covers/" + videoId + ".jpg");
    if (Files.exists(cover)) {
      putObject("covers/" + videoId + ".jpg", cover);
    }
  }

  public long objectSize(String key) {
    requireObject();
    try (S3Client client = client()) {
      var head = client.headObject(HeadObjectRequest.builder().bucket(s3().bucket()).key(key).build());
      return head.contentLength() == null ? 0 : head.contentLength();
    } catch (S3Exception ex) {
      if (ex.statusCode() == 404) {
        return 0;
      }
      throw ex;
    }
  }

  public String presignOriginalPut(Long videoId) {
    requireObject();
    String key = originalKey(videoId);
    try (S3Presigner presigner = presigner()) {
      var put = PutObjectRequest.builder().bucket(s3().bucket()).key(key).build();
      var presign = presigner.presignPutObject(PutObjectPresignRequest.builder()
          .signatureDuration(Duration.ofMinutes(30))
          .putObjectRequest(put)
          .build());
      return presign.url().toString();
    }
  }

  private void uploadTree(Path dir, String prefix) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    try (Stream<Path> walk = Files.walk(dir)) {
      for (Path file : walk.filter(Files::isRegularFile).toList()) {
        String rel = dir.relativize(file).toString().replace('\\', '/');
        putObject(prefix + rel, file);
      }
    }
  }

  private void putObject(String key, Path file) {
    requireObject();
    try (S3Client client = client()) {
      client.putObject(PutObjectRequest.builder().bucket(s3().bucket()).key(key).build(),
          RequestBody.fromFile(file));
    }
  }

  private void requireObject() {
    if (!objectConfigured()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "对象存储未配置");
    }
  }

  private S3Client client() {
    S3Settings s3 = s3();
    return S3Client.builder()
        .endpointOverride(URI.create(s3.endpoint()))
        .region(Region.of(s3.region()))
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(s3.accessKey(), s3.secretKey())))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }

  private S3Presigner presigner() {
    S3Settings s3 = s3();
    return S3Presigner.builder()
        .endpointOverride(URI.create(s3.endpoint()))
        .region(Region.of(s3.region()))
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(s3.accessKey(), s3.secretKey())))
        .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }

  public record S3Settings(String endpoint, String region, String bucket, String accessKey, String secretKey) {}

  public record SavedFile(String key, long size, String checksum) {}
}
