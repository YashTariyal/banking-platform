package com.banking.identity.service;

import com.banking.identity.domain.MFAMethod;
import com.banking.identity.domain.MFASettings;
import com.banking.identity.repository.MFASettingsRepository;
import com.banking.identity.repository.UserRepository;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MFAService {

    private static final int BACKUP_CODE_COUNT = 10;
    private static final int BACKUP_CODE_LENGTH = 8;

    private final MFASettingsRepository mfaSettingsRepository;
    private final UserRepository userRepository;

    public MFAService(MFASettingsRepository mfaSettingsRepository, UserRepository userRepository) {
        this.mfaSettingsRepository = mfaSettingsRepository;
        this.userRepository = userRepository;
    }

    public MFASettings enableTOTP(UUID userId, String totpSecret) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        MFASettings settings = mfaSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    MFASettings newSettings = new MFASettings();
                    newSettings.setUserId(userId);
                    return newSettings;
                });

        settings.setMfaEnabled(true);
        settings.setMfaMethod(MFAMethod.TOTP);
        settings.setTotpSecret(totpSecret);
        settings.setTotpBackupCodes(generateBackupCodes());

        return mfaSettingsRepository.save(settings);
    }

    public MFASettings enableSMS(UUID userId, String phoneNumber) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        MFASettings settings = mfaSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    MFASettings newSettings = new MFASettings();
                    newSettings.setUserId(userId);
                    return newSettings;
                });

        settings.setMfaEnabled(true);
        settings.setMfaMethod(MFAMethod.SMS);
        settings.setPhoneNumber(phoneNumber);
        settings.setPhoneVerified(false);

        return mfaSettingsRepository.save(settings);
    }

    public void verifyPhone(UUID userId, String code) {
        // In real implementation, verify the code sent via SMS
        // For now, just mark as verified
        MFASettings settings = mfaSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("MFA settings not found"));

        if (settings.getMfaMethod() != MFAMethod.SMS) {
            throw new IllegalStateException("MFA method is not SMS");
        }

        settings.setPhoneVerified(true);
        mfaSettingsRepository.save(settings);
    }

    public void disableMFA(UUID userId) {
        MFASettings settings = mfaSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("MFA settings not found"));

        settings.setMfaEnabled(false);
        settings.setMfaMethod(null);
        settings.setTotpSecret(null);
        settings.setTotpBackupCodes(null);
        mfaSettingsRepository.save(settings);
    }

    public MFASettings getMFASettings(UUID userId) {
        return mfaSettingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    MFASettings settings = new MFASettings();
                    settings.setUserId(userId);
                    settings.setMfaEnabled(false);
                    return mfaSettingsRepository.save(settings);
                });
    }

    public boolean verifyTOTPCode(UUID userId, String code) {
        MFASettings settings = mfaSettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("MFA settings not found"));

        if (!settings.getMfaEnabled() || settings.getMfaMethod() != MFAMethod.TOTP) {
            return false;
        }

        // In real implementation, use TOTP library (e.g., Google Authenticator)
        // For now, return true if code matches backup codes
        if (settings.getTotpBackupCodes() != null) {
            String[] backupCodes = settings.getTotpBackupCodes().split(",");
            for (String backupCode : backupCodes) {
                if (backupCode.trim().equals(code)) {
                    return true;
                }
            }
        }

        // TODO: Implement actual TOTP verification using a library like:
        // TOTP totp = new TOTP(settings.getTotpSecret());
        // return totp.verify(code, Instant.now());
        
        return false;
    }

    private String generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < BACKUP_CODE_LENGTH; j++) {
                code.append(random.nextInt(10));
            }
            codes.add(code.toString());
        }
        return String.join(",", codes);
    }
}

