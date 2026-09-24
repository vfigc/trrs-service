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
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Validates the short-lived internal service token integration-layer mints for every call to
 * trrs-service (see InternalServiceTokenIssuer on the integration-layer side). Applies to
 * /api/v1/** only - actuator health endpoints stay open for ops/health-check tooling.
 *
 * <p>The verification key(s) are resolved once, at construction, from the platform-shared
 * internal-auth secret in Secrets Manager (internal-auth.secret-arn) - the SAME secret
 * integration-layer signs with, so the two sides can never independently drift out of sync the
 * way two separately-held copies of a raw key could. Every kid present in the secret's keys map
 * is accepted, not just whichever one integration-layer currently signs new tokens with - this is
 * what makes a rotation (add a new kid, flip integration-layer's activeKid, retire the old kid
 * once nothing still holds a token signed with it) work without a synchronized deployment on both
 * sides.
 */
@Component
public class InternalServiceAuthFilter extends OncePerRequestFilter {

    private static final String EXPECTED_ISSUER = "integration-layer";
    private static final String EXPECTED_AUDIENCE = "trrs-service";
    private static final String SUPPORTED_ALGORITHM = "HS256";

    private final Map<String, SecretKey> keysByKid;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InternalServiceAuthFilter(SecretsManagerClient secretsManagerClient,
                                      @Value("${internal-auth.secret-arn}") String secretArn) {
        InternalAuthSecret secret = loadSecret(secretsManagerClient, secretArn);
        if (!SUPPORTED_ALGORITHM.equals(secret.algorithm())) {
            throw new IllegalStateException("internal-auth secret at " + secretArn
                    + " declares algorithm=" + secret.algorithm() + ", but this filter only supports "
                    + SUPPORTED_ALGORITHM + ".");
        }
        if (secret.keys() == null || secret.keys().isEmpty()) {
            throw new IllegalStateException("internal-auth secret at " + secretArn + " has no keys.");
        }
        Map<String, SecretKey> resolved = new LinkedHashMap<>();
        secret.keys().forEach((kid, base64Key) ->
                resolved.put(kid, Keys.hmacShaKeyFor(Base64.getDecoder().decode(base64Key))));
        this.keysByKid = Map.copyOf(resolved);
    }

    private InternalAuthSecret loadSecret(SecretsManagerClient secretsManagerClient, String secretArn) {
        String secretJson = secretsManagerClient.getSecretValue(
                GetSecretValueRequest.builder().secretId(secretArn).build()).secretString();
        try {
            return objectMapper.readValue(secretJson, InternalAuthSecret.class);
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse the internal-auth secret at " + secretArn
                    + " - expected {algorithm, activeKid, previousKid, keys}: " + e.getMessage(), e);
        }
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
                    .keyLocator(jwtHeader -> keysByKid.get(String.valueOf(jwtHeader.get("kid"))))
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
