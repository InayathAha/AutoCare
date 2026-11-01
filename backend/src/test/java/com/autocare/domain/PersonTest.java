package com.autocare.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Person Hierarchy - Abstract Base Tests")
class PersonTest {

    @Test
    @DisplayName("Person class is strictly abstract and cannot be directly instantiated")
    void personClassIsAbstract() {
        assertThat(Modifier.isAbstract(Person.class.getModifiers()))
                .as("Person must declare abstract modifier")
                .isTrue();
    }

    @Test
    @DisplayName("Concrete subclass inherits encapsulated attributes and contact details")
    void concreteSubclassInheritsAttributes() {
        Person customer = new Customer("c-100", "Alice Smith", "0712345678", "alice@example.com");

        assertThat(customer.getId()).isEqualTo("c-100");
        assertThat(customer.getName()).isEqualTo("Alice Smith");
        assertThat(customer.getPhone()).isEqualTo("0712345678");
        assertThat(customer.getEmail()).isEqualTo("alice@example.com");
        assertThat(customer.getContactDetails()).isEqualTo("Alice Smith, 0712345678, alice@example.com");
    }

    @ParameterizedTest(name = "Invalid email format ''{0}'' is rejected")
    @ValueSource(strings = {"", "   ", "invalid-email-without-at", "plainaddress"})
    @DisplayName("Email validation rejects missing or malformed addresses")
    void invalidEmailIsRejected(String invalidEmail) {
        assertThatThrownBy(() -> new Customer("c-101", "Bob", "0711111111", invalidEmail))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Blank ID or Name in constructor throws IllegalArgumentException")
    void blankIdOrNameIsRejected() {
        assertThatThrownBy(() -> new Customer("", "Bob", "0711111111", "bob@example.com"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new Customer("c-102", "   ", "0711111111", "bob@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
