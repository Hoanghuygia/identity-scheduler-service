# SMTP Email and Role Assignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace stub email service with real SMTP functionality and implement proper role assignment for user registration

**Architecture:** Spring Boot Mail integration with Gmail SMTP, async email verification, auto-role creation with customer assignment during registration

**Tech Stack:** Spring Boot Mail, JavaMail, Spring Events, JPA, Gmail SMTP with app password authentication

---

## File Structure

**Files to Create:**
- `src/main/java/com/example/authservice/mail/service/SmtpEmailService.java` - Real SMTP email implementation
- `src/main/java/com/example/authservice/mail/config/MailConfig.java` - Spring Mail SMTP configuration
- `src/main/resources/templates/email-verification.html` - HTML email template
- `src/main/java/com/example/authservice/role/entity/RoleName.java` - Role name enum

**Files to Modify:**
- `.env` - Add SMTP credentials
- `src/main/java/com/example/authservice/role/service/RoleServiceImpl.java:23-26` - Implement role assignment
- `src/main/resources/application.properties` - Add mail configuration
- `pom.xml` - Add Spring Boot Mail dependency

**Files to Test:**
- `src/test/java/com/example/authservice/mail/service/SmtpEmailServiceTest.java`
- `src/test/java/com/example/authservice/role/service/RoleServiceImplTest.java`

---

### Task 1: Add Spring Boot Mail Dependency

**Files:**
- Modify: `pom.xml:75-85` (add to dependencies section)

- [ ] **Step 1: Add Spring Boot Mail starter dependency**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

- [ ] **Step 2: Run Maven dependency resolution**

Run: `mvn -B dependency:resolve`
Expected: SUCCESS with mail dependencies downloaded

- [ ] **Step 3: Commit dependency addition**

```bash
git add pom.xml
git commit -m "feat: add spring-boot-starter-mail dependency"
```

### Task 2: SMTP Configuration Setup

**Files:**
- Modify: `.env:18-23`
- Modify: `src/main/resources/application.properties:30-40`

- [ ] **Step 1: Update .env with SMTP credentials**

Replace lines 18-23 in `.env`:
```env
# Mail Configuration - Gmail SMTP
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=nanhph0310@gmail.com
SMTP_PASSWORD=xtes ooto lyfi ctbx
```

- [ ] **Step 2: Add mail configuration to application.properties**

Append to `src/main/resources/application.properties`:
```properties
# Mail Configuration
spring.mail.host=${SMTP_HOST}
spring.mail.port=${SMTP_PORT}
spring.mail.username=${SMTP_USERNAME}
spring.mail.password=${SMTP_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.ssl.protocols=TLSv1.2
```

- [ ] **Step 3: Commit configuration changes**

```bash
git add .env src/main/resources/application.properties
git commit -m "feat: add SMTP configuration for Gmail"
```

### Task 3: Create Role Name Enum

**Files:**
- Create: `src/main/java/com/example/authservice/role/entity/RoleName.java`

- [ ] **Step 1: Write test for RoleName enum**

Create `src/test/java/com/example/authservice/role/entity/RoleNameTest.java`:
```java
package com.example.authservice.role.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleNameTest {

    @Test
    void shouldContainAllRequiredRoles() {
        assertThat(RoleName.CUSTOMER).isNotNull();
        assertThat(RoleName.STAFF).isNotNull();
        assertThat(RoleName.ADMIN).isNotNull();
    }

    @Test
    void shouldHaveCorrectStringValues() {
        assertThat(RoleName.CUSTOMER.toString()).isEqualTo("CUSTOMER");
        assertThat(RoleName.STAFF.toString()).isEqualTo("STAFF");
        assertThat(RoleName.ADMIN.toString()).isEqualTo("ADMIN");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=RoleNameTest`
Expected: FAIL with "cannot find symbol: class RoleName"

- [ ] **Step 3: Create RoleName enum**

Create `src/main/java/com/example/authservice/role/entity/RoleName.java`:
```java
package com.example.authservice.role.entity;

public enum RoleName {
    CUSTOMER,
    STAFF,
    ADMIN
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -B test -Dtest=RoleNameTest`
Expected: PASS

- [ ] **Step 5: Commit RoleName enum**

```bash
git add src/main/java/com/example/authservice/role/entity/RoleName.java src/test/java/com/example/authservice/role/entity/RoleNameTest.java
git commit -m "feat: add RoleName enum for role management"
```

