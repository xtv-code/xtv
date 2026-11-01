package com.xtv.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigInteger;
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
public class SolanaClient {
  public static final String USDT_MINT = "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB";
  private static final String RPC = "https://solana-rpc.publicnode.com";

  private final ObjectMapper json;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

  public SolanaClient(ObjectMapper json) {
    this.json = json;
  }

  public List<Transfer> recentUsdt(String owner) throws Exception {
    JsonNode accounts = rpc("getTokenAccountsByOwner",
        "[\"" + owner + "\",{\"mint\":\"" + USDT_MINT + "\"},{\"encoding\":\"jsonParsed\"}]");
    List<Transfer> txs = new ArrayList<>();
    for (JsonNode account : accounts.path("value")) {
      String tokenAccount = account.path("pubkey").asText("");
      if (tokenAccount.isBlank()) {
        continue;
      }
      JsonNode signatures = rpc("getSignaturesForAddress", "[\"" + tokenAccount + "\",{\"limit\":20}]");
      if (!signatures.isArray()) {
        continue;
      }
      for (JsonNode sig : signatures) {
        if (!sig.path("err").isNull() && !sig.path("err").isMissingNode()) {
          continue;
        }
        String signature = sig.path("signature").asText("");
        if (signature.isBlank()) {
          continue;
        }
        Instant at = Instant.ofEpochSecond(sig.path("blockTime").asLong(0));
        JsonNode tx = rpc("getTransaction",
            "[\"" + signature + "\",{\"encoding\":\"jsonParsed\",\"maxSupportedTransactionVersion\":0}]");
        collect(txs, tx.path("transaction").path("message").path("instructions"), tokenAccount, signature, at);
        for (JsonNode inner : tx.path("meta").path("innerInstructions")) {
          collect(txs, inner.path("instructions"), tokenAccount, signature, at);
        }
      }
    }
    return txs;
  }

  private static void collect(List<Transfer> txs, JsonNode instructions, String tokenAccount, String signature, Instant at) {
    if (!instructions.isArray()) {
      return;
    }
    for (JsonNode instruction : instructions) {
      JsonNode info = instruction.path("parsed").path("info");
      if (!tokenAccount.equals(info.path("destination").asText())) {
        continue;
      }
      String type = instruction.path("parsed").path("type").asText("");
      if (!"transfer".equals(type) && !"transferChecked".equals(type)) {
        continue;
      }
      if ("transferChecked".equals(type) && !USDT_MINT.equals(info.path("mint").asText())) {
        continue;
      }
      String raw = info.path("tokenAmount").path("amount").asText(info.path("amount").asText(""));
      if (raw.isBlank()) {
        continue;
      }
      String from = info.path("authority").asText(info.path("source").asText(""));
      txs.add(new Transfer(signature, from, new BigInteger(raw), at));
    }
  }

  private JsonNode rpc(String method, String params) throws Exception {
    String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\",\"params\":" + params + "}";
    HttpRequest request = HttpRequest.newBuilder(URI.create(RPC))
        .timeout(Duration.ofSeconds(15))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() >= 300) {
      throw new IllegalStateException("Solana " + response.statusCode());
    }
    JsonNode root = json.readTree(response.body());
    if (root.hasNonNull("error")) {
      throw new IllegalStateException(root.path("error").path("message").asText("Solana 查询失败"));
    }
    return root.path("result");
  }

  public record Transfer(String hash, String from, BigInteger amount, Instant at) {}
}
