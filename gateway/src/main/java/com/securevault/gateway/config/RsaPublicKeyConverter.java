package com.securevault.gateway.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
@ConfigurationPropertiesBinding
public class RsaPublicKeyConverter implements Converter<String, RSAPublicKey> {

    @Override
    public RSAPublicKey convert(String source) {
        try {
            String pem;
            if (source.startsWith("classpath:") || source.startsWith("file:")) {
                Resource resource = new DefaultResourceLoader().getResource(source);
                pem = new String(resource.getInputStream().readAllBytes());
            } else {
                pem = source;
            }

            pem = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(pem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert PEM to RSAPublicKey", e);
        }
    }
}
