package com.example.authservice.auth.service;

import com.example.authservice.auth.dto.LoginRequest;

public interface AuthProviderService {

    AuthPrincipal authenticate(LoginRequest request);
}
