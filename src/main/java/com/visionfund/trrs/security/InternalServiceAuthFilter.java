package com.visionfund.trrs.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validates the short-lived internal service token integration-layer mints
 * for every call to trrs-service (see InternalServiceTokenIssuer on the
 * integration-layer side). Applies to /api/v1/** only - actuator health
 * endpoints stay open for ops/health-check tooling.
 */
@Component
public class InternalServiceAuthFilter extends OncePerRequestFilter {

    private static final String EXPECTED_ISSUER = "integration-layer";
    private static final String EXPECTED_AUDIENCE = "trrs-service";

    private final SecretKey key;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InternalServiceAuthFilter(@Value("${internal-auth.secret}") String base64Secret) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Secret));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            reject(response, "Missing Authorization header");
            return;
        }
        String token = header.substring("Bearer ".length());
        try {
            var claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!EXPECTED_ISSUER.equals(claims.getIssuer())
                    || claims.getAudience() == null
                    || !claims.getAudience().contains(EXPECTED_AUDIENCE)) {
                reject(response, "Token issuer/audience mismatch");
                return;
            }
        } catch (JwtException | IllegalArgumentException e) {
            reject(response, "Invalid or expired internal service token");
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "TRRS-AUTH-401-001");
        body.put("message", message);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
