package com.example.authservice.role.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleNameTest {

    @Test
    void shouldContainAllRequiredRoles() {
        assertThat(RoleName.CUSTOMER).isNotNull();
        assertThat(RoleName.STAFF).isNotNull();
        assertThat(RoleName.ADMIN).isNotNull();
    }

    @Test
    void shouldHaveCorrectStringValues() {
        assertThat(RoleName.CUSTOMER.toString()).isEqualTo("CUSTOMER");
        assertThat(RoleName.STAFF.toString()).isEqualTo("STAFF");
        assertThat(RoleName.ADMIN.toString()).isEqualTo("ADMIN");
    }
}