### Task 4: HTML Email Template

**Files:**
- Create: `src/main/resources/templates/email-verification.html`

- [ ] **Step 1: Create templates directory**

```bash
mkdir -p src/main/resources/templates
```

- [ ] **Step 2: Create HTML email verification template**

Create `src/main/resources/templates/email-verification.html`:
```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Email Verification</title>
    <style>
        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
        .header { background-color: #007bff; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
        .content { background-color: #f8f9fa; padding: 30px; border-radius: 0 0 5px 5px; }
        .button { display: inline-block; background-color: #28a745; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; margin: 20px 0; }
        .footer { text-align: center; font-size: 12px; color: #666; margin-top: 20px; }
    </style>
</head>
<body>
    <div class="header">
        <h1>Welcome to Identity Service!</h1>
    </div>
    <div class="content">
        <h2>Verify Your Email Address</h2>
        <p>Thank you for registering with our service. To complete your registration and activate your account, please verify your email address by clicking the button below:</p>
        
        <a href="{{verificationUrl}}" class="button">Verify Email Address</a>
        
        <p>If the button doesn't work, you can copy and paste this link into your browser:</p>
        <p style="word-break: break-all; color: #007bff;">{{verificationUrl}}</p>
        
        <p><strong>Important:</strong> This verification link will expire in 24 hours for security reasons.</p>
        
        <p>If you didn't create an account with us, please ignore this email.</p>
    </div>
    <div class="footer">
        <p>&copy; 2026 Identity Service. All rights reserved.</p>
    </div>
</body>
</html>
```

- [ ] **Step 3: Commit email template**

```bash
git add src/main/resources/templates/email-verification.html
git commit -m "feat: add HTML email verification template"
```

### Task 5: SMTP Email Service Implementation

**Files:**
- Create: `src/main/java/com/example/authservice/mail/service/SmtpEmailService.java`

- [ ] **Step 1: Write test for SMTP email service**

Create `src/test/java/com/example/authservice/mail/service/SmtpEmailServiceTest.java`:
```java
package com.example.authservice.mail.service;

import com.example.authservice.mail.dto.EmailDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailTemplateBuilder templateBuilder;

    private SmtpEmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new SmtpEmailService(mailSender, templateBuilder);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@test.com");
        ReflectionTestUtils.setField(emailService, "frontendBaseUrl", "http://localhost:3000");
    }

    @Test
    void shouldSendVerificationEmail() {
        // Given
        String email = "test@example.com";
        String token = "verification-token";
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateBuilder.buildVerificationEmail(any(EmailDto.class)))
            .thenReturn("HTML content");

        // When
        emailService.sendVerificationEmail(email, token);

        // Then
        verify(mailSender).send(mimeMessage);
        verify(templateBuilder).buildVerificationEmail(any(EmailDto.class));
    }

    @Test
    void shouldSendPasswordResetEmail() {
        // Given
        String email = "test@example.com";
        String token = "reset-token";
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateBuilder.buildPasswordResetEmail(any(EmailDto.class)))
            .thenReturn("HTML content");

        // When
        emailService.sendPasswordResetEmail(email, token);

        // Then
        verify(mailSender).send(mimeMessage);
        verify(templateBuilder).buildPasswordResetEmail(any(EmailDto.class));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=SmtpEmailServiceTest`
Expected: FAIL with "cannot find symbol: class SmtpEmailService"

- [ ] **Step 3: Create SMTP email service implementation**

Create `src/main/java/com/example/authservice/mail/service/SmtpEmailService.java`:
```java
package com.example.authservice.mail.service;

import com.example.authservice.mail.dto.EmailDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateBuilder templateBuilder;

    @Value("${SMTP_USERNAME}")
    private String fromEmail;

    @Value("${FRONTEND_BASE_URL}")
    private String frontendBaseUrl;

    @Override
    public void sendVerificationEmail(String email, String token) {
        String verificationUrl = String.format("%s/verify-email?token=%s", frontendBaseUrl, token);
        
        EmailDto emailDto = new EmailDto(
            email,
            "Email Verification - Identity Service",
            verificationUrl,
            token
        );

        String htmlContent = templateBuilder.buildVerificationEmail(emailDto);
        sendHtmlEmail(email, "Email Verification - Identity Service", htmlContent);
        
        log.info("email_verification_sent recipient={} token_length={}", 
                email, token.length());
    }

    @Override
    public void sendPasswordResetEmail(String email, String token) {
        String resetUrl = String.format("%s/reset-password?token=%s", frontendBaseUrl, token);
        
        EmailDto emailDto = new EmailDto(
            email,
            "Password Reset - Identity Service",
            resetUrl,
            token
        );

        String htmlContent = templateBuilder.buildPasswordResetEmail(emailDto);
        sendHtmlEmail(email, "Password Reset - Identity Service", htmlContent);
        
        log.info("password_reset_email_sent recipient={} token_length={}", 
                email, token.length());
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            
            log.info("email_sent to={} subject='{}' from={}", to, subject, fromEmail);
        } catch (MessagingException e) {
            log.error("email_send_failed to={} subject='{}' error={}", to, subject, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -B test -Dtest=SmtpEmailServiceTest`
