package com.banking.identity.web;

import com.banking.identity.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/email")
@Tag(name = "Email Verification", description = "Email verification APIs")
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    public EmailVerificationController(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @GetMapping("/verify")
    @Operation(summary = "Verify email with token")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully."));
    }

    @PostMapping("/resend")
    @Operation(summary = "Resend verification email")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestParam UUID userId) {
        emailVerificationService.resendVerificationEmail(userId);
        return ResponseEntity.ok(Map.of("message", "Verification email sent."));
    }
}
