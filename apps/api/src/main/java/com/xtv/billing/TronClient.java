package com.xtv.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtv.config.XtvProperties;
import com.xtv.domain.Vocab;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TronClient {
  public static final String USDT_CONTRACT = "TR7NHqjeKQxGTCi8q8ZY4pL8otSzgjLj6t";

  private final XtvProperties properties;
  private final ObjectMapper json;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
  private volatile Instant cachedSolidBefore = Instant.EPOCH;
  private volatile long cachedSolidAtMs;

  public TronClient(XtvProperties properties, ObjectMapper json) {
    this.properties = properties;
    this.json = json;
  }

  public List<ChainTx> recentUsdt(String address, String apiKey, Vocab.TronNetwork network) throws Exception {
    String contract = contract(network);
    String url = base(network) + "/v1/accounts/" + address + "/transactions/trc20?only_confirmed=true&limit=50&contract_address=" + contract;
    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET();
    String key = apiKey == null || apiKey.isBlank() ? properties.getUsdt().getApiKey() : apiKey;
    if (key != null && !key.isBlank()) {
      builder.header("TRON-PRO-API-KEY", key);
    }
    HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() >= 300) {
      throw new IllegalStateException("TronGrid " + response.statusCode());
    }
    JsonNode data = json.readTree(response.body()).path("data");
    List<ChainTx> txs = new ArrayList<>();
    if (!data.isArray()) {
      return txs;
    }
    for (JsonNode item : data) {
      if (!address.equals(item.path("to").asText())) {
        continue;
      }
      String token = item.path("token_info").path("address").asText(contract);
      if (!contract.equals(token)) {
        continue;
      }
      txs.add(new ChainTx(
          item.path("transaction_id").asText(),
          item.path("from").asText(""),
          Long.parseLong(item.path("value").asText("0")),
          Instant.ofEpochMilli(item.path("block_timestamp").asLong())));
    }
    return txs;
  }

  /** Chain tip minus about 20 blocks. Transfers newer than this are not settled. */
  public Instant solidBefore(String apiKey, Vocab.TronNetwork network) throws Exception {
    long now = System.currentTimeMillis();
    if (now - cachedSolidAtMs < 8_000 && cachedSolidBefore.isAfter(Instant.EPOCH)) {
      return cachedSolidBefore;
    }
    HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base(network) + "/wallet/getnowblock"))
        .timeout(Duration.ofSeconds(15))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("{}"));
    String key = apiKey == null || apiKey.isBlank() ? properties.getUsdt().getApiKey() : apiKey;
    if (key != null && !key.isBlank()) {
      builder.header("TRON-PRO-API-KEY", key);
    }
    HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() >= 300) {
      throw new IllegalStateException("TronGrid block " + response.statusCode());
    }
    long tip = json.readTree(response.body()).path("block_header").path("raw_data").path("timestamp").asLong(0);
    if (tip <= 0) {
      throw new IllegalStateException("TronGrid 没有返回区块时间");
    }
    Instant solid = Instant.ofEpochMilli(tip).minusSeconds(60);
    cachedSolidBefore = solid;
    cachedSolidAtMs = now;
    return solid;
  }

  public static String contract(Vocab.TronNetwork network) {
    return network == Vocab.TronNetwork.NILE ? NILE_USDT_CONTRACT : USDT_CONTRACT;
  }

  private String base(Vocab.TronNetwork network) {
    String configured = network == Vocab.TronNetwork.NILE ? "https://nile.trongrid.io" : properties.getUsdt().getTrongridBase();
    if (configured == null || configured.isBlank()) {
      configured = "https://api.trongrid.io";
    }
    return configured.endsWith("/") ? configured.substring(0, configured.length() - 1) : configured;
  }

  public record ChainTx(String hash, String from, long amountSun, Instant at) {}

  /** Nile testnet USDT. Mainnet USDT stays {@link #USDT_CONTRACT}. */
  public static final String NILE_USDT_CONTRACT = "TXYZopYRdj2D9XRtbG411XZZ3kM5VkAeBf";
}
