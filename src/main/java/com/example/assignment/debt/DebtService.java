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

    private final ObjectMapper mapper;

    public DebtService(PaymentServiceClient paymentServiceClient) {
        this.paymentServiceClient = paymentServiceClient;
        this.mapper = new ObjectMapper();
    }

    /**
     * Prints and fetches debts information as expected by calling TrueAccord endpoints for the data.
     * Logic written according to the Problem statement given in
     * https://gist.github.com/jeffling/2dd661ff8398726883cff09839dc316c
     *
     * @return
     * @throws PaymentServiceClientException
     * @throws JsonProcessingException
     */
    @Scheduled(fixedRate = 300_000) //Runs every 5min
    public List<Debt> getAllDebts() throws PaymentServiceClientException, JsonProcessingException {

        List<com.example.assignment.debt.dependency.model.Debt> paymentServiceDebts =
                paymentServiceClient.getAllDebts();
        List<PaymentPlan> paymentPlans = paymentServiceClient.getAllPaymentPlans();
        List<Payment> payments = paymentServiceClient.getAllPayments();

        Map<Integer, PaymentPlan> debtIdPaymentPlanMap =
                buildDebtIdPaymentPlanMap(paymentServiceDebts, paymentPlans);
        Map<Integer, List<Payment>> paymentPlanIdPaymentsMap = buildPaymentPlanIdPaymentsMap(paymentPlans, payments);

        List<Debt> debts = new ArrayList<>();
        if (paymentServiceDebts != null) {
            for (com.example.assignment.debt.dependency.model.Debt paymentServiceDebt : paymentServiceDebts) {
                PaymentPlan debtAssociatedPaymentPlan = debtIdPaymentPlanMap.get(paymentServiceDebt.getId());
                boolean isInPaymentPlan = false;
                BigDecimal remainingAmount = paymentServiceDebt.getAmount();
                String nextPaymentDueDate = null;
                if (debtAssociatedPaymentPlan != null) {
                    List<Payment> paymentPlanAssociatedPayments =
                            paymentPlanIdPaymentsMap.get(debtAssociatedPaymentPlan.getId());
                    // If no payments done till now, remainingAmount is paymentPlan's amountToPay
                    // otherwise paymentPlan's amountToPay - paid amount
                    remainingAmount = paymentPlanAssociatedPayments == null ?
                            debtAssociatedPaymentPlan.getAmountToPay()
                            : debtAssociatedPaymentPlan.getAmountToPay()
                            .subtract(paymentPlanAssociatedPayments.stream()
                                    .map(Payment::getAmount).reduce(BigDecimal::add).get());
                    if (BigDecimal.ZERO.compareTo(remainingAmount) < 0) {
                        isInPaymentPlan = true;
                        // If no payments done till now, paymentPlan's start-date is the next nextPaymentDueDate
                        // otherwise recent payment's date + installment-frequency duration
                        LocalDate lastPaymentDate = paymentPlanAssociatedPayments == null ?
                                debtAssociatedPaymentPlan.getStartDate() :
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
        }
        return debts;
    }

    /**
     * Helper method to build a map with key as debt id and value as the paymentPlan for that debt.
     * @param debts
     * @param paymentPlans
     * @return
     */
    private Map<Integer, PaymentPlan> buildDebtIdPaymentPlanMap(
                                                        List<com.example.assignment.debt.dependency.model.Debt> debts,
                                                        List<PaymentPlan> paymentPlans) {
        Map<Integer, PaymentPlan> debtIdPaymentPlanMap = new HashMap<>();
        //Assumption: debts will be associated with maximum of one payment plan.
        if (debts != null && paymentPlans != null) {
            debts.forEach(debt -> {
                        PaymentPlan paymentPlan =
                                paymentPlans.stream().filter(paymentPlan1 ->
                                        debt.getId().equals(paymentPlan1.getDebtId()))
                                        .findFirst().orElse(null);
                        debtIdPaymentPlanMap.put(debt.getId(), paymentPlan);
                    }
            );
        }
        return debtIdPaymentPlanMap;
    }

    /**
     * Helper method to build a map with key as paymentPlan id and value as the list of payments made for that
     * paymentPlan.
     * @param paymentPlans
     * @param payments
     * @return
     */
    private Map<Integer, List<Payment>> buildPaymentPlanIdPaymentsMap(List<PaymentPlan> paymentPlans,
                                                                      List<Payment> payments) {
        Map<Integer, List<Payment>> paymentPlanIdPaymentsMap = new HashMap<>();
        if (paymentPlans != null && payments != null) {
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
        }
        return paymentPlanIdPaymentsMap;
    }
}
