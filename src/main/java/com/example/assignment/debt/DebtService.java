package com.example.assignment.debt;

import com.example.assignment.debt.dependency.PaymentServiceClient;
import com.example.assignment.debt.dependency.PaymentServiceClientException;
import com.example.assignment.debt.dependency.model.InstallmentFrequency;
import com.example.assignment.debt.dependency.model.Payment;
import com.example.assignment.debt.dependency.model.PaymentPlan;
import com.example.assignment.debt.model.Debt;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Sreeni Sannuthi
 */
@Service
public class DebtService {

    private final PaymentServiceClient paymentServiceClient;

    ObjectMapper mapper = new ObjectMapper();

    public DebtService(PaymentServiceClient paymentServiceClient) {
        this.paymentServiceClient = paymentServiceClient;
    }

    @Scheduled(cron = "0 0/1 * ? * *") //Runs every 5min
    public List<Debt> getAllDebts() throws PaymentServiceClientException, JsonProcessingException {
        List<com.example.assignment.debt.dependency.model.Debt> paymentServiceDebts =
                paymentServiceClient.getAllDebts();

        List<Debt> debts = new ArrayList<>();
        List<PaymentPlan> paymentPlans = paymentServiceClient.getAllPaymentPlans();
        List<Payment> payments = paymentServiceClient.getAllPayments();
        Map<Integer, PaymentPlan> debtIdPaymentPlanMap =
                buildDebtIdPaymentPlanMap(paymentServiceDebts, paymentPlans);
        Map<Integer, List<Payment>> paymentPlanIdPaymentsMap = buildPaymentPlanIdPaymentsMap(paymentPlans, payments);
        for(com.example.assignment.debt.dependency.model.Debt paymentServiceDebt: paymentServiceDebts) {
            PaymentPlan debtAssociatedPaymentPlan = debtIdPaymentPlanMap.get(paymentServiceDebt.getId());
            boolean isInPaymentPlan = false;
            BigDecimal remainingAmount = paymentServiceDebt.getAmount();
            String nextPaymentDueDate = null;
            if(debtAssociatedPaymentPlan != null) {
                List<Payment> paymentPlanAssociatedPayments =
                        paymentPlanIdPaymentsMap.get(debtAssociatedPaymentPlan.getDebtId());
                remainingAmount = debtAssociatedPaymentPlan.getAmountToPay()
                        .subtract(paymentPlanAssociatedPayments.stream()
                                .map(Payment::getAmount).reduce(BigDecimal::add).get());
                if(BigDecimal.ZERO.compareTo(remainingAmount) < 0) {
                    isInPaymentPlan = true;
                    LocalDate lastPaymentDate =
                            paymentPlanAssociatedPayments.stream().map(Payment::getDate).max(LocalDate::compareTo).get();
                    if (InstallmentFrequency.WEEKLY.toString()
                            .equals(debtAssociatedPaymentPlan.getInstallmentFrequency())) {
                        nextPaymentDueDate = lastPaymentDate.plusDays(7).toString();
                    } else if (InstallmentFrequency.BI_WEEKLY.toString()
                            .equals(debtAssociatedPaymentPlan.getInstallmentFrequency())) {
                        nextPaymentDueDate = lastPaymentDate.plusDays(14).toString();
                    }
                }
            }
            Debt debt = Debt.builder()
                    .amount(paymentServiceDebt.getAmount())
                    .id(paymentServiceDebt.getId())
                    .isInPaymentPlan(isInPaymentPlan)
                    .remainingAmount(remainingAmount)
                    .nextPaymentDueDate(nextPaymentDueDate)
                    .build();
            System.out.println(mapper.writer().writeValueAsString(debt));
            debts.add(debt);
        }
        return debts;
    }

    protected Map<Integer, PaymentPlan> buildDebtIdPaymentPlanMap(
                                                        List<com.example.assignment.debt.dependency.model.Debt> debts,
                                                        List<PaymentPlan> paymentPlans) {
        Map<Integer, PaymentPlan> debtIdPaymentPlanMap = new HashMap<>();
        for (com.example.assignment.debt.dependency.model.Debt debt : debts) {
            for (PaymentPlan paymentPlan : paymentPlans) {
                if (Objects.equals(debt.getId(), paymentPlan.getDebtId())) {
                    //Assumption: debts will be associated with only one payment plan.
                    debtIdPaymentPlanMap.put(debt.getId(), paymentPlan);
                }
            }
        }
        return debtIdPaymentPlanMap;
    }

    protected Map<Integer, List<Payment>> buildPaymentPlanIdPaymentsMap(List<PaymentPlan> paymentPlans,
                                                                      List<Payment> payments) {
        Map<Integer, List<Payment>> paymentPlanIdPaymentsMap = new HashMap<>();
        for (PaymentPlan paymentPlan : paymentPlans) {
            List<Payment> paymentPlanAssociatedPayments = new ArrayList<>();
            for (Payment payment : payments) {
                if (Objects.equals(paymentPlan.getId(), payment.getPaymentPlanId())) {
                    paymentPlanAssociatedPayments.add(payment);
                }
            }
            if (!CollectionUtils.isEmpty(paymentPlanAssociatedPayments)) {
                paymentPlanIdPaymentsMap.put(paymentPlan.getId(), paymentPlanAssociatedPayments);
            }
        }
        return paymentPlanIdPaymentsMap;
    }
}
