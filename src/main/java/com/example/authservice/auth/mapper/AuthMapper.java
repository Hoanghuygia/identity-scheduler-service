package com.example.authservice.auth.mapper;

import com.example.authservice.auth.dto.RegisterRequest;
import com.example.authservice.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "emailVerified", constant = "false")
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "userRoles", ignore = true)
    @Mapping(target = "refreshTokens", ignore = true)
    @Mapping(target = "passwordResetTokens", ignore = true)
    @Mapping(target = "emailVerificationTokens", ignore = true)
    @Mapping(target = "authAuditLogs", ignore = true)
    User toUser(RegisterRequest request);
}

