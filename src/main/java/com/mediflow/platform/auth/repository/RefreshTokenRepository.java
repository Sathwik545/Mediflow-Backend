package com.mediflow.platform.auth.repository;

import com.mediflow.platform.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    /** Revoke all active refresh tokens for a user — called on logout. */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.userId = :userId AND rt.revoked = false")
    void revokeAllByUserId(@Param("userId") Long userId);

    /** Delete expired or revoked tokens for a user — called before issuing a new refresh token. */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.userId = :userId AND (rt.revoked = true OR rt.expiresAt < CURRENT_TIMESTAMP)")
    void deleteExpiredOrRevokedByUserId(@Param("userId") Long userId);
}
