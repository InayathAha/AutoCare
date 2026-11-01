package com.autocare.repository;

import com.autocare.domain.Invoice;
import com.autocare.domain.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Invoice aggregate root.
 * Demonstrates query methods for invoice status filtering, customer service history,
 * and fetch-joined aggregate retrieval.
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    /**
     * Finds all invoices for a customer, ordered newest first by issued date.
     * Used for customer service history lookups.
     *
     * @param customerId ID of the customer
     * @return list of invoices ordered by issued_at DESC
     */
    @Query("SELECT i FROM Invoice i WHERE i.customer.id = :customerId ORDER BY i.issuedAt DESC NULLS LAST, i.invoiceId DESC")
    List<Invoice> findByCustomerIdOrderByIssuedAtDesc(@Param("customerId") String customerId);

    /**
     * Finds all invoices matching a specific payment status.
     *
     * @param status payment status (OPEN, ISSUED, PAID)
     * @return list of matching invoices
     */
    List<Invoice> findByStatus(InvoiceStatus status);

    /**
     * Loads an invoice and eagerly fetches its associated service tasks and customer.
     * Prevents N+1 queries when building full invoice responses.
     *
     * @param invoiceId unique invoice ID
     * @return optional containing the fetched invoice
     */
    @Query("SELECT DISTINCT i FROM Invoice i " +
            "JOIN FETCH i.customer " +
            "JOIN FETCH i.vehicle " +
            "LEFT JOIN FETCH i.taskList t " +
            "WHERE i.invoiceId = :invoiceId")
    Optional<Invoice> findByIdWithTasksAndDetails(@Param("invoiceId") String invoiceId);
}
