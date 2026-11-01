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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class EvmUsdtClient {
  public static final Chain ETH = new Chain("ETH", "以太坊", "https://ethereum-rpc.publicnode.com",
      "0xdac17f958d2ee523a2206206994597c13d831ec7", 500);
  public static final Chain ETH_TEST = new Chain("ETH", "以太坊", "https://ethereum-sepolia-rpc.publicnode.com",
      "0xaa8e23fb1079ea71e0a56f48a2aa51851d8433d0", 800);
  public static final Chain POLYGON = new Chain("POLYGON", "Polygon", "https://polygon-bor-rpc.publicnode.com",
      "0xc2132d05d31c914a87c6611c10748aeb04b58e8f", 2000);
  public static final Chain ARBITRUM = new Chain("ARBITRUM", "Arbitrum", "https://arbitrum-one-rpc.publicnode.com",
      "0xfd086bc7cd5c481dcc9c85ebe478a1c0b69fcbb9", 8000);
  public static final Chain AVALANCHE = new Chain("AVALANCHE", "Avalanche", "https://avalanche-c-chain-rpc.publicnode.com",
      "0x9702230a8ea53601f5cd2dc00fdbc13d4df4a8c7", 2000);

  private static final String TRANSFER = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

  private final ObjectMapper json;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
  private final Map<String, Integer> decimals = new HashMap<>();

  public EvmUsdtClient(ObjectMapper json) {
    this.json = json;
  }

  public int decimals(Chain chain) throws Exception {
    Integer cached = decimals.get(chain.contract());
    if (cached != null) {
      return cached;
    }
    JsonNode result = rpc(chain, "eth_call", "[{\"to\":\"" + chain.contract() + "\",\"data\":\"0x313ce567\"},\"latest\"]");
    int value = hex(result.asText("0x0")).intValue();
    if (value < 6 || value > 18) {
      throw new IllegalStateException(chain.label() + " USDT 精度异常");
    }
    decimals.put(chain.contract(), value);
    return value;
  }

  public List<Transfer> recentUsdt(String address, Chain chain) throws Exception {
    String receive = normalize(address);
    long tip = hex(rpc(chain, "eth_blockNumber", "[]").asText("0x0")).longValue();
    long solid = Math.max(0, tip - 15);
    long from = Math.max(0, solid - chain.lookback());
    String padded = "0x" + "0".repeat(24) + receive.substring(2);
    JsonNode logs = rpc(chain, "eth_getLogs", "[{"
        + "\"fromBlock\":\"" + hex(from) + "\","
        + "\"toBlock\":\"" + hex(solid) + "\","
        + "\"address\":\"" + chain.contract() + "\","
        + "\"topics\":[\"" + TRANSFER + "\",null,\"" + padded + "\"]}]");
    List<Transfer> txs = new ArrayList<>();
    if (!logs.isArray()) {
      return txs;
    }
    Map<String, Instant> times = new HashMap<>();
    for (JsonNode item : logs) {
      long block = hex(item.path("blockNumber").asText("0x0")).longValue();
      if (block > solid) {
        continue;
      }
      String data = item.path("data").asText("0x0");
      String fromTopic = item.path("topics").path(1).asText("");
      if (fromTopic.length() < 40) {
        continue;
      }
      String blockHex = item.path("blockNumber").asText();
      Instant at = times.computeIfAbsent(blockHex, key -> blockTime(chain, key));
      txs.add(new Transfer(
          item.path("transactionHash").asText(),
          "0x" + fromTopic.substring(fromTopic.length() - 40),
          hex(data),
          at));
    }
    return txs;
  }

  private Instant blockTime(Chain chain, String blockHex) {
    try {
      JsonNode block = rpc(chain, "eth_getBlockByNumber", "[\"" + blockHex + "\",false]");
      return Instant.ofEpochSecond(hex(block.path("timestamp").asText("0x0")).longValue());
    } catch (Exception ex) {
      return Instant.EPOCH;
    }
  }

  private JsonNode rpc(Chain chain, String method, String params) throws Exception {
    String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\",\"params\":" + params + "}";
    HttpRequest request = HttpRequest.newBuilder(URI.create(chain.rpc()))
        .timeout(Duration.ofSeconds(15))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() >= 300) {
      throw new IllegalStateException(chain.label() + " " + response.statusCode());
    }
    JsonNode root = json.readTree(response.body());
    if (root.hasNonNull("error")) {
      throw new IllegalStateException(root.path("error").path("message").asText(chain.label() + "查询失败"));
    }
    return root.path("result");
  }

  private static String normalize(String address) {
    String text = address == null ? "" : address.trim().toLowerCase();
    if (!text.matches("^0x[0-9a-f]{40}$")) {
      throw new IllegalArgumentException("地址无效");
    }
    return text;
  }

  private static String hex(long value) {
    return "0x" + Long.toHexString(value);
  }

  private static BigInteger hex(String value) {
    String text = value == null ? "0" : value.trim();
    if (text.startsWith("0x") || text.startsWith("0X")) {
      text = text.substring(2);
    }
    if (text.isBlank()) {
      return BigInteger.ZERO;
    }
    return new BigInteger(text, 16);
  }

  public record Chain(String code, String label, String rpc, String contract, int lookback) {}

  public record Transfer(String hash, String from, BigInteger amount, Instant at) {}
}