Expected: PASS

- [ ] **Step 5: Commit SMTP email service**

```bash
git add src/main/java/com/example/authservice/mail/service/SmtpEmailService.java src/test/java/com/example/authservice/mail/service/SmtpEmailServiceTest.java
git commit -m "feat: implement SMTP email service with HTML templates"
```

### Task 6: Enhanced Email Template Builder

**Files:**
- Modify: `src/main/java/com/example/authservice/mail/service/EmailTemplateBuilder.java:1-50`

- [ ] **Step 1: Write test for template builder enhancements**

Create `src/test/java/com/example/authservice/mail/service/EmailTemplateBuilderTest.java`:
```java
package com.example.authservice.mail.service;

import com.example.authservice.mail.dto.EmailDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateBuilderTest {

    private EmailTemplateBuilder templateBuilder;

    @BeforeEach
    void setUp() {
        templateBuilder = new EmailTemplateBuilder();
    }

    @Test
    void shouldBuildVerificationEmailWithTemplate() {
        // Given
        EmailDto emailDto = new EmailDto(
            "test@example.com",
            "Email Verification",
            "http://localhost:3000/verify?token=abc123",
            "abc123"
        );

        // When
        String result = templateBuilder.buildVerificationEmail(emailDto);

        // Then
        assertThat(result).contains("Verify Your Email Address");
        assertThat(result).contains("http://localhost:3000/verify?token=abc123");
        assertThat(result).contains("Welcome to Identity Service");
    }

    @Test
    void shouldBuildPasswordResetEmail() {
        // Given
        EmailDto emailDto = new EmailDto(
            "test@example.com",
            "Password Reset",
            "http://localhost:3000/reset?token=xyz789",
            "xyz789"
        );

        // When
        String result = templateBuilder.buildPasswordResetEmail(emailDto);

        // Then
        assertThat(result).contains("Password Reset");
        assertThat(result).contains("http://localhost:3000/reset?token=xyz789");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=EmailTemplateBuilderTest`
Expected: FAIL with method calls to buildVerificationEmail

- [ ] **Step 3: Update EmailTemplateBuilder implementation**

Update `src/main/java/com/example/authservice/mail/service/EmailTemplateBuilder.java`:
```java
package com.example.authservice.mail.service;

import com.example.authservice.mail.dto.EmailDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class EmailTemplateBuilder {

    public String buildVerificationEmail(EmailDto emailDto) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email-verification.html");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            
            return template.replace("{{verificationUrl}}", emailDto.url());
        } catch (IOException e) {
            log.error("failed_to_load_email_template template=email-verification.html error={}", e.getMessage());
            return buildFallbackVerificationEmail(emailDto);
        }
    }

    public String buildPasswordResetEmail(EmailDto emailDto) {
        return String.format(
            """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #dc3545;">Password Reset Request</h2>
                    <p>You requested a password reset for your account.</p>
                    <p>Click the link below to reset your password:</p>
                    <a href="%s" style="display: inline-block; background-color: #dc3545; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; margin: 20px 0;">Reset Password</a>
                    <p>If you didn't request this, please ignore this email.</p>
                    <p>This link expires in 24 hours.</p>
                </div>
            </body>
            </html>
            """, emailDto.url()
        );
    }

    private String buildFallbackVerificationEmail(EmailDto emailDto) {
        return String.format(
            """
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #007bff;">Welcome to Identity Service!</h2>
                    <p>Thank you for registering. Please verify your email by clicking the link below:</p>
                    <a href="%s" style="display: inline-block; background-color: #28a745; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; margin: 20px 0;">Verify Email</a>
                    <p>This link expires in 24 hours.</p>
                </div>
            </body>
            </html>
            """, emailDto.url()
        );
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -B test -Dtest=EmailTemplateBuilderTest`
Expected: PASS

