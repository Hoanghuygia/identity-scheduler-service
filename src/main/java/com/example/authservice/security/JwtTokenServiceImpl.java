package com.example.authservice.security;

import com.example.authservice.config.AppProperties;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class JwtTokenServiceImpl implements JwtTokenService {

    private final AppProperties appProperties;
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    public JwtTokenServiceImpl(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.privateKey = parsePrivateKey(appProperties.getJwt().getPrivateKey());
        this.publicKey = parsePublicKey(appProperties.getJwt().getPublicKey());
    }

    @Override
    public String generateAccessToken(UUID userId, String email, Set<String> roles) {
        try {
            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(appProperties.getJwt().getAccessTokenExpirySeconds());

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .issuer(appProperties.getJwt().getIssuer())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiresAt))
                .claim("email", email)
                .claim("roles", roles)
                .build();

            SignedJWT signedJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
                claimsSet
            );
            signedJwt.sign(new RSASSASigner(privateKey));
            return signedJwt.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Failed to generate access token", ex);
        }
    }

    @Override
    public String generateRefreshToken(UUID userId) {
        // TODO: Implement JWT/opaque refresh token generation strategy.
        return "stub-refresh-token-for-" + userId;
    }

    @Override
    public boolean isTokenValid(String token) {
        return validateAccessToken(token).valid();
    }

    @Override
    public JwtTokenValidationResult validateAccessToken(String token) {
        if (!StringUtils.hasText(token)) {
            return JwtTokenValidationResult.failure(JwtTokenValidationError.TOKEN_MISSING);
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            boolean signatureValid = signedJWT.verify(new RSASSAVerifier(publicKey));
            if (!signatureValid) {
                log.info("Access token is invalid");
                return JwtTokenValidationResult.failure(JwtTokenValidationError.TOKEN_INVALID_SIGNATURE);
            }

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            Date expirationTime = claimsSet.getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                log.info("Access token has expired. Expiration time: {}", expirationTime);
                return JwtTokenValidationResult.failure(JwtTokenValidationError.TOKEN_EXPIRED);
            }

            String issuer = claimsSet.getIssuer();
            if (!appProperties.getJwt().getIssuer().equals(issuer)) {
                return JwtTokenValidationResult.failure(JwtTokenValidationError.TOKEN_INVALID_ISSUER);
            }

            return JwtTokenValidationResult.success();
        } catch (ParseException ex) {
            log.info("JWT token is malformed", ex);
            return JwtTokenValidationResult.failure(JwtTokenValidationError.TOKEN_MALFORMED);
        } catch (JOSEException ex) {
            log.error("Error occurred while validating JWT token", ex);
            return JwtTokenValidationResult.failure(JwtTokenValidationError.TOKEN_INVALID);
        }
    }

    @Override
    public UUID extractUserId(String token) {
        if (!isTokenValid(token)) {
            return null;
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String subject = signedJWT.getJWTClaimsSet().getSubject();
            return UUID.fromString(subject);
        } catch (ParseException | IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public Set<String> extractRoles(String token) {
        if (!isTokenValid(token)) {
            return Collections.emptySet();
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            List<String> roles = claimsSet.getStringListClaim("roles");
            if (roles == null) {
                return Collections.emptySet();
            }

            return new HashSet<>(roles);
        } catch (ParseException ex) {
            return Collections.emptySet();
        }
    }

    private RSAPrivateKey parsePrivateKey(String pemPrivateKey) {
        try {
            byte[] keyBytes = decodePemKey(pemPrivateKey, "PRIVATE KEY");
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey key = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            return (RSAPrivateKey) key;
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid RSA private key configuration", ex);
        }
    }

    private RSAPublicKey parsePublicKey(String pemPublicKey) {
        try {
            byte[] keyBytes = decodePemKey(pemPublicKey, "PUBLIC KEY");
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey key = keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
            return (RSAPublicKey) key;
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid RSA public key configuration", ex);
        }
    }

    private byte[] decodePemKey(String key, String keyType) {
        String keyContent = resolveKeyContent(key, keyType);

        String normalized = keyContent.replace("\\n", "\n")
            .replace("-----BEGIN " + keyType + "-----", "")
            .replace("-----END " + keyType + "-----", "")
            .replaceAll("\\s", "");

        try {
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException ex) {
            String urlSafeNormalized = normalized.replace('-', '+').replace('_', '/');
            return Base64.getDecoder().decode(withBase64Padding(urlSafeNormalized));
        }
    }

    private String withBase64Padding(String value) {
        int remainder = value.length() % 4;
        if (remainder == 0) {
            return value;
        }

        return value + "=".repeat(4 - remainder);
    }

    private String resolveKeyContent(String key, String keyType) {
        if (!StringUtils.hasText(key)) {
            throw new IllegalStateException("JWT " + keyType + " is not configured");
        }

        String trimmed = key.trim();
        if (trimmed.startsWith("-----BEGIN ")) {
            return trimmed;
        }

        if (trimmed.startsWith("classpath:")) {
            return readClasspathResource(trimmed.substring("classpath:".length()), keyType);
        }

        try {
            Path path = Path.of(trimmed);
            if (Files.exists(path)) {
                return Files.readString(path, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            // Fall through to classpath and inline value handling.
        }

        if (trimmed.endsWith(".pem")) {
            return readClasspathResource(trimmed, keyType);
        }

        return trimmed;
    }

    private String readClasspathResource(String location, String keyType) {
        String normalizedLocation = location.startsWith("/") ? location.substring(1) : location;
        ClassLoader classLoader = JwtTokenServiceImpl.class.getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(normalizedLocation)) {
            if (inputStream == null) {
                throw new IllegalStateException("JWT " + keyType + " resource not found: " + location);
            }

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read JWT " + keyType + " resource: " + location, ex);
        }
    }
}
