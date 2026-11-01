package com.autocare.service;

import com.autocare.domain.Customer;
import com.autocare.domain.Vehicle;
import com.autocare.dto.*;
import com.autocare.exception.BusinessRuleViolationException;
import com.autocare.exception.DuplicateResourceException;
import com.autocare.exception.ResourceNotFoundException;
import com.autocare.mapper.CustomerMapper;
import com.autocare.mapper.InvoiceMapper;
import com.autocare.mapper.VehicleFactory;
import com.autocare.mapper.VehicleMapper;
import com.autocare.repository.CustomerRepository;
import com.autocare.repository.InvoiceRepository;
import com.autocare.repository.VehicleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service orchestrating customer and vehicle management use cases.
 * Calls rich domain methods; contains no business rules that belong in the domain.
 */
@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerMapper customerMapper;
    private final VehicleMapper vehicleMapper;
    private final VehicleFactory vehicleFactory;
    private final InvoiceMapper invoiceMapper;

    public CustomerService(CustomerRepository customerRepository,
                           VehicleRepository vehicleRepository,
                           InvoiceRepository invoiceRepository,
                           CustomerMapper customerMapper,
                           VehicleMapper vehicleMapper,
                           VehicleFactory vehicleFactory,
                           InvoiceMapper invoiceMapper) {
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerMapper = customerMapper;
        this.vehicleMapper = vehicleMapper;
        this.vehicleFactory = vehicleFactory;
        this.invoiceMapper = invoiceMapper;
    }

    public CustomerResponse create(CustomerRequest req) {
        if (customerRepository.existsByEmail(req.email().trim())) {
            throw new DuplicateResourceException("A customer with email " + req.email() + " already exists");
        }
        Customer customer = new Customer(UUID.randomUUID().toString(), req.name(), req.phone(), req.email());
        Customer saved = customerRepository.save(customer);
        return customerMapper.toResponse(saved);
    }

    public CustomerResponse update(String id, CustomerRequest req) {
        Customer customer = findCustomerOrThrow(id);
        String newEmail = req.email().trim();
        if (!customer.getEmail().equalsIgnoreCase(newEmail) && customerRepository.existsByEmail(newEmail)) {
            throw new DuplicateResourceException("A customer with email " + req.email() + " already exists");
        }
        customer.setName(req.name());
        customer.setPhone(req.phone());
        customer.setEmail(newEmail);
        return customerMapper.toResponse(customer);
    }

    public void delete(String id) {
        Customer customer = findCustomerOrThrow(id);
        if (customer.getVehicles() != null && !customer.getVehicles().isEmpty()) {
            throw new DataIntegrityViolationException("Cannot delete customer with registered vehicles");
        }
        if (!invoiceRepository.findByCustomerIdOrderByIssuedAtDesc(id).isEmpty()) {
            throw new DataIntegrityViolationException("Cannot delete customer with existing invoices");
        }
        customerRepository.delete(customer);
        customerRepository.flush();
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(String id) {
        return customerMapper.toResponse(findCustomerOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> list(String query) {
        List<Customer> list = (query != null && !query.isBlank())
                ? customerRepository.searchByNameOrEmail(query.trim())
                : customerRepository.findAll();
        return list.stream().map(customerMapper::toResponse).toList();
    }

    public VehicleResponse registerVehicle(String customerId, VehicleRequest req) {
        String plate = req.licensePlate().trim().toUpperCase();
        if (vehicleRepository.existsById(plate)) {
            throw new DuplicateResourceException("Vehicle with license plate " + plate + " already exists");
        }
        Customer customer = findCustomerOrThrow(customerId);
        Vehicle vehicle = vehicleFactory.createVehicle(req, customer);
        customer.registerVehicle(vehicle); // Invariant enforcement inside Customer domain model
        vehicleRepository.save(vehicle);
        return vehicleMapper.toResponse(vehicle);
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getCustomerVehicles(String customerId) {
        findCustomerOrThrow(customerId);
        return vehicleRepository.findByOwnerId(customerId).stream()
                .map(vehicleMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicle(String licensePlate) {
        String plate = licensePlate.trim().toUpperCase();
        Vehicle vehicle = vehicleRepository.findById(plate)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + plate));
        return vehicleMapper.toResponse(vehicle);
    }

    public RedeemResponse redeemPoints(String customerId, int points) {
        Customer customer = findCustomerOrThrow(customerId);
        boolean redeemed = customer.redeemPoints(points); // Domain invariant enforcement
        if (!redeemed) {
            throw new BusinessRuleViolationException("Insufficient loyalty points for redemption (available: " + customer.getLoyaltyPoints() + ")");
        }
        return new RedeemResponse(true, customer.getLoyaltyPoints());
    }

    @Transactional(readOnly = true)
    public List<InvoiceSummary> serviceHistory(String customerId) {
        findCustomerOrThrow(customerId);
        return invoiceRepository.findByCustomerIdOrderByIssuedAtDesc(customerId).stream()
                .map(invoiceMapper::toSummary)
                .toList();
    }

    private Customer findCustomerOrThrow(String id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }
}
