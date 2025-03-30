package com.fmsp.dataregister.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoadDataConfig {

    @Value("${paypal.cancelUrl}")
    public String cancelUrl;

    @Value("${paypal.successUrl}")
    public String successUrl;

    @Value("${paypal.value}")
    public String paypalValue;

    @Value("${paypal.currency}")
    public String paypalCurrency;

    public String getCancelUrl() {
        return cancelUrl;
    }

    public void setCancelUrl(String cancelUrl) {
        this.cancelUrl = cancelUrl;
    }

    public String getSuccessUrl() {
        return successUrl;
    }

    public void setSuccessUrl(String successUrl) {
        this.successUrl = successUrl;
    }
}
