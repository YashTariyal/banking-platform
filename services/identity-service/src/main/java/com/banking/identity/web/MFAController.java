package com.banking.identity.web;

import com.banking.identity.domain.MFASettings;
import com.banking.identity.service.MFAService;
import com.banking.identity.web.dto.EnableSMSRequest;
import com.banking.identity.web.dto.EnableTOTPRequest;
import com.banking.identity.web.dto.MFASettingsResponse;
import com.banking.identity.web.dto.VerifyMFARequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mfa")
@Tag(name = "Multi-Factor Authentication", description = "MFA settings and verification")
public class MFAController {

    private final MFAService mfaService;

    public MFAController(MFAService mfaService) {
        this.mfaService = mfaService;
    }

    @GetMapping("/{userId}")
    @Operation(
            summary = "Get MFA settings",
            description = "Retrieves MFA settings for a user"
    )
    public ResponseEntity<MFASettingsResponse> getMFASettings(
            @Parameter(description = "User unique identifier", required = true)
            @PathVariable UUID userId
    ) {
        MFASettings settings = mfaService.getMFASettings(userId);
        return ResponseEntity.ok(toResponse(settings));
    }

    @PostMapping("/{userId}/totp")
    @Operation(
            summary = "Enable TOTP",
            description = "Enables TOTP-based MFA for a user"
    )
    public ResponseEntity<MFASettingsResponse> enableTOTP(
            @PathVariable UUID userId,
            @Valid @RequestBody EnableTOTPRequest request
    ) {
        MFASettings settings = mfaService.enableTOTP(userId, request.totpSecret());
        return ResponseEntity.ok(toResponse(settings));
    }

    @PostMapping("/{userId}/sms")
    @Operation(
            summary = "Enable SMS MFA",
            description = "Enables SMS-based MFA for a user"
    )
    public ResponseEntity<MFASettingsResponse> enableSMS(
            @PathVariable UUID userId,
            @Valid @RequestBody EnableSMSRequest request
    ) {
        MFASettings settings = mfaService.enableSMS(userId, request.phoneNumber());
        return ResponseEntity.ok(toResponse(settings));
    }

    @PutMapping("/{userId}/verify-phone")
    @Operation(
            summary = "Verify phone number",
            description = "Verifies the phone number for SMS MFA"
    )
    public ResponseEntity<MFASettingsResponse> verifyPhone(
            @PathVariable UUID userId,
            @Valid @RequestBody VerifyMFARequest request
    ) {
        mfaService.verifyPhone(userId, request.code());
        MFASettings settings = mfaService.getMFASettings(userId);
        return ResponseEntity.ok(toResponse(settings));
    }

    @DeleteMapping("/{userId}")
    @Operation(
            summary = "Disable MFA",
            description = "Disables MFA for a user"
    )
    public ResponseEntity<Void> disableMFA(@PathVariable UUID userId) {
        mfaService.disableMFA(userId);
        return ResponseEntity.noContent().build();
    }

    private MFASettingsResponse toResponse(MFASettings settings) {
        return new MFASettingsResponse(
                settings.getId(),
                settings.getUserId(),
                settings.getMfaEnabled(),
                settings.getMfaMethod(),
                settings.getPhoneVerified(),
                settings.getCreatedAt(),
                settings.getUpdatedAt()
        );
    }
}

