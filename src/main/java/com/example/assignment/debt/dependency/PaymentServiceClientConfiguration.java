/*
 * Copyright 2020 Expedia, Inc. All rights reserved.
 * EXPEDIA PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.example.assignment.debt.dependency;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import feign.slf4j.Slf4jLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * @author Sreeni Sannuthi
 */
public class PaymentServiceClientConfiguration {

    @Value("${paymentService.retryLimit}")
    private int retryCount;
    @Value("${paymentService.connectionTimeoutMillis}")
    private int connectionTimeoutMillis;
    @Value("${paymentService.readTimeoutMillis}")
    private int readTimeoutMillis;
    @Value("${paymentService.retryLatencyInMillis}")
    private int retryLatencyInMillis;

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public Logger logger() {
        return new Slf4jLogger();
    }

    @Bean
    public Retryer.Default defaultRetryer() {
        return new Retryer.Default(retryLatencyInMillis, retryLatencyInMillis, retryCount);
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new PaymentServiceErrorDecoder();
    }

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(connectionTimeoutMillis, readTimeoutMillis);
    }
}
