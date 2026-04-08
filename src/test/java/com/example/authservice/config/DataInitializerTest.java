package com.example.authservice.config;

import com.example.authservice.role.entity.Role;
import com.example.authservice.role.entity.RoleName;
import com.example.authservice.role.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private RoleRepository roleRepository;

    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        dataInitializer = new DataInitializer(roleRepository);
    }

    @Test
    void shouldCreateAllDefaultRolesWhenNoneExist() {
        // Given
        when(roleRepository.findByCode("CUSTOMER")).thenReturn(Optional.empty());
        when(roleRepository.findByCode("STAFF")).thenReturn(Optional.empty());
        when(roleRepository.findByCode("ADMIN")).thenReturn(Optional.empty());

        // When
        dataInitializer.initializeDefaultRoles();

        // Then
        verify(roleRepository, times(3)).save(any(Role.class));
    }

    @Test
    void shouldNotCreateRolesWhenTheyExist() {
        // Given
        Role existingRole = new Role();
        when(roleRepository.findByCode("CUSTOMER")).thenReturn(Optional.of(existingRole));
        when(roleRepository.findByCode("STAFF")).thenReturn(Optional.of(existingRole));
        when(roleRepository.findByCode("ADMIN")).thenReturn(Optional.of(existingRole));

        // When
        dataInitializer.initializeDefaultRoles();

        // Then
        verify(roleRepository, times(0)).save(any(Role.class));
    }
}