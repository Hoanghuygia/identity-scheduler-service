package com.example.authservice.user.repository;

import com.example.authservice.user.entity.UserRole;
import com.example.authservice.user.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
}

