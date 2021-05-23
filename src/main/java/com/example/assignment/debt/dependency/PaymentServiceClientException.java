/*
 * Copyright 2020 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.example.assignment.debt.dependency;

import lombok.Builder;
import lombok.Getter;

/**
 * @author Yihao Wang (yihwang@expedia.com)
 */
@Getter
@Builder
public class PaymentServiceClientException extends Exception {
    private final String statusCode;
    private final String message;

    public PaymentServiceClientException(String statusCode, String message) {
        super(message);
        this.message = message;
        this.statusCode = statusCode;
    }
}
