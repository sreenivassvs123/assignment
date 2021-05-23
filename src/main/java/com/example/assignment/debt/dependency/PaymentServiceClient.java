package com.example.assignment.debt.dependency;

import com.example.assignment.debt.dependency.model.Debt;
import com.example.assignment.debt.dependency.model.Payment;
import com.example.assignment.debt.dependency.model.PaymentPlan;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * FeignClient for PaymentService API (TrueAccord mock endpoint)
 *
 * @author Sreeni Sannuthi
 */
@FeignClient(name = "paymentServiceClient", configuration = PaymentServiceClientConfiguration.class,
        url = "${paymentService.url}")
public interface PaymentServiceClient {

    @RequestMapping(method = RequestMethod.GET, value = "debts",
            headers = {"Accept=application/json"})
    List<Debt> getAllDebts() throws PaymentServiceClientException;

    @RequestMapping(method = RequestMethod.GET, value = "payment_plans",
            headers = {"Accept=application/json"})
    List<PaymentPlan> getAllPaymentPlans() throws PaymentServiceClientException;

    @RequestMapping(method = RequestMethod.GET, value = "payments",
            headers = {"Accept=application/json"})
    List<Payment> getAllPayments() throws PaymentServiceClientException;
}
