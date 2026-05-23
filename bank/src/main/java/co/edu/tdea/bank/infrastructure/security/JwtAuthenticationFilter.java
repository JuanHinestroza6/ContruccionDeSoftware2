package co.edu.tdea.bank.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Stateless JWT authentication filter.
 *
 * <p>Runs once per request, ahead of {@code UsernamePasswordAuthenticationFilter}.
 * Reads the {@code Authorization: Bearer <token>} header, validates the token,
 * loads the matching {@link UserDetails}, and populates the
 * {@link SecurityContextHolder}. If anything is missing or invalid the filter
 * simply continues without setting an authentication — protected endpoints
 * will then return 401 via the standard {@code authenticationEntryPoint}.
 *
 * <p>The filter never throws on invalid tokens and never logs token contents.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final BankUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   BankUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        if (!jwtService.validateToken(token)) {
            chain.doFilter(request, response);
            return;
        }

        // Only set the SecurityContext when there is no prior authentication.
        // This keeps the filter idempotent in case another filter has already
        // authenticated the request (e.g. tests using @WithMockUser).
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String username = jwtService.extractUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (UsernameNotFoundException ex) {
                // The token referenced a user that no longer exists or has been
                // removed. Fall through without authenticating; Spring will
                // return 401 on the protected resource.
            }
        }

        chain.doFilter(request, response);
    }
}
