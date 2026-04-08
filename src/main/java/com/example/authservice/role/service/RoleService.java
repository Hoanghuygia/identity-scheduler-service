package com.example.authservice.role.service;

import com.example.authservice.role.entity.Role;

import java.util.Optional;
import java.util.UUID;

public interface RoleService {

    Optional<Role> findByCode(String code);

    void assignCustomerRole(UUID userId);
}

