package com.xtv.web;

import com.xtv.billing.BillingService;
import com.xtv.domain.UserAccount;
import com.xtv.domain.Vocab;
import com.xtv.dto.Models;
import com.xtv.dto.Requests;
import com.xtv.service.CurrentUser;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class BillingController {
  private final BillingService billing;

  public BillingController(BillingService billing) {
    this.billing = billing;
  }

  @PostMapping("/orders")
  public Models.OrderView create(@RequestBody Requests.OrderRequest req) {
    UserAccount user = CurrentUser.require();
    Vocab.OrderType type = Vocab.OrderType.valueOf(req.type());
    return billing.create(user, type, req.planId(), req.videoId(), req.paymentMethod());
  }

  @GetMapping("/payment-channels")
  public List<Models.PaymentChannel> channels() {
    return billing.channels();
  }

  @GetMapping("/orders/{id}")
  public Models.OrderView get(@PathVariable Long id) {
    return billing.get(id, CurrentUser.require());
  }

  @PostMapping("/orders/{id}/check")
  public Models.OrderView check(@PathVariable Long id) {
    return billing.check(id, CurrentUser.require());
  }

  @GetMapping("/orders")
  public List<Models.OrderView> mine() {
    return billing.mine(CurrentUser.require());
  }

  @PostMapping("/orders/{id}/cancel")
  public Models.OrderView cancel(@PathVariable Long id) {
    return billing.cancel(id, CurrentUser.require());
  }
}
