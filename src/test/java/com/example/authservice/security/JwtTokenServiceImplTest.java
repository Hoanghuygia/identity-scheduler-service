package com.example.authservice.security;

import com.example.authservice.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenServiceImplTest {

    @Test
    void shouldReturnExpiredErrorWhenTokenIsExpired() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        AppProperties appProperties = buildAppProperties(keyPair);
        appProperties.getJwt().setAccessTokenExpirySeconds(-1L);
        JwtTokenServiceImpl jwtTokenService = new JwtTokenServiceImpl(appProperties);

        String token = jwtTokenService.generateAccessToken(UUID.randomUUID(), "active@example.com", Set.of("USER"));

        JwtTokenValidationResult result = jwtTokenService.validateAccessToken(token);

        assertFalse(result.valid());
        assertEquals(JwtTokenValidationError.TOKEN_EXPIRED, result.error());
    }

    @Test
    void shouldReturnMalformedErrorWhenTokenCannotBeParsed() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        AppProperties appProperties = buildAppProperties(keyPair);
        JwtTokenServiceImpl jwtTokenService = new JwtTokenServiceImpl(appProperties);

        JwtTokenValidationResult result = jwtTokenService.validateAccessToken("this-is-not-a-jwt");

        assertFalse(result.valid());
        assertEquals(JwtTokenValidationError.TOKEN_MALFORMED, result.error());
    }

    @Test
    void shouldReturnInvalidSignatureErrorWhenSignatureDoesNotMatch() throws Exception {
        KeyPair issuerKeyPair = generateRsaKeyPair();
        KeyPair verifierKeyPair = generateRsaKeyPair();

        JwtTokenServiceImpl issuerService = new JwtTokenServiceImpl(buildAppProperties(issuerKeyPair));
        JwtTokenServiceImpl verifierService = new JwtTokenServiceImpl(buildAppProperties(verifierKeyPair));

        String token = issuerService.generateAccessToken(UUID.randomUUID(), "active@example.com", Set.of("USER"));

        JwtTokenValidationResult result = verifierService.validateAccessToken(token);

        assertFalse(result.valid());
        assertEquals(JwtTokenValidationError.TOKEN_INVALID_SIGNATURE, result.error());
    }

    @Test
    void shouldGenerateAndValidateAccessTokenAndExtractClaims() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        AppProperties appProperties = buildAppProperties(keyPair);
        JwtTokenServiceImpl jwtTokenService = new JwtTokenServiceImpl(appProperties);

        UUID userId = UUID.randomUUID();
        Set<String> roles = Set.of("CUSTOMER", "USER");

        String token = jwtTokenService.generateAccessToken(userId, "active@example.com", roles);

        assertNotNull(token);
        assertTrue(jwtTokenService.isTokenValid(token));
        assertEquals(userId, jwtTokenService.extractUserId(token));
        assertEquals(roles, jwtTokenService.extractRoles(token));
    }

    @Test
    void shouldReturnFalseWhenIssuerDoesNotMatch() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        AppProperties appProperties = buildAppProperties(keyPair);
        JwtTokenServiceImpl jwtTokenService = new JwtTokenServiceImpl(appProperties);

        String token = jwtTokenService.generateAccessToken(UUID.randomUUID(), "active@example.com", Set.of("USER"));

        appProperties.getJwt().setIssuer("another-issuer");

        assertFalse(jwtTokenService.isTokenValid(token));
    }

    @Test
    void shouldSupportUrlSafeBase64EncodedKeysWithoutPemHeaders() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        AppProperties appProperties = new AppProperties();
        AppProperties.Jwt jwt = new AppProperties.Jwt();
        jwt.setIssuer("test-auth-service");
        jwt.setAccessTokenExpirySeconds(900L);
        jwt.setPrivateKey(toUrlSafeBase64((RSAPrivateKey) keyPair.getPrivate()));
        jwt.setPublicKey(toUrlSafeBase64((RSAPublicKey) keyPair.getPublic()));
        appProperties.setJwt(jwt);

        JwtTokenServiceImpl jwtTokenService = new JwtTokenServiceImpl(appProperties);
        String token = jwtTokenService.generateAccessToken(UUID.randomUUID(), "active@example.com", Set.of("USER"));

        assertTrue(jwtTokenService.isTokenValid(token));
    }

    private AppProperties buildAppProperties(KeyPair keyPair) {
        AppProperties appProperties = new AppProperties();
        AppProperties.Jwt jwt = new AppProperties.Jwt();
        jwt.setIssuer("test-auth-service");
        jwt.setAccessTokenExpirySeconds(900L);
        jwt.setPrivateKey(toPemPrivateKey((RSAPrivateKey) keyPair.getPrivate()));
        jwt.setPublicKey(toPemPublicKey((RSAPublicKey) keyPair.getPublic()));
        appProperties.setJwt(jwt);
        return appProperties;
    }

    private KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    private String toPemPrivateKey(RSAPrivateKey privateKey) {
        String encoded = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----";
    }

    private String toPemPublicKey(RSAPublicKey publicKey) {
        String encoded = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----";
    }

    private String toUrlSafeBase64(java.security.Key key) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(key.getEncoded());
    }
}
