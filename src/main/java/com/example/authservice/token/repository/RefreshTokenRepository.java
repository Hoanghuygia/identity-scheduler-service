package com.example.authservice.token.repository;

import com.example.authservice.token.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    @Query("""
        select rt from RefreshToken rt
        where rt.token = :token and rt.revoked = false and rt.expiresAt > :now
    """)
    Optional<RefreshToken> findActiveByToken(@Param("token") String token, @Param("now") Instant now);

    Optional<RefreshToken> findByToken(String token);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select rt from RefreshToken rt
        where rt.token = :token
    """)
    Optional<RefreshToken> findByTokenForUpdate(@Param("token") String token);

    @Query("""
        select rt from RefreshToken rt
        where rt.user.id = :userId and rt.revoked = false and rt.expiresAt > :now
    """)
    List<RefreshToken> findAllActiveByUserId(@Param("userId") UUID userId, @Param("now") Instant now);
}
