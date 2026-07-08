package com.aiflow.enterprise.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final SecretKey accessTokenSecret;
    private final SecretKey refreshTokenSecret;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.access-token-secret}") String accessTokenSecret,
            @Value("${app.jwt.refresh-token-secret}") String refreshTokenSecret,
            @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs) {
        this.accessTokenSecret = Keys.hmacShaKeyFor(
                Base64.getEncoder().encodeToString(accessTokenSecret.getBytes(StandardCharsets.UTF_8))
                        .getBytes(StandardCharsets.UTF_8));
        this.refreshTokenSecret = Keys.hmacShaKeyFor(
                Base64.getEncoder().encodeToString(refreshTokenSecret.getBytes(StandardCharsets.UTF_8))
                        .getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(String userId, String username, Set<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("username", username)
                .claim("roles", roles)
                .issuer("ai-flow-enterprise")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpirationMs)))
                .signWith(accessTokenSecret)
                .compact();
    }

    public String generateRefreshToken(String userId, String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .claim("username", username)
                .claim("type", "refresh")
                .issuer("ai-flow-enterprise")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(refreshTokenExpirationMs)))
                .signWith(refreshTokenSecret)
                .compact();
    }

    public String getUserIdFromAccessToken(String token) {
        return parseClaims(token, accessTokenSecret).getSubject();
    }

    public String getUserIdFromRefreshToken(String token) {
        return parseClaims(token, refreshTokenSecret).getSubject();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseClaims(token, accessTokenSecret);
        return claims.get("username", String.class);
    }

    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        Claims claims = parseClaims(token, accessTokenSecret);
        return Set.copyOf(claims.get("roles", java.util.List.class));
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, accessTokenSecret);
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, refreshTokenSecret);
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    private Claims parseClaims(String token, SecretKey secret) {
        return Jwts.parser()
                .verifyWith(secret)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean validateToken(String token, SecretKey secret) {
        try {
            Jwts.parser().verifyWith(secret).build().parseSignedClaims(token);
            return true;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
