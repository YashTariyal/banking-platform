package com.banking.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.identity.domain.MFAMethod;
import com.banking.identity.domain.MFASettings;
import com.banking.identity.domain.User;
import com.banking.identity.repository.MFASettingsRepository;
import com.banking.identity.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MFAServiceTest {

    @Mock
    private MFASettingsRepository mfaSettingsRepository;

    @Mock
    private UserRepository userRepository;

    private MFAService mfaService;

    @BeforeEach
    void setUp() {
        mfaService = new MFAService(mfaSettingsRepository, userRepository);
    }

    @Test
    void enableTOTP_CreatesOrUpdatesSettings() {
        // Given
        UUID userId = UUID.randomUUID();
        String totpSecret = "secret123";
        User user = createUser();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mfaSettingsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(mfaSettingsRepository.save(any(MFASettings.class))).thenAnswer(invocation -> {
            MFASettings settings = invocation.getArgument(0);
            settings.setId(UUID.randomUUID());
            return settings;
        });

        // When
        MFASettings settings = mfaService.enableTOTP(userId, totpSecret);

        // Then
        assertThat(settings).isNotNull();
        assertThat(settings.getMfaEnabled()).isTrue();
        assertThat(settings.getMfaMethod()).isEqualTo(MFAMethod.TOTP);
        assertThat(settings.getTotpSecret()).isEqualTo(totpSecret);
        assertThat(settings.getTotpBackupCodes()).isNotNull();
        verify(mfaSettingsRepository).save(any(MFASettings.class));
    }

    @Test
    void enableSMS_CreatesOrUpdatesSettings() {
        // Given
        UUID userId = UUID.randomUUID();
        String phoneNumber = "+1234567890";
        User user = createUser();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(mfaSettingsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(mfaSettingsRepository.save(any(MFASettings.class))).thenAnswer(invocation -> {
            MFASettings settings = invocation.getArgument(0);
            settings.setId(UUID.randomUUID());
            return settings;
        });

        // When
        MFASettings settings = mfaService.enableSMS(userId, phoneNumber);

        // Then
        assertThat(settings).isNotNull();
        assertThat(settings.getMfaEnabled()).isTrue();
        assertThat(settings.getMfaMethod()).isEqualTo(MFAMethod.SMS);
        assertThat(settings.getPhoneNumber()).isEqualTo(phoneNumber);
        assertThat(settings.getPhoneVerified()).isFalse();
        verify(mfaSettingsRepository).save(any(MFASettings.class));
    }

    @Test
    void verifyPhone_VerifiesPhoneNumber() {
        // Given
        UUID userId = UUID.randomUUID();
        MFASettings settings = createMFASettings();
        settings.setMfaMethod(MFAMethod.SMS);
        settings.setPhoneVerified(false);

        when(mfaSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(settings));
        when(mfaSettingsRepository.save(any(MFASettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        mfaService.verifyPhone(userId, "123456");

        // Then
        assertThat(settings.getPhoneVerified()).isTrue();
        verify(mfaSettingsRepository).save(settings);
    }

    @Test
    void disableMFA_DisablesMFA() {
        // Given
        UUID userId = UUID.randomUUID();
        MFASettings settings = createMFASettings();
        settings.setMfaEnabled(true);
        settings.setMfaMethod(MFAMethod.TOTP);

        when(mfaSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(settings));
        when(mfaSettingsRepository.save(any(MFASettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        mfaService.disableMFA(userId);

        // Then
        assertThat(settings.getMfaEnabled()).isFalse();
        assertThat(settings.getMfaMethod()).isNull();
        verify(mfaSettingsRepository).save(settings);
    }

    @Test
    void getMFASettings_CreatesDefaultIfNotExists() {
        // Given
        UUID userId = UUID.randomUUID();

        when(mfaSettingsRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(mfaSettingsRepository.save(any(MFASettings.class))).thenAnswer(invocation -> {
            MFASettings settings = invocation.getArgument(0);
            settings.setId(UUID.randomUUID());
            return settings;
        });

        // When
        MFASettings settings = mfaService.getMFASettings(userId);

        // Then
        assertThat(settings).isNotNull();
        assertThat(settings.getMfaEnabled()).isFalse();
        verify(mfaSettingsRepository).save(any(MFASettings.class));
    }

    @Test
    void verifyTOTPCode_WithBackupCode_ReturnsTrue() {
        // Given
        UUID userId = UUID.randomUUID();
        MFASettings settings = createMFASettings();
        settings.setMfaEnabled(true);
        settings.setMfaMethod(MFAMethod.TOTP);
        settings.setTotpBackupCodes("12345678,87654321");

        when(mfaSettingsRepository.findByUserId(userId)).thenReturn(Optional.of(settings));

        // When
        boolean result = mfaService.verifyTOTPCode(userId, "12345678");

        // Then
        assertThat(result).isTrue();
    }

    private User createUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testuser");
        return user;
    }

    private MFASettings createMFASettings() {
        MFASettings settings = new MFASettings();
        settings.setId(UUID.randomUUID());
        settings.setUserId(UUID.randomUUID());
        settings.setMfaEnabled(false);
        return settings;
    }
}

