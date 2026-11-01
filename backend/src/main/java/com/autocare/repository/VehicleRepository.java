package com.autocare.repository;

import com.autocare.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for polymorphic Vehicle hierarchy (Car, Van, Motorcycle).
 * Hibernate automatically materializes concrete subclasses based on the vehicle_type discriminator column.
 */
@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {

    /**
     * Finds all vehicles registered to a specific customer.
     * Demonstrates polymorphic return: the list can contain Car, Van, and Motorcycle instances.
     *
     * @param customerId unique ID of the customer
     * @return list of vehicles owned by the customer
     */
    @Query("SELECT v FROM Vehicle v WHERE v.owner.id = :customerId")
    List<Vehicle> findByOwnerId(@Param("customerId") String customerId);
}
