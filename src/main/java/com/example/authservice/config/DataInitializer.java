package com.example.authservice.config;

import com.example.authservice.role.entity.Role;
import com.example.authservice.role.entity.RoleName;
import com.example.authservice.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        initializeDefaultRoles();
    }

    void initializeDefaultRoles() {
        createRoleIfNotExists(RoleName.CUSTOMER, "Customer", "Default customer role for registered users");
        createRoleIfNotExists(RoleName.STAFF, "Staff", "Staff role with elevated privileges");
        createRoleIfNotExists(RoleName.ADMIN, "Admin", "Administrator role with full system access");
        
        log.info("default_roles_initialization_completed");
    }

    private void createRoleIfNotExists(RoleName roleName, String displayName, String description) {
        String roleCode = roleName.name();
        
        if (roleRepository.findByCode(roleCode).isEmpty()) {
            Role role = new Role();
            role.setId(UUID.randomUUID());
            role.setCode(roleCode);
            role.setName(displayName);
            role.setDescription(description);
            
            roleRepository.save(role);
            log.info("default_role_created role={} id={}", roleCode, role.getId());
        } else {
            log.debug("default_role_exists role={}", roleCode);
        }
    }
}