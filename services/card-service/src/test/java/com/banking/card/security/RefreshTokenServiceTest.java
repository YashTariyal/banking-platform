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
        "spring.flyway.enabled=false"
})
class RefreshTokenServiceTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void issuePersistsAndValidates() {
        RefreshTokenService.RefreshTokenInfo info = refreshTokenService.issue("subject-1", "scope-a scope-b");

        assertThat(refreshTokenRepository.findById(info.token())).isPresent();
        assertThat(info.revoked()).isFalse();
        assertThat(refreshTokenService.validate(info.token())).isPresent();
    }

    @Test
    void expiredTokenIsRevokedOnValidation() {
        RefreshTokenService.RefreshTokenInfo info = refreshTokenService.issue("subject-2", "scope-a");
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
        RefreshTokenService.RefreshTokenInfo info = refreshTokenService.issue("subject-3", "scope-a");

        refreshTokenService.revoke(info.token());

        assertThat(refreshTokenService.validate(info.token())).isEmpty();
        assertThat(refreshTokenRepository.findById(info.token()))
                .get()
                .extracting(RefreshToken::isRevoked)
                .isEqualTo(true);
    }
}

