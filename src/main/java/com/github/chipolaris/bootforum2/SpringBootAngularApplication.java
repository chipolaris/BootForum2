package com.github.chipolaris.bootforum2;

import com.github.chipolaris.bootforum2.security.JwtAuthenticationFilter;
import jakarta.annotation.Resource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class SpringBootAngularApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootAngularApplication.class, args);
    }

    @Configuration
    @EnableWebSecurity // (debug=true) // remove (debug=true) for production readiness
    protected static class SecurityConfiguration {

        static final String[] SECURED_ROLES = new String[]{"ADMIN", "USER"};
        // Define API paths
        private static final String API_AUTH_PATH = "/api/authenticate"; // New JWT auth endpoint
        private static final String API_USER_PROFILE_PATH = "/api/user/profile";

        // Inject the custom JWT filter
        @Resource
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
            // Expose AuthenticationManager as a Bean for the AuthController
            return authenticationConfiguration.getAuthenticationManager();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

            http
                    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                    // *** Disable CSRF - Not needed for stateless JWT auth via headers ***
                    .csrf(csrf -> csrf.disable())
                    // Exception Handling: Return 401 for unauthorized access attempts
                    .exceptionHandling(eh -> eh
                            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                    )
                    // *** Set session management to STATELESS ***
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    // Authorization Rules
                    .authorizeHttpRequests(authz -> authz
                            // Permit static resources, Angular app routes, public API, and NEW AUTH endpoint
                            .requestMatchers(
                                    "/",
                                    "/index.html",
                                    "/assets/**",      // Permit everything under assets
                                    "/favicon.ico",
                                    "/*.js",           // Matches main.js, polyfills.js, runtime.js etc. at the root
                                    "/*.css",          // Matches styles.css etc. at the root
                                    "/*.png", "/*.jpg", "/*.jpeg", "/*.gif", "/*.svg", // Common image types at the root (if any)
                                    "/*.woff", "/*.woff2", "/*.ttf", "/*.eot", // Common font types at the root (if any)
                                    "/manifest.webmanifest", // Example: PWA manifest
                                    "/media/**", // primeicons files are put in the resources/static/browser/media folder
                                    // Keep other permitted paths
                                    "/attachment/**",  // Assuming this is public or handled separately
                                    "/api/public/**",
                                    "/error",
                                    API_AUTH_PATH // Permit auth endpoint
                            ).permitAll()
                            // Secure specific API endpoints
                            .requestMatchers(API_USER_PROFILE_PATH).authenticated() // Still requires authentication
                            .requestMatchers("/api/secured/**").hasAnyRole(SECURED_ROLES)
                            // Any other request must be authenticated
                            .anyRequest().authenticated()
                    );

            // *** Add JWT filter before the standard UsernamePasswordAuthenticationFilter ***
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

            return http.build();
        }

        @Bean
        CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
            configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
            // Ensure 'Authorization' header is allowed if not using "*"
            configuration.setAllowedHeaders(List.of("*")); // Or specify headers including "Authorization", "Content-Type"
            // configuration.setAllowCredentials(true); // No longer strictly required for JWT via header, can often be removed
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);
            return source;
        }
    }

    /**
     * enable Angular static resources to be served from the /static/browser folder
     */
    @Configuration
    public class AngularResourceConfiguration implements WebMvcConfigurer {
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/**").addResourceLocations("classpath:/static/browser")
                    .resourceChain(true)
                    .addResolver(new PathResourceResolver() {
                        @Override
                        protected org.springframework.core.io.Resource getResource(String resourcePath,
                                                                                   org.springframework.core.io.Resource location) throws IOException {
                            org.springframework.core.io.Resource requestedResource = location.createRelative(resourcePath);
                            return requestedResource.exists() && requestedResource.isReadable() ? requestedResource
                                    : new ClassPathResource("/static/browser/index.html");
                        }
                    });
            //registry.setOrder(Integer.MAX_VALUE - 1);  // *2* make sure this order value is less than the one above (*1*)
        }

        /**
         * forward requests to the root ("/") to index.html
         * This takes precedence over default welcome page handling for "/"
         */
        @Override
        public void addViewControllers(ViewControllerRegistry registry) {
            registry.addViewController("/").setViewName("forward:/index.html");
        }
    }

    /*
     * passwordEncoder declared here is used implicitly by Spring Security
     * E.g, no need to define the configureGlobal(AuthenticationManagerBuilder auth) method
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
