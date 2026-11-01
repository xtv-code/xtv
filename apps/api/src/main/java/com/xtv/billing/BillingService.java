package com.xtv.billing;

import com.xtv.domain.ChainReceiver;
import com.xtv.domain.MembershipPlan;
import com.xtv.domain.PayOrder;
import com.xtv.domain.UserAccount;
import com.xtv.domain.Video;
import com.xtv.domain.VideoEntitlement;
import com.xtv.domain.Vocab;
import com.xtv.dto.Models;
import com.xtv.dto.Requests;
import com.xtv.domain.PlatformConfig;
import com.xtv.repo.ChainReceiverRepository;
import com.xtv.repo.MembershipPlanRepository;
import com.xtv.repo.PayOrderRepository;
import com.xtv.repo.PlatformConfigRepository;
import com.xtv.repo.UserAccountRepository;
import com.xtv.repo.VideoEntitlementRepository;
import com.xtv.repo.VideoRepository;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BillingService {
  private static final Logger log = LoggerFactory.getLogger(BillingService.class);

  private final PayOrderRepository orders;
  private final MembershipPlanRepository plans;
  private final VideoRepository videos;
  private final VideoEntitlementRepository entitlements;
  private final UserAccountRepository users;
  private final PlatformConfigRepository configs;
  private final TronClient tron;
  private final BscClient bsc;
  private final EvmUsdtClient evm;
  private final SolanaClient solana;
  private final TonClient ton;
  private final ChainReceiverRepository receivers;
  private final com.xtv.service.AuditService audit;
  private final ObjectProvider<BillingService> self;

  public BillingService(PayOrderRepository orders, MembershipPlanRepository plans, VideoRepository videos,
                        VideoEntitlementRepository entitlements, UserAccountRepository users,
                        PlatformConfigRepository configs, TronClient tron, BscClient bsc, EvmUsdtClient evm,
                        SolanaClient solana, TonClient ton, ChainReceiverRepository receivers,
                        com.xtv.service.AuditService audit, ObjectProvider<BillingService> self) {
    this.orders = orders;
    this.plans = plans;
    this.videos = videos;
    this.entitlements = entitlements;
    this.users = users;
    this.configs = configs;
    this.tron = tron;
    this.bsc = bsc;
    this.evm = evm;
    this.solana = solana;
    this.ton = ton;
    this.receivers = receivers;
    this.audit = audit;
    this.self = self;
  }

  public List<Models.PaymentChannel> channels() {
    PlatformConfig cfg = payment();
    List<Models.PaymentChannel> list = new ArrayList<>();
    if (filled(cfg.getUsdtAddress())) {
      list.add(new Models.PaymentChannel(Vocab.PaymentMethod.USDT_TRC20.name()));
    }
    if (filled(receiver("ETH"))) {
      list.add(new Models.PaymentChannel(Vocab.PaymentMethod.USDT_ERC20.name()));
    }
    if (filled(cfg.getBscAddress())) {
      list.add(new Models.PaymentChannel(Vocab.PaymentMethod.USDT_BEP20.name()));
    }
    if (filled(receiver("POLYGON"))) {
      list.add(new Models.PaymentChannel(Vocab.PaymentMethod.USDT_POLYGON.name()));
    }
    if (filled(receiver("ARBITRUM"))) {
      list.add(new Models.PaymentChannel(Vocab.PaymentMethod.USDT_ARBITRUM.name()));
    }
    if (filled(receiver("AVALANCHE"))) {
      list.add(new Models.PaymentChannel(Vocab.PaymentMethod.USDT_AVALANCHE.name()));
    }
    if (filled(receiver("SOLANA"))) {
      list.add(new Models.PaymentChannel(Vocab.PaymentMethod.USDT_SOLANA.name()));
    }
    if (filled(receiver("TON"))) {
      list.add(new Models.PaymentChannel(Vocab.PaymentMethod.USDT_TON.name()));
    }
    return list;
  }

  public List<Models.ChainReceiverView> chainReceivers() {
    return List.of("ETH", "POLYGON", "ARBITRUM", "AVALANCHE", "SOLANA", "TON").stream()
        .map(code -> new Models.ChainReceiverView(code, receiver(code), receiverNetwork(code))).toList();
  }

  @Transactional
  public List<Models.ChainReceiverView> saveChainReceivers(List<Requests.ChainReceiverRequest> requests) {
    if (requests == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请填写收款地址");
    }
    for (Requests.ChainReceiverRequest req : requests) {
      if (req == null || chainOf(req.code()) == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "收款渠道无效");
      }
      String receive = req.address() == null ? "" : req.address().trim();
      if (receive.length() > 128) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "收款地址过长");
      }
      if (!receive.isBlank() && !addressOk(req.code(), receive)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "收款地址格式不对");
      }
      String network = req.network() == null || req.network().isBlank() ? "MAINNET" : req.network().trim();
      if (!"MAINNET".equals(network) && !"TESTNET".equals(network)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "网络无效");
      }
      if ("TESTNET".equals(network) && !"ETH".equals(req.code())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "这个渠道没有测试网");
      }
      ChainReceiver row = receivers.findById(req.code()).orElseGet(() -> {
        ChainReceiver created = new ChainReceiver();
        created.setCode(req.code());
        return created;
      });
      row.setAddress(receive);
      row.setNetwork(network);
      receivers.save(row);
    }
    return chainReceivers();
  }

  @Transactional
  public Models.OrderView create(UserAccount user, Vocab.OrderType type, Long planId, Long videoId, String paymentMethod) {
    BigDecimal list;
    Long savedPlan = null;
    Long savedVideo = null;
    if (type == Vocab.OrderType.MEMBERSHIP) {
      MembershipPlan plan = plans.findById(planId == null ? -1 : planId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "会员方案不存在"));
      if (!plan.isEnabled()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "会员方案已下架");
      }
      list = plan.getPriceUsdt();
      savedPlan = plan.getId();
    } else {
      Video video = videos.findById(videoId == null ? -1 : videoId)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "影片不存在"));
      if (video.getStatus() != Vocab.VideoStatus.PUBLISHED || video.getAccessType() != Vocab.AccessType.PPV) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "该片不支持单集购买");
      }
      if (entitlements.existsByUserIdAndVideoId(user.getId(), video.getId())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "已经购买过这一集");
      }
      list = video.getPriceUsdt();
      savedVideo = video.getId();
    }
    if (list == null || list.signum() <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "价格无效");
    }
    PayOrder order = new PayOrder();
    order.setUserId(user.getId());
    order.setOrderType(type);
    order.setPlanId(savedPlan);
    order.setVideoId(savedVideo);
    Vocab.PaymentMethod chosen = method(paymentMethod);
    order.setListPrice(list.setScale(6, RoundingMode.HALF_UP));
    order.setPayAmount(uniqueAmount(list));
    order.setPaymentMethod(chosen);
    order.setState(Vocab.OrderState.PENDING);
    order.setCreatedAt(Instant.now());
    order.setExpiresAt(order.getCreatedAt().plus(30, ChronoUnit.MINUTES));
    orders.saveAndFlush(order);
    return view(order);
  }

  @Transactional
  public Models.OrderView adminConfirm(Long orderId) {
    return markPaid(orderId, "admin-" + orderId + "-" + System.currentTimeMillis());
  }

  @Transactional
  public Models.OrderView cancel(Long orderId) {
    return cancel(orderId, null);
  }

  @Transactional
  public Models.OrderView cancel(Long orderId, UserAccount actor) {
    PayOrder order = orders.findById(orderId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
    if (actor != null && actor.getRole() != Vocab.Role.ADMIN && !order.getUserId().equals(actor.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
    }
    if (order.getState() == Vocab.OrderState.PAID) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "已支付的订单不能关闭");
    }
    if (order.getState() != Vocab.OrderState.PENDING) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "订单已关闭");
    }
    order.setState(Vocab.OrderState.EXPIRED);
    audit.record("order.cancel", "order", String.valueOf(orderId), order.getOrderType().name());
    return view(order);
  }

  @Transactional
  public Models.OrderView markPaid(Long orderId, String txHash) {
    return markPaid(orderId, txHash, "");
  }

  @Transactional
  public Models.OrderView markPaid(Long orderId, String txHash, String payer) {
    PayOrder order = orders.findById(orderId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
    if (order.getState() == Vocab.OrderState.PAID) {
      return view(order);
    }
    if (order.getState() != Vocab.OrderState.PENDING) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "订单不可支付");
    }
    if (txHash == null || txHash.isBlank() || orders.existsByTxHash(txHash)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "交易已使用");
    }
    order.setState(Vocab.OrderState.PAID);
    order.setTxHash(txHash);
    String from = payer == null ? "" : payer.trim();
    order.setPayerAddress(from.length() > 64 ? from.substring(0, 64) : from);
    audit.record("order.paid", "order", String.valueOf(orderId),
        order.getOrderType().name() + " amount=" + order.getPayAmount() + " tx=" + txHash
            + (order.getPayerAddress().isBlank() ? "" : " from=" + order.getPayerAddress()));
    if (order.getOrderType() == Vocab.OrderType.MEMBERSHIP) {
      MembershipPlan plan = plans.findById(order.getPlanId()).orElseThrow();
      UserAccount owner = users.findById(order.getUserId()).orElseThrow();
      Instant base = owner.getMemberUntil() != null && owner.getMemberUntil().isAfter(Instant.now())
          ? owner.getMemberUntil() : Instant.now();
      owner.setMemberUntil(base.plus(plan.getDurationDays(), ChronoUnit.DAYS));
    } else if (!entitlements.existsByUserIdAndVideoId(order.getUserId(), order.getVideoId())) {
      VideoEntitlement entitlement = new VideoEntitlement();
      entitlement.setUserId(order.getUserId());
      entitlement.setVideoId(order.getVideoId());
      entitlement.setOrderId(order.getId());
      entitlements.save(entitlement);
    }
    return view(order);
  }

  @Transactional
  public void expire() {
    for (PayOrder order : orders.findByStateAndExpiresAtBefore(Vocab.OrderState.PENDING, Instant.now())) {
      order.setState(Vocab.OrderState.EXPIRED);
    }
  }

  public void reconcile() {
    Instant now = Instant.now();
    List<PayOrder> pending = orders.findByState(Vocab.OrderState.PENDING).stream()
        .filter(order -> order.getExpiresAt() == null || !order.getExpiresAt().isBefore(now))
        .toList();
    if (pending.stream().anyMatch(order -> order.getPaymentMethod() == Vocab.PaymentMethod.USDT_TRC20)) {
      try {
        reconcileTron(pending);
      } catch (Exception ex) {
        log.warn("波场对账失败: {}", ex.getMessage());
      }
    }
    if (pending.stream().anyMatch(order -> order.getPaymentMethod() == Vocab.PaymentMethod.USDT_BEP20)) {
      try {
        reconcileBsc(pending);
      } catch (Exception ex) {
        log.warn("币安链对账失败: {}", ex.getMessage());
      }
    }
    reconcileWatched(pending, Vocab.PaymentMethod.USDT_ERC20, "以太坊");
    reconcileWatched(pending, Vocab.PaymentMethod.USDT_POLYGON, "Polygon");
    reconcileWatched(pending, Vocab.PaymentMethod.USDT_ARBITRUM, "Arbitrum");
    reconcileWatched(pending, Vocab.PaymentMethod.USDT_AVALANCHE, "Avalanche");
    reconcileWatched(pending, Vocab.PaymentMethod.USDT_SOLANA, "Solana");
    reconcileWatched(pending, Vocab.PaymentMethod.USDT_TON, "TON");
    self.getObject().expire();
  }

  private void reconcileWatched(List<PayOrder> pending, Vocab.PaymentMethod method, String label) {
    if (pending.stream().noneMatch(order -> order.getPaymentMethod() == method)) {
      return;
    }
    try {
      reconcileChain(pending, method);
    } catch (Exception ex) {
      log.warn("{}对账失败: {}", label, ex.getMessage());
    }
  }

  private void reconcileTron(List<PayOrder> pending) throws Exception {
    PlatformConfig cfg = payment();
    String address = cfg.getUsdtAddress();
    if (address == null || address.isBlank()) {
      return;
    }
    Vocab.TronNetwork network = cfg.getTronNetwork() == null ? Vocab.TronNetwork.MAINNET : cfg.getTronNetwork();
    List<TronClient.ChainTx> txs = tron.recentUsdt(address, cfg.getTronApiKey(), network);
    // Mainnet waits ~1 minute past tip so reorgs are less likely; Nile credits on confirmed only.
    Instant solidBefore = network == Vocab.TronNetwork.NILE
        ? Instant.MAX
        : tron.solidBefore(cfg.getTronApiKey(), network);
    for (PayOrder order : pending) {
      if (order.getPaymentMethod() != Vocab.PaymentMethod.USDT_TRC20 || order.getExpiresAt().isBefore(Instant.now())) {
        continue;
      }
      long expected = order.getPayAmount().movePointRight(6).setScale(0, RoundingMode.HALF_UP).longValueExact();
      for (TronClient.ChainTx tx : txs) {
        boolean settled = !tx.at().isAfter(solidBefore);
        boolean inWindow = !tx.at().isBefore(order.getCreatedAt().minusSeconds(60))
            && !tx.at().isAfter(order.getExpiresAt().plusSeconds(120));
        boolean notSelf = tx.from() != null && !tx.from().equalsIgnoreCase(address);
        if (settled && tx.amountSun() == expected && inWindow && notSelf && !orders.existsByTxHash(tx.hash())) {
          credit(order.getId(), tx.hash(), tx.from());
          break;
        }
      }
    }
  }

  private void reconcileBsc(List<PayOrder> pending) throws Exception {
    PlatformConfig cfg = payment();
    String address = cfg.getBscAddress();
    if (address == null || address.isBlank()) {
      return;
    }
    Vocab.BscNetwork network = cfg.getBscNetwork() == null ? Vocab.BscNetwork.MAINNET : cfg.getBscNetwork();
    int decimals = bsc.decimals(network);
    List<BscClient.Transfer> txs = bsc.recentUsdt(address, network);
    for (PayOrder order : pending) {
      if (order.getPaymentMethod() != Vocab.PaymentMethod.USDT_BEP20 || order.getExpiresAt().isBefore(Instant.now())) {
        continue;
      }
      BigInteger expected = order.getPayAmount().movePointRight(decimals).toBigIntegerExact();
      for (BscClient.Transfer tx : txs) {
        boolean inWindow = !tx.at().isBefore(order.getCreatedAt().minusSeconds(60))
            && !tx.at().isAfter(order.getExpiresAt().plusSeconds(120));
        boolean notSelf = tx.from() != null && !tx.from().equalsIgnoreCase(address);
        if (expected.equals(tx.amount()) && inWindow && notSelf && !orders.existsByTxHash(tx.hash())) {
          credit(order.getId(), tx.hash(), tx.from());
          break;
        }
      }
    }
  }

  private void credit(Long orderId, String hash, String from) {
    try {
      self.getObject().markPaid(orderId, hash, from);
    } catch (ResponseStatusException ex) {
      log.warn("订单 {} 对账跳过: {}", orderId, ex.getReason());
    }
  }

  public Models.OrderView get(Long id, UserAccount user) {
    PayOrder order = orders.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
    if (user.getRole() != Vocab.Role.ADMIN && !order.getUserId().equals(user.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
    }
    return view(order);
  }

  public Models.OrderView check(Long id, UserAccount user) {
    PayOrder order = orders.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在"));
    if (user.getRole() != Vocab.Role.ADMIN && !order.getUserId().equals(user.getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
    }
    if (order.getState() == Vocab.OrderState.PENDING && !order.getExpiresAt().isBefore(Instant.now())) {
      try {
        if (order.getPaymentMethod() == Vocab.PaymentMethod.USDT_TRC20) {
          reconcileTron(List.of(order));
        } else if (order.getPaymentMethod() == Vocab.PaymentMethod.USDT_BEP20) {
          reconcileBsc(List.of(order));
        } else {
          reconcileChain(List.of(order), order.getPaymentMethod());
        }
      } catch (Exception ex) {
        log.warn("对账失败: {}", ex.getMessage());
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "暂时无法查询到账，请稍后再试");
      }
    }
    return view(orders.findById(id).orElseThrow());
  }

  public List<Models.OrderView> mine(UserAccount user) {
    return orders.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::view).toList();
  }

  public Models.PageResult<Models.OrderView> all(int page, int size, String id, String orderType, String state,
                                                 String payAmount, String createdAt, String sort, String dir) {
    java.util.List<org.springframework.data.jpa.domain.Specification<PayOrder>> parts = new java.util.ArrayList<>();
    com.xtv.service.AdminQuery.add(parts, com.xtv.service.AdminQuery.eqLong("id", id));
    com.xtv.service.AdminQuery.add(parts, com.xtv.service.AdminQuery.eqEnum("orderType", Vocab.OrderType.class, orderType));
    com.xtv.service.AdminQuery.add(parts, com.xtv.service.AdminQuery.eqEnum("state", Vocab.OrderState.class, state));
    com.xtv.service.AdminQuery.add(parts, com.xtv.service.AdminQuery.eqBigDecimal("payAmount", payAmount));
    com.xtv.service.AdminQuery.add(parts, com.xtv.service.AdminQuery.instantContains("createdAt", createdAt));
    var result = orders.findAll(com.xtv.service.AdminQuery.and(parts),
        com.xtv.service.AdminQuery.page(page, size, sort, dir, "createdAt",
            java.util.Set.of("id", "orderType", "state", "payAmount", "createdAt", "userId", "paymentMethod")));
    return new Models.PageResult<>(result.getContent().stream().map(this::view).toList(),
        result.getNumber() + 1, result.getSize(), result.getTotalElements());
  }

  private BigDecimal uniqueAmount(BigDecimal list) {
    BigDecimal base = list.setScale(6, RoundingMode.HALF_UP);
    for (int i = 0; i < 30; i++) {
      int tail = ThreadLocalRandom.current().nextInt(1, 10_000);
      BigDecimal amount = base.add(BigDecimal.valueOf(tail, 6));
      if (!orders.existsByStateAndPayAmount(Vocab.OrderState.PENDING, amount)) {
        return amount;
      }
    }
    throw new ResponseStatusException(HttpStatus.CONFLICT, "暂时无法分配支付金额");
  }

  private Vocab.PaymentMethod method(String raw) {
    PlatformConfig cfg = payment();
    if (raw == null || raw.isBlank()) {
      List<Models.PaymentChannel> open = channels();
      if (open.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "没有可用的收款渠道");
      }
      return Vocab.PaymentMethod.valueOf(open.get(0).method());
    }
    Vocab.PaymentMethod chosen;
    try {
      chosen = Vocab.PaymentMethod.valueOf(raw.trim());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "收款渠道无效");
    }
    if (chosen == Vocab.PaymentMethod.BINANCE_PAY || chosen == Vocab.PaymentMethod.GATE_PAY
        || chosen == Vocab.PaymentMethod.BYBIT_PAY) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "这个渠道已关闭");
    }
    String receive = receiveAddress(cfg, chosen);
    if (receive == null || receive.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "这个渠道还没有收款地址");
    }
    return chosen;
  }

  private void reconcileChain(List<PayOrder> pending, Vocab.PaymentMethod method) throws Exception {
    if (method == Vocab.PaymentMethod.USDT_SOLANA) {
      reconcileSolana(pending);
      return;
    }
    if (method == Vocab.PaymentMethod.USDT_TON) {
      reconcileTon(pending);
      return;
    }
    EvmUsdtClient.Chain chain = evmChain(method);
    if (chain == null) {
      return;
    }
    String address = receiver(chain.code());
    if (!filled(address)) {
      return;
    }
    int decimals = evm.decimals(chain);
    List<EvmUsdtClient.Transfer> txs = evm.recentUsdt(address, chain);
    for (PayOrder order : pending) {
      if (order.getPaymentMethod() != method || order.getExpiresAt().isBefore(Instant.now())) {
        continue;
      }
      BigInteger expected = order.getPayAmount().movePointRight(decimals).toBigIntegerExact();
      for (EvmUsdtClient.Transfer tx : txs) {
        boolean inWindow = !tx.at().isBefore(order.getCreatedAt().minusSeconds(60))
            && !tx.at().isAfter(order.getExpiresAt().plusSeconds(120));
        boolean notSelf = tx.from() != null && !tx.from().equalsIgnoreCase(address);
        if (expected.equals(tx.amount()) && inWindow && notSelf && !orders.existsByTxHash(tx.hash())) {
          credit(order.getId(), tx.hash(), tx.from());
          break;
        }
      }
    }
  }

  private void reconcileSolana(List<PayOrder> pending) throws Exception {
    String address = receiver("SOLANA");
    if (!filled(address)) {
      return;
    }
    List<SolanaClient.Transfer> txs = solana.recentUsdt(address);
    matchFixed(pending, Vocab.PaymentMethod.USDT_SOLANA, address, txs);
  }

  private void reconcileTon(List<PayOrder> pending) throws Exception {
    String address = receiver("TON");
    if (!filled(address)) {
      return;
    }
    List<TonClient.Transfer> txs = ton.recentUsdt(address);
    for (PayOrder order : pending) {
      if (order.getPaymentMethod() != Vocab.PaymentMethod.USDT_TON || order.getExpiresAt().isBefore(Instant.now())) {
        continue;
      }
      BigInteger expected = order.getPayAmount().movePointRight(6).toBigIntegerExact();
      for (TonClient.Transfer tx : txs) {
        boolean inWindow = !tx.at().isBefore(order.getCreatedAt().minusSeconds(60))
            && !tx.at().isAfter(order.getExpiresAt().plusSeconds(120));
        if (expected.equals(tx.amount()) && inWindow && !orders.existsByTxHash(tx.hash())) {
          credit(order.getId(), tx.hash(), tx.from());
          break;
        }
      }
    }
  }

  private void matchFixed(List<PayOrder> pending, Vocab.PaymentMethod method, String address, List<SolanaClient.Transfer> txs) {
    for (PayOrder order : pending) {
      if (order.getPaymentMethod() != method || order.getExpiresAt().isBefore(Instant.now())) {
        continue;
      }
      BigInteger expected = order.getPayAmount().movePointRight(6).toBigIntegerExact();
      for (SolanaClient.Transfer tx : txs) {
        boolean inWindow = !tx.at().isBefore(order.getCreatedAt().minusSeconds(60))
            && !tx.at().isAfter(order.getExpiresAt().plusSeconds(120));
        boolean notSelf = tx.from() != null && !tx.from().equals(address);
        if (expected.equals(tx.amount()) && inWindow && notSelf && !orders.existsByTxHash(tx.hash())) {
          credit(order.getId(), tx.hash(), tx.from());
          break;
        }
      }
    }
  }

  private Models.OrderView view(PayOrder order) {
    PlatformConfig cfg = payment();
    String address = receiveAddress(cfg, order.getPaymentMethod());
    String network = networkOf(cfg, order.getPaymentMethod());
    return new Models.OrderView(order.getId(), order.getOrderType().name(), order.getPlanId(), order.getVideoId(),
        order.getListPrice(), order.getPayAmount(), order.getPaymentMethod().name(), order.getState().name(),
        address == null ? "" : address, cfg.isPaymentSimulate(), order.getExpiresAt(), order.getCreatedAt(),
        order.getUserId(), order.getPayerAddress() == null ? "" : order.getPayerAddress(),
        order.getTxHash() == null ? "" : order.getTxHash(), network,
        order.getCheckoutUrl() == null ? "" : order.getCheckoutUrl());
  }

  private String receiveAddress(PlatformConfig cfg, Vocab.PaymentMethod method) {
    if (method == Vocab.PaymentMethod.USDT_BEP20) {
      return cfg.getBscAddress();
    }
    if (method == Vocab.PaymentMethod.USDT_TRC20) {
      return cfg.getUsdtAddress();
    }
    EvmUsdtClient.Chain chain = evmChain(method);
    if (chain != null) {
      return receiver(chain.code());
    }
    if (method == Vocab.PaymentMethod.USDT_SOLANA) {
      return receiver("SOLANA");
    }
    if (method == Vocab.PaymentMethod.USDT_TON) {
      return receiver("TON");
    }
    return "";
  }

  private String networkOf(PlatformConfig cfg, Vocab.PaymentMethod method) {
    if (method == Vocab.PaymentMethod.USDT_BEP20) {
      return cfg.getBscNetwork() == Vocab.BscNetwork.TESTNET ? "BSC_TEST" : "BSC";
    }
    if (method == Vocab.PaymentMethod.USDT_TRC20) {
      Vocab.TronNetwork tronNetwork = cfg.getTronNetwork() == null ? Vocab.TronNetwork.MAINNET : cfg.getTronNetwork();
      return tronNetwork == Vocab.TronNetwork.NILE ? "NILE" : "MAINNET";
    }
    EvmUsdtClient.Chain chain = evmChain(method);
    if (chain == EvmUsdtClient.ETH_TEST) {
      return "ETH_TEST";
    }
    if (chain != null) {
      return chain.code();
    }
    if (method == Vocab.PaymentMethod.USDT_SOLANA) {
      return "SOLANA";
    }
    if (method == Vocab.PaymentMethod.USDT_TON) {
      return "TON";
    }
    return "";
  }

  private EvmUsdtClient.Chain evmChain(Vocab.PaymentMethod method) {
    return switch (method) {
      case USDT_ERC20 -> "TESTNET".equals(receiverNetwork("ETH")) ? EvmUsdtClient.ETH_TEST : EvmUsdtClient.ETH;
      case USDT_POLYGON -> EvmUsdtClient.POLYGON;
      case USDT_ARBITRUM -> EvmUsdtClient.ARBITRUM;
      case USDT_AVALANCHE -> EvmUsdtClient.AVALANCHE;
      default -> null;
    };
  }

  private static Vocab.PaymentMethod chainOf(String code) {
    return switch (code == null ? "" : code) {
      case "ETH" -> Vocab.PaymentMethod.USDT_ERC20;
      case "POLYGON" -> Vocab.PaymentMethod.USDT_POLYGON;
      case "ARBITRUM" -> Vocab.PaymentMethod.USDT_ARBITRUM;
      case "AVALANCHE" -> Vocab.PaymentMethod.USDT_AVALANCHE;
      case "SOLANA" -> Vocab.PaymentMethod.USDT_SOLANA;
      case "TON" -> Vocab.PaymentMethod.USDT_TON;
      default -> null;
    };
  }

  private static boolean addressOk(String code, String address) {
    return switch (code) {
      case "ETH", "POLYGON", "ARBITRUM", "AVALANCHE" -> address.matches("^0x[0-9a-fA-F]{40}$");
      case "SOLANA" -> address.matches("^[1-9A-HJ-NP-Za-km-z]{32,44}$");
      case "TON" -> address.matches("^(EQ|UQ|kQ|0Q)[A-Za-z0-9_\\-]{46}$")
          || address.matches("^-?[01]:[0-9a-fA-F]{64}$");
      default -> false;
    };
  }

  private String receiver(String code) {
    return receivers.findById(code).map(ChainReceiver::getAddress).orElse("");
  }

  private String receiverNetwork(String code) {
    String network = receivers.findById(code).map(ChainReceiver::getNetwork).orElse("MAINNET");
    return network == null || network.isBlank() ? "MAINNET" : network;
  }

  private static boolean filled(String value) {
    return value != null && !value.isBlank();
  }

  private PlatformConfig payment() {
    return configs.findById(1L).orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "缺少平台配置"));
  }
}
