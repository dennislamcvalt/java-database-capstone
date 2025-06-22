package com.project.back_end.services;

import com.project.back_end.models.Admin;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component // 1. Spring-managed component
public class TokenService {

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    @Value("${jwt.secret}")
    private String secret;

    private Key signingKey;

    // 2. Constructor injection
    public TokenService(AdminRepository adminRepository,
                        DoctorRepository doctorRepository,
                        PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    // 3. Get signing key
    @PostConstruct
    private void init() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    // 4. Generate token
    public String generateToken(Long id, String role, String email) {
        long expirationMillis = 1000 * 60 * 60 * 24 * 7; // 7 days
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("id", id)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // 5. Extract email (subject) from token
    public String extractEmailFromToken(String token) {
        try {
            return parseToken(token).getBody().getSubject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 6. Validate token based on role
    public boolean validateToken(String token, String role) {
        try {
            Claims claims = parseToken(token).getBody();
            String email = claims.getSubject();

            return switch (role.toLowerCase()) {
                case "admin" -> adminRepository.findByUsername(email) != null;
                case "doctor" -> doctorRepository.findByEmail(email) != null;
                case "patient" -> patientRepository.findByEmail(email) != null;
                default -> false;
            };

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Utility: Token parser
    private Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token);
    }
}
