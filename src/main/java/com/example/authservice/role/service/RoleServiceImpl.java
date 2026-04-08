package com.example.authservice.role.service;

import com.example.authservice.role.entity.Role;
import com.example.authservice.role.entity.RoleName;
import com.example.authservice.role.repository.RoleRepository;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.entity.UserRole;
import com.example.authservice.user.entity.UserRoleId;
import com.example.authservice.user.repository.UserRepository;
import com.example.authservice.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public Optional<Role> findByCode(String code) {
        return roleRepository.findByCode(code);
    }

    @Override
    @Transactional
    public void assignCustomerRole(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Role customerRole = roleRepository.findByCode(RoleName.CUSTOMER.name())
            .orElseGet(() -> createCustomerRole());

        UserRole userRole = new UserRole();
        UserRoleId userRoleId = new UserRoleId();
        userRoleId.setUserId(userId);
        userRoleId.setRoleId(customerRole.getId());
        userRole.setId(userRoleId);
        userRole.setUser(user);
        userRole.setRole(customerRole);
        
        userRoleRepository.save(userRole);

        log.info("role_assigned user_id={} role={}", userId, RoleName.CUSTOMER.name());
    }

    private Role createCustomerRole() {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setCode(RoleName.CUSTOMER.name());
        role.setName("Customer");
        role.setDescription("Default customer role for registered users");
        
        Role savedRole = roleRepository.save(role);
        log.info("role_created role={} id={}", RoleName.CUSTOMER.name(), savedRole.getId());
        
        return savedRole;
    }
}

