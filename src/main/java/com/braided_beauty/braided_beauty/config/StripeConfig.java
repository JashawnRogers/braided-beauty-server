package com.braided_beauty.braided_beauty.config;

import com.braided_beauty.braided_beauty.records.StripeProperties;
import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StripeProperties.class)
public class StripeConfig {
    private final String stripeSecretKey;

    public StripeConfig(StripeProperties props) {
        this.stripeSecretKey = props.secretKey();
    }

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }
}
