package com.example.authservice.role.service;

import com.example.authservice.role.entity.Role;

import java.util.Optional;

public interface RoleService {

    Optional<Role> findByCode(String code);
}

