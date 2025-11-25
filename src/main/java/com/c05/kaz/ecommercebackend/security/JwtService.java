package com.c05.kaz.ecommercebackend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String SECRET_KEY =
            "4C6F6E67426173654B6579546F724A575441757448656C6C6F313233343536373839";

    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000;

    private Key getSignInKey() {
        byte[] keyBytes = hexToBytes(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ======================================================
    //  TOKEN ĐẦY ĐỦ (USERNAME + USERID + ROLES)
    // ======================================================
    public String generateToken(UserDetails userDetails, Long userId) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("id", userId)              // ⭐ QUAN TRỌNG: userId cho chat
                .claim("roles", userDetails.getAuthorities())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ======================================================
    //  CẤM DÙNG generateToken(String username)
    //  (TOKEN TẠO TỪ HÀM NÀY KHÔNG CÓ USER ID → LỖI CHAT)
    // ======================================================
    @Deprecated
    public String generateToken(String username) {
        throw new RuntimeException(
                "Không được dùng generateToken(String). " +
                        "Hãy dùng generateToken(UserDetails, userId) để token chứa id!"
        );
    }

    // ======================================================
    //  TRÍCH XUẤT CLAIMS
    // ======================================================
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        final Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return resolver.apply(claims);
    }

    // ======================================================
    //  VALIDATION
    // ======================================================
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    // HEX -> byte[]
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        if (len % 2 != 0) throw new IllegalArgumentException("Invalid HEX length");

        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(hex.charAt(i), 16);
            int lo = Character.digit(hex.charAt(i + 1), 16);
            if (hi < 0 || lo < 0) throw new IllegalArgumentException("Invalid HEX char");
            data[i / 2] = (byte) ((hi << 4) + lo);
        }
        return data;
    }

    // ======================================================
    //  LẤY USER ID TỪ TOKEN
    // ======================================================
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("id", Long.class));
    }
}
