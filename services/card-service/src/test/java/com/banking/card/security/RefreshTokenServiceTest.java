package com.banking.card.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.card.domain.RefreshToken;
import com.banking.card.repository.RefreshTokenRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@Import(RefreshTokenService.class)
@TestPropertySource(properties = {
        "card.security.refresh-token.ttl-seconds=3600",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RefreshTokenServiceTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void issuePersistsAndValidates() {
        RefreshTokenService.RefreshTokenInfo info = refreshTokenService.issue("subject-1", "scope-a scope-b", "device-1", "JUnit");

        assertThat(refreshTokenRepository.findById(info.token())).isPresent();
        assertThat(info.revoked()).isFalse();
        assertThat(refreshTokenService.validate(info.token())).isPresent();
    }

    @Test
    void expiredTokenIsRevokedOnValidation() {
        RefreshTokenService.RefreshTokenInfo info = refreshTokenService.issue("subject-2", "scope-a", "device-2", "JUnit");
        RefreshToken token = refreshTokenRepository.findById(info.token()).orElseThrow();

        token.setExpiresAt(Instant.now().minusSeconds(10));
        refreshTokenRepository.save(token);

        assertThat(refreshTokenService.validate(info.token())).isEmpty();
        assertThat(refreshTokenRepository.findById(info.token()))
                .get()
                .extracting(RefreshToken::isRevoked)
                .isEqualTo(true);
    }

    @Test
    void revokedTokenIsRejected() {
        RefreshTokenService.RefreshTokenInfo info = refreshTokenService.issue("subject-3", "scope-a", "device-3", "JUnit");

        refreshTokenService.revoke(info.token());

        assertThat(refreshTokenService.validate(info.token())).isEmpty();
        assertThat(refreshTokenRepository.findById(info.token()))
                .get()
                .extracting(RefreshToken::isRevoked)
                .isEqualTo(true);
    }

    @Test
    void rotateReplacesTokenAndRevokesOld() {
        RefreshTokenService.RefreshTokenInfo issued = refreshTokenService.issue("subject-4", "scope-a", "device-4", "JUnit");

        RefreshTokenService.RefreshTokenInfo rotated = refreshTokenService
                .rotate(issued.token(), "device-4", "JUnit")
                .orElseThrow();

        assertThat(rotated.token()).isNotEqualTo(issued.token());
        assertThat(rotated.deviceId()).isEqualTo("device-4");
        assertThat(refreshTokenRepository.findById(issued.token()))
                .get()
                .extracting(RefreshToken::isRevoked)
                .isEqualTo(true);
    }

    @Test
    void deviceMismatchRevokesAllTokensForSubject() {
        RefreshTokenService.RefreshTokenInfo t1 = refreshTokenService.issue("subject-5", "scope-a", "device-5a", "JUnit");
        RefreshTokenService.RefreshTokenInfo t2 = refreshTokenService.issue("subject-5", "scope-a", "device-5a", "JUnit");

        // Validate with mismatching device -> should revoke all
        assertThat(refreshTokenService.validate(t1.token(), "device-5b", "JUnit")).isEmpty();

        assertThat(refreshTokenRepository.findById(t1.token())).get().extracting(RefreshToken::isRevoked).isEqualTo(true);
        assertThat(refreshTokenRepository.findById(t2.token())).get().extracting(RefreshToken::isRevoked).isEqualTo(true);
    }

    @Test
    void reuseOfRevokedTokenRevokesAllForSubject() {
        RefreshTokenService.RefreshTokenInfo t1 = refreshTokenService.issue("subject-6", "scope-a", "device-6", "JUnit");
        RefreshTokenService.RefreshTokenInfo t2 = refreshTokenService.issue("subject-6", "scope-a", "device-6", "JUnit");

        refreshTokenService.revoke(t1.token());

        // Reuse revoked token triggers revocation of all tokens for subject
        assertThat(refreshTokenService.validate(t1.token(), "device-6", "JUnit")).isEmpty();

        assertThat(refreshTokenRepository.findById(t1.token())).get().extracting(RefreshToken::isRevoked).isEqualTo(true);
        assertThat(refreshTokenRepository.findById(t2.token())).get().extracting(RefreshToken::isRevoked).isEqualTo(true);
    }
}

