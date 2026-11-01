package com.autocare.domain;

/**
 * Demonstrates Interface Abstraction:
 * Contract for any entity in the garage system capable of producing a billable total
 * and an itemized printable receipt.
 */
public interface Billable {

    /**
     * Calculates the final payable amount including all items, fees, discounts, and taxes.
     *
     * @return net payable total amount
     */
    double calculateTotal();

    /**
     * Formats and produces a complete itemized textual receipt.
     *
     * @return formatted receipt text
     */
    String generateReceipt();
}