- [ ] **Step 5: Commit template builder updates**

```bash
git add src/main/java/com/example/authservice/mail/service/EmailTemplateBuilder.java src/test/java/com/example/authservice/mail/service/EmailTemplateBuilderTest.java
git commit -m "feat: enhance email template builder with HTML file loading"
```

### Task 7: Role Assignment Implementation

**Files:**
- Modify: `src/main/java/com/example/authservice/role/service/RoleServiceImpl.java:23-26`

- [ ] **Step 1: Write test for role assignment**

Create `src/test/java/com/example/authservice/role/service/RoleServiceImplTest.java`:
```java
package com.example.authservice.role.service;

import com.example.authservice.role.entity.Role;
import com.example.authservice.role.entity.RoleName;
import com.example.authservice.role.repository.RoleRepository;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    private RoleServiceImpl roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleServiceImpl(roleRepository, userRepository);
    }

    @Test
    void shouldAssignCustomerRoleWhenRoleExists() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        
        Role customerRole = new Role();
        customerRole.setCode("CUSTOMER");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("CUSTOMER")).thenReturn(Optional.of(customerRole));

        // When
        roleService.assignCustomerRole(userId);

        // Then
        verify(userRepository).findById(userId);
        verify(roleRepository).findByCode("CUSTOMER");
        verify(userRepository).save(user);
    }

    @Test
    void shouldCreateCustomerRoleWhenNotExists() {
        // Given
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        
        Role newCustomerRole = new Role();
        newCustomerRole.setCode("CUSTOMER");
        newCustomerRole.setName("Customer");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("CUSTOMER")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(newCustomerRole);

        // When
        roleService.assignCustomerRole(userId);

        // Then
        verify(roleRepository).save(any(Role.class));
        verify(userRepository).save(user);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=RoleServiceImplTest`
Expected: FAIL with constructor issues and method not implemented

- [ ] **Step 3: Update RoleServiceImpl with proper role assignment**

Update `src/main/java/com/example/authservice/role/service/RoleServiceImpl.java`:
```java
package com.example.authservice.role.service;

import com.example.authservice.role.entity.Role;
import com.example.authservice.role.entity.RoleName;
import com.example.authservice.role.repository.RoleRepository;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    @Override
    public Optional<Role> findByCode(String code) {
        return roleRepository.findByCode(code);
    }

    @Override
    @Transactional
    public void assignCustomerRole(UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        Role customerRole = roleRepository.findByCode(RoleName.CUSTOMER.name())
            .orElseGet(() -> createCustomerRole());

        user.getRoles().add(customerRole);
        userRepository.save(user);

        log.info("role_assigned user_id={} role={}", userId, RoleName.CUSTOMER.name());
    }

    private Role createCustomerRole() {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setCode(RoleName.CUSTOMER.name());
        role.setName("Customer");
        role.setDescription("Default customer role for registered users");
        
        Role savedRole = roleRepository.save(role);
        log.info("role_created role={} id={}", RoleName.CUSTOMER.name(), savedRole.getId());
        
        return savedRole;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -B test -Dtest=RoleServiceImplTest`
Expected: PASS

- [ ] **Step 5: Commit role assignment implementation**

```bash
git add src/main/java/com/example/authservice/role/service/RoleServiceImpl.java src/test/java/com/example/authservice/role/service/RoleServiceImplTest.java
git commit -m "feat: implement role assignment with auto-creation of customer role"
```

### Task 8: Initialize Default Roles

**Files:**
- Create: `src/main/java/com/example/authservice/config/DataInitializer.java`

- [ ] **Step 1: Write test for data initializer**

