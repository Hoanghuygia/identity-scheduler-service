package com.example.authservice.role.service;

import com.example.authservice.role.entity.Role;
import com.example.authservice.role.entity.RoleName;
import com.example.authservice.role.repository.RoleRepository;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.entity.UserRole;
import com.example.authservice.user.repository.UserRepository;
import com.example.authservice.user.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    private RoleServiceImpl roleService;

    @BeforeEach
    void setUp() {
        // This will fail until I implement the proper constructor
        roleService = new RoleServiceImpl(roleRepository, userRepository, userRoleRepository);
    }

    @Test
    void shouldAssignCustomerRoleWhenRoleExists() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        
        Role customerRole = new Role();
        customerRole.setId(UUID.randomUUID());
        customerRole.setCode(RoleName.CUSTOMER.name());
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode(RoleName.CUSTOMER.name())).thenReturn(Optional.of(customerRole));

        // When
        roleService.assignCustomerRole(userId);

        // Then
        verify(userRepository).findById(userId);
        verify(roleRepository).findByCode(RoleName.CUSTOMER.name());
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    void shouldCreateCustomerRoleWhenNotExists() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        
        Role newCustomerRole = new Role();
        newCustomerRole.setId(UUID.randomUUID());
        newCustomerRole.setCode(RoleName.CUSTOMER.name());
        newCustomerRole.setName("Customer");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode(RoleName.CUSTOMER.name())).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(newCustomerRole);

        // When
        roleService.assignCustomerRole(userId);

        // Then
        verify(roleRepository).save(any(Role.class));
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(RuntimeException.class, () -> {
            roleService.assignCustomerRole(userId);
        });
    }
}