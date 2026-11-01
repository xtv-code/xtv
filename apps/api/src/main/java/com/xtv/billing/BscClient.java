package com.xtv.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xtv.domain.Vocab;
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
public class BscClient {
  /** Binance-Peg USDT on BNB Smart Chain. 18 decimals. */
  public static final String MAINNET_USDT = "0x55d398326f99059ff775485246999027b3197955";
  /** Chapel faucet USDT. Decimals are read from the contract. */
  public static final String TESTNET_USDT = "0x337610d27c682e347c9cd60bd4b3b107c9d34ddd";
  private static final String TRANSFER = "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef";

  private final ObjectMapper json;
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
  private final Map<Vocab.BscNetwork, Integer> decimals = new HashMap<>();

  public BscClient(ObjectMapper json) {
    this.json = json;
  }

  public int decimals(Vocab.BscNetwork network) throws Exception {
    Integer cached = decimals.get(network);
    if (cached != null) {
      return cached;
    }
    String contract = contract(network);
    JsonNode result = rpc(network, "eth_call", "[{\"to\":\"" + contract + "\",\"data\":\"0x313ce567\"},\"latest\"]");
    int value = hex(result.asText("0x0")).intValue();
    if (value < 6 || value > 18) {
      throw new IllegalStateException("币安链 USDT 精度异常");
    }
    decimals.put(network, value);
    return value;
  }

  public List<Transfer> recentUsdt(String address, Vocab.BscNetwork network) throws Exception {
    String receive = normalize(address);
    String contract = contract(network);
    long tip = hex(rpc(network, "eth_blockNumber", "[]").asText("0x0")).longValue();
    long solid = Math.max(0, tip - 15);
    long from = Math.max(0, solid - 900);
    String padded = "0x" + "0".repeat(24) + receive.substring(2);
    JsonNode logs = rpc(network, "eth_getLogs", "[{"
        + "\"fromBlock\":\"" + hex(from) + "\","
        + "\"toBlock\":\"" + hex(solid) + "\","
        + "\"address\":\"" + contract + "\","
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
      Instant at = times.computeIfAbsent(blockHex, key -> blockTime(network, key));
      txs.add(new Transfer(
          item.path("transactionHash").asText(),
          "0x" + fromTopic.substring(fromTopic.length() - 40),
          hex(data),
          at));
    }
    return txs;
  }

  public static String contract(Vocab.BscNetwork network) {
    return network == Vocab.BscNetwork.TESTNET ? TESTNET_USDT : MAINNET_USDT;
  }

  private Instant blockTime(Vocab.BscNetwork network, String blockHex) {
    try {
      JsonNode block = rpc(network, "eth_getBlockByNumber", "[\"" + blockHex + "\",false]");
      return Instant.ofEpochSecond(hex(block.path("timestamp").asText("0x0")).longValue());
    } catch (Exception ex) {
      return Instant.EPOCH;
    }
  }

  private JsonNode rpc(Vocab.BscNetwork network, String method, String params) throws Exception {
    String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\",\"params\":" + params + "}";
    HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint(network)))
        .timeout(Duration.ofSeconds(15))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() >= 300) {
      throw new IllegalStateException("币安链 " + response.statusCode());
    }
    JsonNode root = json.readTree(response.body());
    if (root.hasNonNull("error")) {
      throw new IllegalStateException(root.path("error").path("message").asText("币安链查询失败"));
    }
    return root.path("result");
  }

  private static String endpoint(Vocab.BscNetwork network) {
    return network == Vocab.BscNetwork.TESTNET
        ? "https://bsc-testnet.publicnode.com"
        : "https://bsc-dataseed.binance.org/";
  }

  private static String normalize(String address) {
    String text = address == null ? "" : address.trim().toLowerCase();
    if (!text.matches("^0x[0-9a-f]{40}$")) {
      throw new IllegalArgumentException("币安链地址无效");
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

  public record Transfer(String hash, String from, BigInteger amount, Instant at) {}
}
