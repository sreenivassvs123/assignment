package com.example.assignment.debt;

import com.example.assignment.debt.dependency.PaymentServiceClient;
import com.example.assignment.debt.dependency.PaymentServiceClientException;
import com.example.assignment.debt.dependency.model.Debt;
import com.example.assignment.debt.dependency.model.InstallmentFrequency;
import com.example.assignment.debt.dependency.model.Payment;
import com.example.assignment.debt.dependency.model.PaymentPlan;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class DebtServiceTest {

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @InjectMocks
    private DebtService debtService;

    @Test
    void testGetDebtsNullResponse() throws PaymentServiceClientException, JsonProcessingException {
        Mockito.when(paymentServiceClient.getAllDebts()).thenReturn(null);
        List<com.example.assignment.debt.model.Debt> debts = debtService.getAllDebts();

        //Verification
        assertEquals(0, debts.size());
    }

    /**
     * Debt with no paymentPlan
     * RemainingAmount is original Debt's amount and NextPaymentDueDate is not known
     * @throws PaymentServiceClientException
     * @throws JsonProcessingException
     */
    @Test
    void testGetDebtsWithNoPaymentPlan() throws PaymentServiceClientException, JsonProcessingException {
        int debtId = 1;
        Debt debt = Debt.builder().id(debtId).amount(BigDecimal.TEN).build();
        Mockito.when(paymentServiceClient.getAllDebts()).thenReturn(Arrays.asList(debt));
        List<com.example.assignment.debt.model.Debt> debts = debtService.getAllDebts();

        //Verification
        assertEquals(1, debts.size());
        assertFalse(debts.get(0).isInPaymentPlan());
        assertNull(debts.get(0).getNextPaymentDueDate());
        assertTrue(debt.getAmount().equals(debts.get(0).getRemainingAmount()));
    }

    /**
     * Debt with PaymentPlan exist but completed as the amountToPay is fully paid off.
     * @throws PaymentServiceClientException
     * @throws JsonProcessingException
     */
    @Test
    void testGetDebtWithFullyPaidPaymentPlan() throws PaymentServiceClientException, JsonProcessingException {
        int debtId = 1;
        Debt debt = Debt.builder().id(debtId).amount(BigDecimal.TEN).build();
        PaymentPlan paymentPlan =
                PaymentPlan.builder()
                        .amountToPay(BigDecimal.TEN)
                        .debtId(debtId)
                        .id(11)
                        .installmentAmount(BigDecimal.valueOf(5))
                        .installmentFrequency(InstallmentFrequency.WEEKLY.name())
                        .startDate(LocalDate.of(2021, 5, 1))
                        .build();
        List<Payment> payments = Arrays.asList(
                Payment.builder()
                        .amount(BigDecimal.valueOf(5))
                        .paymentPlanId(11)
                        .date(LocalDate.of(2021, 5, 8)).build(),
                Payment.builder()
                        .amount(BigDecimal.valueOf(5))
                        .paymentPlanId(11)
                        .date(LocalDate.of(2021, 5, 15)).build());
        Mockito.when(paymentServiceClient.getAllDebts()).thenReturn(Arrays.asList(debt));
        Mockito.when(paymentServiceClient.getAllPaymentPlans()).thenReturn(Arrays.asList(paymentPlan));
        Mockito.when(paymentServiceClient.getAllPayments()).thenReturn(payments);
        List<com.example.assignment.debt.model.Debt> debts = debtService.getAllDebts();

        //Verification
        assertEquals(1, debts.size());
        assertFalse(debts.get(0).isInPaymentPlan());
        assertNull(debts.get(0).getNextPaymentDueDate());
        assertTrue(BigDecimal.ZERO.equals(debts.get(0).getRemainingAmount()));
    }

    /**
     * Debt with PaymentPlan exist and completed but the total amount paid is more than paymentPlan's amountToPay.
     * Debt amount = 10$; PaymentPlan's amountToPay = 10$; Total payments done for the paymentPlan = 55$
     * making 45$ over payment
     * @throws PaymentServiceClientException
     * @throws JsonProcessingException
     */
    @Test
    void testGetDebtWithOverPaidPaymentPlan() throws PaymentServiceClientException, JsonProcessingException {
        int debtId = 1;
        Debt debt = Debt.builder().id(debtId).amount(BigDecimal.TEN).build();
        PaymentPlan paymentPlan =
                PaymentPlan.builder()
                        .amountToPay(BigDecimal.TEN)
                        .debtId(debtId)
                        .id(11)
                        .installmentAmount(BigDecimal.valueOf(5))
                        .installmentFrequency(InstallmentFrequency.WEEKLY.name())
                        .startDate(LocalDate.of(2021, 5, 1))
                        .build();
        List<Payment> payments = Arrays.asList(
                Payment.builder()
                        .amount(BigDecimal.valueOf(5))
                        .paymentPlanId(11)
                        .date(LocalDate.of(2021, 5, 8)).build(),
                Payment.builder()
                        .amount(BigDecimal.valueOf(50))
                        .paymentPlanId(11)
                        .date(LocalDate.of(2021, 5, 15)).build());
        Mockito.when(paymentServiceClient.getAllDebts()).thenReturn(Arrays.asList(debt));
        Mockito.when(paymentServiceClient.getAllPaymentPlans()).thenReturn(Arrays.asList(paymentPlan));
        Mockito.when(paymentServiceClient.getAllPayments()).thenReturn(payments);
        List<com.example.assignment.debt.model.Debt> debts = debtService.getAllDebts();

        //Verification
        assertEquals(1, debts.size());
        assertFalse(debts.get(0).isInPaymentPlan());
        assertNull(debts.get(0).getNextPaymentDueDate());
        assertTrue(BigDecimal.valueOf(-45).equals(debts.get(0).getRemainingAmount()));
    }

    /**
     * Debt with paymentPlan with pending amount and weekly installmentFrequency.
     * @throws PaymentServiceClientException
     * @throws JsonProcessingException
     */
    @Test
    void testGetDebtWithInProgressAndWeeklyPaymentPlan() throws PaymentServiceClientException,
            JsonProcessingException {
        int debtId = 1;
        Debt debt = Debt.builder().id(debtId).amount(BigDecimal.TEN).build();
        PaymentPlan paymentPlan =
                PaymentPlan.builder()
                        .amountToPay(BigDecimal.TEN)
                        .debtId(debtId)
                        .id(11)
                        .installmentAmount(BigDecimal.valueOf(5))
                        .installmentFrequency(InstallmentFrequency.WEEKLY.name())
                        .startDate(LocalDate.of(2021, 5, 1))
                        .build();
        List<Payment> payments = Arrays.asList(
                Payment.builder()
                        .amount(BigDecimal.valueOf(5))
                        .paymentPlanId(11)
                        .date(LocalDate.of(2021, 5, 8)).build());
        Mockito.when(paymentServiceClient.getAllDebts()).thenReturn(Arrays.asList(debt));
        Mockito.when(paymentServiceClient.getAllPaymentPlans()).thenReturn(Arrays.asList(paymentPlan));
        Mockito.when(paymentServiceClient.getAllPayments()).thenReturn(payments);
        List<com.example.assignment.debt.model.Debt> debts = debtService.getAllDebts();

        //Verification
        assertEquals(1, debts.size());
        assertTrue(debts.get(0).isInPaymentPlan());
        assertTrue("2021-05-15".equals(debts.get(0).getNextPaymentDueDate()));
        assertTrue(BigDecimal.valueOf(5).equals(debts.get(0).getRemainingAmount()));
    }

    /**
     * Debt with paymentPlan with pending amount and biWeekly installmentFrequency.
     * @throws PaymentServiceClientException
     * @throws JsonProcessingException
     */
    @Test
    void testGetDebtWithInProgressAndBiWeeklyPaymentPlan() throws PaymentServiceClientException,
            JsonProcessingException {
        int debtId = 1;
        Debt debt = Debt.builder().id(debtId).amount(BigDecimal.TEN).build();
        PaymentPlan paymentPlan =
                PaymentPlan.builder()
                        .amountToPay(BigDecimal.TEN)
                        .debtId(debtId)
                        .id(11)
                        .installmentAmount(BigDecimal.valueOf(5))
                        .installmentFrequency(InstallmentFrequency.BI_WEEKLY.name())
                        .startDate(LocalDate.of(2021, 5, 1))
                        .build();
        List<Payment> payments = Arrays.asList(
                Payment.builder()
                        .amount(BigDecimal.valueOf(5))
                        .paymentPlanId(11)
                        .date(LocalDate.of(2021, 5, 8)).build());
        Mockito.when(paymentServiceClient.getAllDebts()).thenReturn(Arrays.asList(debt));
        Mockito.when(paymentServiceClient.getAllPaymentPlans()).thenReturn(Arrays.asList(paymentPlan));
        Mockito.when(paymentServiceClient.getAllPayments()).thenReturn(payments);
        List<com.example.assignment.debt.model.Debt> debts = debtService.getAllDebts();

        //Verification
        assertEquals(1, debts.size());
        assertTrue(debts.get(0).isInPaymentPlan());
        assertTrue("2021-05-22".equals(debts.get(0).getNextPaymentDueDate()));
        assertTrue(BigDecimal.valueOf(5).equals(debts.get(0).getRemainingAmount()));
    }

    /**
     * Debt with active paymentPlan but no payments done till date.
     * Remaining amount is the paymentPlan's amountToPay; NextPaymentDueDate is the paymentPlan's startDate
     * @throws PaymentServiceClientException
     * @throws JsonProcessingException
     */
    @Test
    void testGetDebtWithInProgressPaymentPlanAndNoPaymentsMadeTillDate() throws PaymentServiceClientException,
            JsonProcessingException {
        int debtId = 1;
        Debt debt = Debt.builder().id(debtId).amount(BigDecimal.valueOf(15)).build();
        PaymentPlan paymentPlan =
                PaymentPlan.builder()
                        .amountToPay(BigDecimal.TEN)
                        .debtId(debtId)
                        .id(11)
                        .installmentAmount(BigDecimal.valueOf(5))
                        .installmentFrequency(InstallmentFrequency.BI_WEEKLY.name())
                        .startDate(LocalDate.of(2021, 5, 1))
                        .build();
        List<Payment> payments = null;
        Mockito.when(paymentServiceClient.getAllDebts()).thenReturn(Arrays.asList(debt));
        Mockito.when(paymentServiceClient.getAllPaymentPlans()).thenReturn(Arrays.asList(paymentPlan));
        Mockito.when(paymentServiceClient.getAllPayments()).thenReturn(payments);
        List<com.example.assignment.debt.model.Debt> debts = debtService.getAllDebts();

        //Verification
        assertEquals(1, debts.size());
        assertTrue(debts.get(0).isInPaymentPlan());
        assertTrue("2021-05-15".equals(debts.get(0).getNextPaymentDueDate()));
        assertTrue(paymentPlan.getAmountToPay().equals(debts.get(0).getRemainingAmount()));
    }
}