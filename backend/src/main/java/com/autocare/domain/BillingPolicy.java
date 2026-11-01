package com.autocare.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable domain value object holding configurable billing parameters.
 * Allows domain rules (coupon validation, discount limits) to operate without
 * direct dependencies on Spring @Configuration or database repositories.
 */
public record BillingPolicy(
        double maxUnapprovedDiscountPct,
        double laborRatePerHour,
        double defaultTaxRate,
        Map<String, Double> coupons
) {

    public BillingPolicy {
        if (maxUnapprovedDiscountPct < 0 || maxUnapprovedDiscountPct > 100) {
            throw new IllegalArgumentException("Max unapproved discount percentage must be between 0 and 100");
        }
        if (laborRatePerHour < 0) {
            throw new IllegalArgumentException("Labor rate per hour cannot be negative");
        }
        if (defaultTaxRate < 0) {
            throw new IllegalArgumentException("Default tax rate cannot be negative");
        }
        if (coupons != null) {
            Map<String, Double> normalized = new HashMap<>();
            coupons.forEach((k, v) -> {
                if (k != null && v != null && v >= 0) {
                    normalized.put(k.trim().toUpperCase(), v);
                }
            });
            coupons = Collections.unmodifiableMap(normalized);
        } else {
            coupons = Collections.emptyMap();
        }
    }

    /**
     * Factory providing standard system defaults matching docs/01-requirements.md §3.
     */
    public static BillingPolicy defaultPolicy() {
        Map<String, Double> defaultCoupons = new HashMap<>();
        defaultCoupons.put("SAVE10", 10.00);
        defaultCoupons.put("WELCOME20", 20.00);
        defaultCoupons.put("SPRING5", 5.00);
        return new BillingPolicy(10.0, 20.00, 8.0, defaultCoupons);
    }

    public boolean hasCoupon(String couponCode) {
        if (couponCode == null) return false;
        return coupons.containsKey(couponCode.trim().toUpperCase());
    }

    public double getMaxCouponReduction(String couponCode) {
        if (!hasCoupon(couponCode)) return 0.0;
        return coupons.get(couponCode.trim().toUpperCase());
    }

    public boolean requiresManagerApproval(double percentage) {
        return percentage > maxUnapprovedDiscountPct;
    }
}
