package com.autocare.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Externalized configuration properties for billing and loyalty rules.
 * Mapped to 'autocare' prefix in application.yml.
 */
@ConfigurationProperties(prefix = "autocare")
public class BillingProperties {

    private Billing billing = new Billing();
    private Loyalty loyalty = new Loyalty();

    public Billing getBilling() {
        return billing;
    }

    public void setBilling(Billing billing) {
        this.billing = billing;
    }

    public Loyalty getLoyalty() {
        return loyalty;
    }

    public void setLoyalty(Loyalty loyalty) {
        this.loyalty = loyalty;
    }

    public static class Billing {
        private double laborRatePerHour = 20.00;
        private double defaultTaxRate = 8.00;
        private double maxUnapprovedDiscountPct = 10.00;
        private Map<String, Double> coupons = new HashMap<>(Map.of(
                "SAVE10", 10.00,
                "WELCOME20", 20.00,
                "SPRING5", 5.00
        ));

        public double getLaborRatePerHour() {
            return laborRatePerHour;
        }

        public void setLaborRatePerHour(double laborRatePerHour) {
            this.laborRatePerHour = laborRatePerHour;
        }

        public double getDefaultTaxRate() {
            return defaultTaxRate;
        }

        public void setDefaultTaxRate(double defaultTaxRate) {
            this.defaultTaxRate = defaultTaxRate;
        }

        public double getMaxUnapprovedDiscountPct() {
            return maxUnapprovedDiscountPct;
        }

        public void setMaxUnapprovedDiscountPct(double maxUnapprovedDiscountPct) {
            this.maxUnapprovedDiscountPct = maxUnapprovedDiscountPct;
        }

        public Map<String, Double> getCoupons() {
            return coupons;
        }

        public void setCoupons(Map<String, Double> coupons) {
            this.coupons = coupons;
        }
    }

    public static class Loyalty {
        private double pointsPerCurrencyUnit = 0.1;

        public double getPointsPerCurrencyUnit() {
            return pointsPerCurrencyUnit;
        }

        public void setPointsPerCurrencyUnit(double pointsPerCurrencyUnit) {
            this.pointsPerCurrencyUnit = pointsPerCurrencyUnit;
        }
    }
}
