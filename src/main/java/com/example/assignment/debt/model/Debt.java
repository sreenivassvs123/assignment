package com.example.assignment.debt.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Sreeni Sannuthi
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Debt {
    private Integer id;
    private BigDecimal amount;
    /**
     * true when the debt is associated with an active payment plan.
     * false when there is no payment plan, or the payment plan is completed.
     */
    private boolean isInPaymentPlan;
    /**
     * If no payment-plan associated, original debt amount.
     * Otherwise, payment-plan's amount_to_pay - any payments made
     */
    private BigDecimal remainingAmount;
    /**
     * If debt is paid off -> NULL
     * Else
     *      If any payment made, then its next installment date
     *      else, start date + installment_frequency period
     */
    private String nextPaymentDueDate;
}