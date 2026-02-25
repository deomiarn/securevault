package com.securevault.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.security.interfaces.RSAPublicKey;

@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private RSAPublicKey publicKey;
}
