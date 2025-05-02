package com.github.chipolaris.bootforum2.security; // Adjust package

import com.github.chipolaris.bootforum2.service.AppUserDetailsService;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component // Make it a Spring bean so it can be injected
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    // Use @Autowired or constructor injection
    @Resource
    private JwtTokenProvider tokenProvider;

    @Resource
    private AppUserDetailsService userDetailsService; // Your existing UserDetailsService

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsernameFromJWT(jwt);

                // Load user details (authorities)
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                // Create authentication object
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Set authentication in security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
                logger.debug("Set SecurityContext for user '{}', URI: {}", username, request.getRequestURI());
            } else {
                if (StringUtils.hasText(jwt)) {
                    logger.debug("JWT validation failed for token prefix: {}", jwt.substring(0, Math.min(jwt.length(), 10)) + "...");
                } else {
                    // Optional: Log requests without JWT if needed for debugging specific paths
                    // logger.trace("No JWT found for URI: {}", request.getRequestURI());
                }
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response); // Continue filter chain
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Extract token after "Bearer "
        }
        return null;
    }
}
