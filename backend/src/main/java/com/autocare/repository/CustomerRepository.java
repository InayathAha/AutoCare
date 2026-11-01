package com.autocare.repository;

import com.autocare.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Customer entity.
 * Demonstrates clean separation between persistence abstraction and domain logic.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    /**
     * Checks if a customer exists with the given email address.
     *
     * @param email customer email
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Finds a customer by email.
     *
     * @param email customer email
     * @return optional containing the customer if found
     */
    Optional<Customer> findByEmail(String email);

    /**
     * Searches customers by name or email containing the query string (case-insensitive).
     *
     * @param name  search pattern for name
     * @param email search pattern for email
     * @return list of matching customers
     */
    @Query("SELECT c FROM Customer c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(c.email) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Customer> searchByNameOrEmail(@Param("q") String query);
}
