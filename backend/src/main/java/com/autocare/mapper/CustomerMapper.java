package com.autocare.mapper;

import com.autocare.domain.Customer;
import com.autocare.dto.CustomerResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between Customer entities and Customer DTOs.
 */
@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer) {
        if (customer == null) {
            return null;
        }
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getLoyaltyPoints(),
                customer.getVehicles() != null ? customer.getVehicles().size() : 0,
                customer.getCreatedAt() != null ? customer.getCreatedAt().toString() : null
        );
    }
}
