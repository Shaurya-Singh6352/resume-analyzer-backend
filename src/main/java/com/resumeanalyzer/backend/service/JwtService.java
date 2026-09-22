package com.resumeanalyzer.backend.service;

import com.resumeanalyzer.backend.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final long expiryHours;

    public JwtService(JwtEncoder encoder,
                      @Value("${app.jwt.expiry-hours:24}") long expiryHours) {
        this.encoder = encoder;
        this.expiryHours = expiryHours;
    }

    /** The token's "subject" is the user's id. Controllers read it to know who is calling. */
    public String createToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("resume-analyzer")
                .issuedAt(now)
                .expiresAt(now.plus(expiryHours, ChronoUnit.HOURS))
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("name", user.getFullName())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}