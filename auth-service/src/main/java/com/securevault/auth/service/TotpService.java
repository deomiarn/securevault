package com.securevault.auth.service;

import com.securevault.auth.client.AuditClient;
import com.securevault.auth.entity.User;
import com.securevault.auth.exception.TotpVerificationException;
import com.securevault.auth.repository.UserRepository;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TotpService {
    private final UserRepository userRepository;
    private final AuditClient auditClient;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);

    // Random Base32 secret generator
    public String generateSecret() {
        return secretGenerator.generate();
    }

    public String generateQrCodeUri(String secret, String email) {
        return String.format(
                "otpauth://totp/SecureVault:%s?secret=%s&issuer=SecureVault&algorithm=SHA1&digits=6&period=30",
                email, secret
        );
    }

    public boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }

    public void enableTotp(UUID userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getTotpSecret() == null) {
            throw new TotpVerificationException("No TOTP secret found. Call setup first.");
        }

        if (!verifyCode(user.getTotpSecret(), code)) {
            throw new TotpVerificationException("Invalid TOTP code");
        }

        user.setTotpEnabled(true);
        userRepository.save(user);

        auditClient.logAuth(userId, "TOTP_ENABLED", "SUCCESS",
                "TOTP enabled for user", null);
    }

    public void disableTotp(UUID userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!verifyCode(user.getTotpSecret(), code)) {
            throw new TotpVerificationException("Invalid TOTP code");
        }

        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);

        auditClient.logAuth(userId, "TOTP_DISABLED", "SUCCESS",
                "TOTP disabled for user", null);
    }
}
