package com.application.gms.security;

import com.application.gms.dto.TokenPair;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SignatureException;
import java.util.Date;
import java.util.Map;


@Component
@Slf4j
public class JwtTokenProvider {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.issuer}")
    private String jwtIssuer;

    @Value("${jwt.accessExpiration}")
    private long accessTokenExpirationMS;

    @Value("${jwt.refreshExpiration}")
    private long refreshTokenExpirationMS;

    public TokenPair generateTokenPair(Authentication authentication) {
        String accessToken = generateAccessToken(authentication);
        String refreshToken = generateRefreshToken(authentication);
        return new TokenPair(accessToken, refreshToken, accessTokenExpirationMS, refreshTokenExpirationMS);
    }

    //  Generate Access Token
    public String generateAccessToken(Authentication authentication){
        UserDetails userPrincipal  = (UserDetails) authentication.getPrincipal();

        assert userPrincipal != null;
        return generateToken(userPrincipal.getUsername(), accessTokenExpirationMS, null);
    }

    // Generate Refresh Token
    public String generateRefreshToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Map<String, Object> claims = Map.of("type", "refresh");

        assert userPrincipal != null;
        return generateToken(userPrincipal.getUsername(), refreshTokenExpirationMS, claims);
    }

    // Generate JWT Token
    private String generateToken(String username, long expirationMs, Map<String, Object> claims) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        JwtBuilder builder = Jwts.builder()
                .issuer(jwtIssuer)
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSecretKey());

        if(claims != null) {
            builder.claims(claims);
        }
        return builder.compact();
    }

    // Extract username from token
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token)
                .getSubject();
    }

    // Validate Token
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith((SecretKey) getSecretKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired");
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty");
        }
        return false;
    }


    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


    public String extractTokenFromHeader(String bearerToken) {
        if (bearerToken !=null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private Key getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }


}
