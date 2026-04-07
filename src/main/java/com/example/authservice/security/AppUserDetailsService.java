package com.example.authservice.security;

import com.example.authservice.user.entity.User;
import com.example.authservice.user.entity.UserStatus;
import com.example.authservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return AuthUserPrincipal.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .password(user.getPasswordHash())
            .roles(user.getUserRoles().stream().map(ur -> ur.getRole().getCode()).collect(java.util.stream.Collectors.toSet()))
            .enabled(UserStatus.ACTIVE.equals(user.getStatus()))
            .build();
    }
}

