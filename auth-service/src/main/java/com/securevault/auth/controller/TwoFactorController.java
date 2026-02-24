package com.securevault.auth.controller;

import com.securevault.auth.dto.AuthResponse;
import com.securevault.auth.dto.TotpLoginRequest;
import com.securevault.auth.dto.TotpSetupResponse;
import com.securevault.auth.dto.TotpVerifyRequest;
import com.securevault.auth.entity.User;
import com.securevault.auth.exception.TotpVerificationException;
import com.securevault.auth.repository.UserRepository;
import com.securevault.auth.service.AuthService;
import com.securevault.auth.service.JwtService;
import com.securevault.auth.service.TotpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
public class TwoFactorController {
    private final TotpService totpService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthService authService;

    @PostMapping("/setup")
    public ResponseEntity<TotpSetupResponse> setup(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow();

        String secret = totpService.generateSecret();
        user.setTotpSecret(secret);
        userRepository.save(user);

        String qrCodeUri = totpService.generateQrCodeUri(secret, user.getEmail());
        return ResponseEntity.ok(TotpSetupResponse.builder()
                .secret(secret)
                .qrCodeUri(qrCodeUri)
                .build());
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TotpVerifyRequest totpVerifyRequest) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow();

        totpService.enableTotp(user.getId(), totpVerifyRequest.getCode());

        return ResponseEntity.ok(Map.of("message", "2FA enabled successfully"));
    }

    @PostMapping("/verify-login")
    public ResponseEntity<AuthResponse> verifyLogin(@Valid @RequestBody TotpLoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow();

        if (!totpService.verifyCode(user.getTotpSecret(), request.getCode())) {
            throw new TotpVerificationException("Invalid TOTP code");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        authService.saveRefreshToken(user, refreshToken);

        return ResponseEntity.ok(AuthResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .message("Login successful")
                .build());
    }

    @PostMapping("/disable")
    public ResponseEntity<Map<String, String>> disable(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody TotpVerifyRequest request) {

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow();

        totpService.disableTotp(user.getId(), request.getCode());

        return ResponseEntity.ok(Map.of("message", "2FA disabled successfully"));
    }
}