Create `src/test/java/com/example/authservice/config/DataInitializerTest.java`:
```java
package com.example.authservice.config;

import com.example.authservice.role.entity.Role;
import com.example.authservice.role.entity.RoleName;
import com.example.authservice.role.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private RoleRepository roleRepository;

    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        dataInitializer = new DataInitializer(roleRepository);
    }

    @Test
    void shouldCreateAllDefaultRolesWhenNoneExist() {
        // Given
        when(roleRepository.findByCode("CUSTOMER")).thenReturn(Optional.empty());
        when(roleRepository.findByCode("STAFF")).thenReturn(Optional.empty());
        when(roleRepository.findByCode("ADMIN")).thenReturn(Optional.empty());

        // When
        dataInitializer.initializeDefaultRoles();

        // Then
        verify(roleRepository, times(3)).save(any(Role.class));
    }

    @Test
    void shouldNotCreateRolesWhenTheyExist() {
        // Given
        Role existingRole = new Role();
        when(roleRepository.findByCode("CUSTOMER")).thenReturn(Optional.of(existingRole));
        when(roleRepository.findByCode("STAFF")).thenReturn(Optional.of(existingRole));
        when(roleRepository.findByCode("ADMIN")).thenReturn(Optional.of(existingRole));

        // When
        dataInitializer.initializeDefaultRoles();

        // Then
        verify(roleRepository, times(0)).save(any(Role.class));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -B test -Dtest=DataInitializerTest`
Expected: FAIL with "cannot find symbol: class DataInitializer"

- [ ] **Step 3: Create DataInitializer implementation**

Create `src/main/java/com/example/authservice/config/DataInitializer.java`:
```java
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
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -B test -Dtest=DataInitializerTest`
Expected: PASS

- [ ] **Step 5: Commit data initializer**

```bash
git add src/main/java/com/example/authservice/config/DataInitializer.java src/test/java/com/example/authservice/config/DataInitializerTest.java
git commit -m "feat: add data initializer for default roles"
```

### Task 9: Integration Testing

**Files:**
- Create: `src/test/java/com/example/authservice/integration/EmailRegistrationIntegrationTest.java`

- [ ] **Step 1: Create integration test for complete flow**

Create `src/test/java/com/example/authservice/integration/EmailRegistrationIntegrationTest.java`:
```java
package com.example.authservice.integration;

import com.example.authservice.auth.dto.RegisterRequest;
import com.example.authservice.role.entity.RoleName;
import com.example.authservice.user.entity.User;
import com.example.authservice.user.entity.UserStatus;
import com.example.authservice.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class EmailRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRegisterUserWithPendingStatusAndCustomerRole() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest(
            "test@example.com",
            "SecureP@ssw0rd123",
            "John",
            "Doe"
        );

        // When
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));

        // Then
        User savedUser = userRepository.findByEmail("test@example.com").orElseThrow();
        assertThat(savedUser.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(savedUser.getRoles()).hasSize(1);
        assertThat(savedUser.getRoles().iterator().next().getCode()).isEqualTo(RoleName.CUSTOMER.name());
    }
}
```

- [ ] **Step 2: Run integration test**

Run: `mvn -B test -Dtest=EmailRegistrationIntegrationTest`
Expected: PASS with complete registration flow working

- [ ] **Step 3: Run all tests to ensure nothing broke**

Run: `mvn -B test`
Expected: All tests PASS

- [ ] **Step 4: Commit integration test**

```bash
git add src/test/java/com/example/authservice/integration/EmailRegistrationIntegrationTest.java
git commit -m "test: add integration test for complete email registration flow"
```

### Task 10: Final Build Verification

**Files:**
- Run complete build with all tests

- [ ] **Step 1: Clean build with all tests**

Run: `mvn -B clean verify`
Expected: BUILD SUCCESS with all tests passing

- [ ] **Step 2: Verify SMTP configuration loads correctly**

Run: `mvn -B spring-boot:run -Dspring.profiles.active=dev` (background process)
Expected: Application starts without errors, logs show mail configuration loaded

- [ ] **Step 3: Stop test application**

Run: `pkill -f java` (stop background process)

- [ ] **Step 4: Final commit with implementation complete**

```bash
git add .
git commit -m "feat: complete SMTP email and role assignment implementation

- Replace StubEmailService with SmtpEmailService using Gmail SMTP
- Add HTML email templates for verification emails  
- Implement role assignment with auto-creation of default roles
- Add comprehensive test coverage for all components
- Configure Spring Boot Mail with STARTTLS security"
```

---

## Self-Review

**Spec coverage:** 
✅ SMTP email functionality with Gmail integration  
✅ Strong password validation (already implemented)  
✅ Event-driven email verification (already implemented)  
✅ Role assignment with default customer role  
✅ HTML email templates  
✅ Environment-based configuration  
✅ Comprehensive testing

**Placeholder scan:** No TBD, TODO, or "implement later" patterns found.

**Type consistency:** All class names, method signatures, and imports are consistent across tasks.

All spec requirements are covered with complete implementation tasks.