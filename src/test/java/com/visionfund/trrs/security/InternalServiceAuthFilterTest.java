package com.visionfund.trrs.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InternalServiceAuthFilterTest {

    private static final String SECRET_ARN = "arn:aws:secretsmanager:eu-west-1:000000000000:secret:test";
    private static final String ACTIVE_KID = "test-kid-a";
    private static final String PREVIOUS_KID = "test-kid-b";
    private static final String ACTIVE_KEY_BASE64 = Base64.getEncoder().encodeToString(
            "test-only-trrs-secret-value-32by".getBytes());
    private static final String PREVIOUS_KEY_BASE64 = Base64.getEncoder().encodeToString(
            "a-different-32-byte-previous-key".getBytes());

    private InternalServiceAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = buildFilter(secretJson(ACTIVE_KID, ACTIVE_KEY_BASE64, PREVIOUS_KID, PREVIOUS_KEY_BASE64));
    }

    private static String secretJson(String activeKid, String activeKeyBase64, String previousKid, String previousKeyBase64) {
        return "{\"algorithm\":\"HS256\",\"activeKid\":\"" + activeKid + "\",\"previousKid\":\"" + previousKid + "\","
                + "\"keys\":{\"" + activeKid + "\":\"" + activeKeyBase64 + "\","
                + "\"" + previousKid + "\":\"" + previousKeyBase64 + "\"}}";
    }

    private static InternalServiceAuthFilter buildFilter(String secretJson) {
        SecretsManagerClient secretsManagerClient = mock(SecretsManagerClient.class);
        when(secretsManagerClient.getSecretValue(any(GetSecretValueRequest.class)))
                .thenReturn(GetSecretValueResponse.builder().secretString(secretJson).build());
        return new InternalServiceAuthFilter(secretsManagerClient, SECRET_ARN);
    }

    private static String tokenSignedWith(String kid, String keyBase64) {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(keyBase64));
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId(kid).and()
                .issuer("integration-layer")
                .audience().add("trrs-service").and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60)))
                .signWith(key)
                .compact();
    }

    private String validToken() {
        return tokenSignedWith(ACTIVE_KID, ACTIVE_KEY_BASE64);
    }

    private String tokenWithWrongAudience() {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(ACTIVE_KEY_BASE64));
        Instant now = Instant.now();
        return Jwts.builder()
                .header().keyId(ACTIVE_KID).and()
                .issuer("integration-layer")
                .audience().add("some-other-service").and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60)))
                .signWith(key)
                .compact();
    }

    @Test
    void bypassesFilterForNonApiPaths() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    @Test
    void appliesFilterToApiV1Paths() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/routes");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    @Test
    void rejectsRequestWithNoAuthorizationHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/routes");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("TRRS-AUTH-401-001");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void rejectsMalformedToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/routes");
        request.addHeader("Authorization", "Bearer not-a-real-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid or expired internal service token");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void rejectsTokenWithAnUnrecognizedKid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/routes");
        request.addHeader("Authorization", "Bearer " + tokenSignedWith("some-unknown-kid", ACTIVE_KEY_BASE64));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void rejectsTokenWithWrongAudience() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/routes");
        request.addHeader("Authorization", "Bearer " + tokenWithWrongAudience());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Token issuer/audience mismatch");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void allowsRequestThroughWithAValidTokenSignedByTheActiveKid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/routes");
        request.addHeader("Authorization", "Bearer " + validToken());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void allowsRequestThroughWithATokenStillSignedByThePreviousKidDuringARotationWindow() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/routes");
        request.addHeader("Authorization", "Bearer " + tokenSignedWith(PREVIOUS_KID, PREVIOUS_KEY_BASE64));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void refusesToStartWithAnUnsupportedAlgorithm() {
        String secretJson = "{\"algorithm\":\"HS512\",\"activeKid\":\"" + ACTIVE_KID + "\",\"previousKid\":null,"
                + "\"keys\":{\"" + ACTIVE_KID + "\":\"" + ACTIVE_KEY_BASE64 + "\"}}";

        assertThatThrownBy(() -> buildFilter(secretJson))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HS256");
    }

    @Test
    void refusesToStartWhenTheSecretHasNoKeys() {
        String secretJson = "{\"algorithm\":\"HS256\",\"activeKid\":\"" + ACTIVE_KID + "\",\"previousKid\":null,\"keys\":{}}";

        assertThatThrownBy(() -> buildFilter(secretJson))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no keys");
    }

    @Test
    void refusesToStartWhenTheSecretIsMalformed() {
        assertThatThrownBy(() -> buildFilter("not-json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not parse");
    }
}
