package com.autocare.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.util.Objects;

/**
 * Demonstrates Abstraction and Inheritance:
 * An abstract base mapped superclass representing common person attributes
 * across customers and employees in the AutoCare system.
 * <p>
 * Demonstrates Encapsulation: fields are protected with explicit validation
 * in the constructor and mutators; state cannot be put into an invalid state.
 */
@MappedSuperclass
public abstract class Person {

    @Id
    @Column(nullable = false, length = 36)
    protected String id;

    @Column(name = "name", nullable = false, length = 100)
    protected String name;

    @Column(name = "phone", nullable = false, length = 20)
    protected String phone;

    @Column(name = "email", nullable = false, length = 100, unique = true)
    protected String email;

    /**
     * Protected no-arg constructor required by JPA specification.
     */
    protected Person() {
    }

    /**
     * Parameterized constructor enforcing non-null and non-blank invariants.
     *
     * @param id    unique identifier (UUID)
     * @param name  full personal name
     * @param phone primary contact telephone
     * @param email unique email address
     */
    public Person(String id, String name, String phone, String email) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Person ID cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Person name cannot be null or blank");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Person phone cannot be null or blank");
        }
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("A valid email address is required");
        }
        this.id = id.trim();
        this.name = name.trim();
        this.phone = phone.trim();
        this.email = email.trim();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    /**
     * Returns concatenated contact details per UML requirement: name, phone, email.
     */
    public String getContactDetails() {
        return name + ", " + phone + ", " + email;
    }

    /**
     * Demonstrates dynamic polymorphism: abstract method to display the person's profile.
     * Concrete subclasses provide specialized output.
     */
    public abstract void displayProfile();

    /**
     * Helper method returning a formatted summary of the person's profile for testing and logging.
     */
    public abstract String describe();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return Objects.equals(id, person.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
