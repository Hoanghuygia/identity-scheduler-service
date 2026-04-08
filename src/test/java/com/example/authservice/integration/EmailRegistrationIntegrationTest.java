package com.example.authservice.integration;

import com.example.authservice.auth.dto.RegisterRequest;
import com.example.authservice.role.entity.RoleName;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.entity.UserStatus;
import com.example.authservice.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @Transactional
    void shouldRegisterUserWithPendingStatusAndCustomerRole() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest(
            "test@example.com",
            "SecureP@ssw0rd123",
            "John Doe",
            "+1-555-123-4567"
        );

        // When
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));

        // Then
        // Flush and clear entity manager to ensure fresh data
        entityManager.flush();
        entityManager.clear();
        
        User savedUser = userRepository.findByEmail("test@example.com").orElseThrow();
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING);
        
        // Force lazy loading by accessing the collection
        savedUser.getUserRoles().size(); // This will trigger the lazy load
        assertThat(savedUser.getUserRoles()).hasSize(1);
        assertThat(savedUser.getUserRoles().iterator().next().getRole().getCode()).isEqualTo(RoleName.CUSTOMER.name());
    }
}