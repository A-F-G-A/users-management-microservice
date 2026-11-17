package com.jcaa.usersmanagement.infrastructure.security;

import com.jcaa.usersmanagement.application.port.out.GetUserByIdPort;
import com.jcaa.usersmanagement.domain.enums.UserStatus;
import com.jcaa.usersmanagement.domain.exception.DomainException;
import com.jcaa.usersmanagement.domain.valueobject.UserId;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates Bearer tokens. The signature proves who the caller is, but authorities come from
 * the current database row: a user deactivated, blocked, deleted or demoted after login loses its
 * permissions on the next request instead of keeping them until the token expires.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final String ROLE_PREFIX = "ROLE_";

  private final JwtTokenService jwtTokenService;
  private final GetUserByIdPort getUserByIdPort;

  public JwtAuthenticationFilter(
      final JwtTokenService jwtTokenService, final GetUserByIdPort getUserByIdPort) {
    this.jwtTokenService = jwtTokenService;
    this.getUserByIdPort = getUserByIdPort;
  }

  @Override
  protected void doFilterInternal(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final FilterChain filterChain)
      throws ServletException, IOException {
    final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (hasBearerToken(authorization)) {
      authenticate(authorization.substring(BEARER_PREFIX.length()));
    }
    filterChain.doFilter(request, response);
  }

  private static boolean hasBearerToken(final String authorization) {
    return StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX);
  }

  private void authenticate(final String token) {
    try {
      final JwtPrincipal principal = jwtTokenService.parse(token);
      getUserByIdPort
          .getById(new UserId(principal.userId()))
          .filter(user -> user.getStatus() == UserStatus.ACTIVE)
          .ifPresentOrElse(
              user -> {
                final SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority(ROLE_PREFIX + user.getRole().name());
                SecurityContextHolder.getContext()
                    .setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                            principal.userId(), null, List.of(authority)));
              },
              SecurityContextHolder::clearContext);
    } catch (final JwtException | DomainException | IllegalArgumentException ignored) {
      SecurityContextHolder.clearContext();
    }
  }
}
