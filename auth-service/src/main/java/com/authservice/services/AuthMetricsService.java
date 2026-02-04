package com.authservice.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthMetricsService {

    private final MeterRegistry meterRegistry;

    private static final String AUTH_LOGIN_METRIC = "auth.login";
    private static final String AUTH_REGISTER_METRIC = "auth.register";
    private static final String TAG_KEY_STATUS = "status";
    private static final String TAG_KEY_REASON = "reason";

    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILURE = "failure";
    private static final String REASON_NONE = "none";

    public void incrementLoginSuccess() {
        incrementMetric(STATUS_SUCCESS, REASON_NONE, AUTH_LOGIN_METRIC);
    }

    public void incrementLoginFailure(String reason) {
        incrementMetric(STATUS_FAILURE, reason, AUTH_LOGIN_METRIC);
    }

    public void incrementRegisterSuccess() {
        incrementMetric(STATUS_SUCCESS, REASON_NONE, AUTH_REGISTER_METRIC);
    }

    public void incrementRegisterFailure(String reason) {
        incrementMetric(STATUS_FAILURE, reason, AUTH_REGISTER_METRIC);
    }

    private void incrementMetric(String status, String reason, String metricName) {
        Counter.builder(metricName)
                .tag(TAG_KEY_STATUS, status)
                .tag(TAG_KEY_REASON, reason)
                .register(meterRegistry)
                .increment();
    }
}

