package com.example.authservice.user.service;

import com.example.authservice.user.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserService {

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID userId);

    User getById(UUID userId);  // Returns User or null

    User createUser(User user);

    void activateUser(UUID userId);
}

