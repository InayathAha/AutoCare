package com.autocare.config;

import com.autocare.domain.BillingPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration creating the immutable domain BillingPolicy value object
 * from externalized BillingProperties.
 */
@Configuration
@EnableConfigurationProperties(BillingProperties.class)
public class BillingConfig {

    @Bean
    public BillingPolicy billingPolicy(BillingProperties properties) {
        BillingProperties.Billing b = properties.getBilling();
        return new BillingPolicy(
                b.getMaxUnapprovedDiscountPct(),
                b.getLaborRatePerHour(),
                b.getDefaultTaxRate(),
                b.getCoupons()
        );
    }
}
