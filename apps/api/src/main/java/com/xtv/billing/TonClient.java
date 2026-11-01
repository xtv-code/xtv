package com.xtv.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TonClient {
  public static final String USDT_MASTER = "EQCxE6mUtQJKFnGfaROTKOt1lZbDiiX1kCixRv7Nw2Id_sDs";
  private static final String RAW_MASTER = "0:b113a994b5024a16719f69139328eb759596c38a25f59028b146fecdc3621dfe";

  private final ObjectMapper json;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

  public TonClient(ObjectMapper json) {
    this.json = json;
  }

  public List<Transfer> recentUsdt(String owner) throws Exception {
    String rawOwner = raw(owner);
    String url = "https://tonapi.io/v2/accounts/" + URLEncoder.encode(owner, StandardCharsets.UTF_8)
        + "/jettons/" + USDT_MASTER + "/history?limit=20";
    HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build();
    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() >= 300) {
      throw new IllegalStateException("TON " + response.statusCode());
    }
    JsonNode root = json.readTree(response.body());
    List<Transfer> txs = new ArrayList<>();
    for (JsonNode event : root.path("events")) {
      String hash = event.path("event_id").asText("");
      Instant at = Instant.ofEpochSecond(event.path("timestamp").asLong(0));
      for (JsonNode action : event.path("actions")) {
        if (!"JettonTransfer".equals(action.path("type").asText()) || !"ok".equals(action.path("status").asText("ok"))) {
          continue;
        }
        JsonNode transfer = action.path("JettonTransfer");
        String jetton = transfer.path("jetton").path("address").asText("");
        if (!jetton.isBlank() && !RAW_MASTER.equalsIgnoreCase(jetton) && !USDT_MASTER.equals(jetton)) {
          continue;
        }
        String from = transfer.path("sender").path("address").asText("");
        if (from.isBlank() || from.equalsIgnoreCase(rawOwner)) {
          continue;
        }
        String amount = transfer.path("amount").asText("");
        if (amount.isBlank() || hash.isBlank()) {
          continue;
        }
        txs.add(new Transfer(hash, from, new BigInteger(amount), at));
      }
    }
    return txs;
  }

  private String raw(String owner) throws Exception {
    String url = "https://tonapi.io/v2/accounts/" + URLEncoder.encode(owner, StandardCharsets.UTF_8);
    HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).GET().build();
    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() >= 300) {
      throw new IllegalStateException("TON " + response.statusCode());
    }
    String address = json.readTree(response.body()).path("address").asText("");
    if (address.isBlank()) {
      throw new IllegalStateException("TON 地址无效");
    }
    return address;
  }

  public record Transfer(String hash, String from, BigInteger amount, Instant at) {}
}
