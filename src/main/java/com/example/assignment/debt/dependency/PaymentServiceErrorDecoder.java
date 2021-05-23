/*
 * Copyright 2020 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.example.assignment.debt.dependency;

import feign.Response;
import feign.RetryableException;
import feign.Util;
import feign.codec.ErrorDecoder;
import org.springframework.util.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import static java.util.Locale.US;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * PaymentService client error decoder
 *
 * @author Sreeni Sannuthi
 */
public class PaymentServiceErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() >= 500 && response.status() < 600) {
            return new RetryableException(response.status(), "Service unavailable", response.request().httpMethod(),
                    this.convertRetryAfterStringToDate(firstOrNull(response.headers(), Util.RETRY_AFTER)), null);
        }
        return new PaymentServiceClientException(String.valueOf(response.status()), response.reason());
    }

    private <T> T firstOrNull(Map<String, Collection<T>> map, String key) {
        if (map.containsKey(key) && !map.get(key).isEmpty()) {
            return map.get(key).iterator().next();
        }
        return null;
    }

    private Date convertRetryAfterStringToDate(String retryAfter) {
        DateFormat rfc822Format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", US);
        if (!StringUtils.hasText(retryAfter)) {
            return null;
        }
        if (retryAfter.matches("^[0-9]+$")) {
            long deltaMillis = SECONDS.toMillis(Long.parseLong(retryAfter));
            return new Date(System.currentTimeMillis() + deltaMillis);
        }
        try {
            return rfc822Format.parse(retryAfter);
        }
        catch (ParseException ignored) {
            return null;
        }
    }
}
