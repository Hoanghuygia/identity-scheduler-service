package com.example.authservice.role.service;

import com.example.authservice.role.entity.Role;
import com.example.authservice.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Override
    public Optional<Role> findByCode(String code) {
        return roleRepository.findByCode(code);
    }

    @Override
    public void assignCustomerRole(UUID userId) {
        // TODO: Implement customer role assignment
        // This is a stub implementation for testing
    }
}

