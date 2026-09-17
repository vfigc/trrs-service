package com.visionfund.trrs.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class InternalServiceAuthFilterTest {

    private static final String SECRET_BASE64 = Base64.getEncoder().encodeToString(
            "test-only-trrs-secret-value-32by".getBytes());

    private InternalServiceAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new InternalServiceAuthFilter(SECRET_BASE64);
    }

    private String validToken() {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_BASE64));
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer("integration-layer")
                .audience().add("trrs-service").and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(60)))
                .signWith(key)
                .compact();
    }

    private String tokenWithWrongAudience() {
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET_BASE64));
        Instant now = Instant.now();
        return Jwts.builder()
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
    void allowsRequestThroughWithValidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/routes");
        request.addHeader("Authorization", "Bearer " + validToken());
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain, times(1)).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
