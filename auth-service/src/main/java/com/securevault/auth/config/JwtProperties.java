package com.securevault.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {
    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;
}
