package com.github.chipolaris.bootforum2;

import com.github.chipolaris.bootforum2.config.SeedDataInitializer;
import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.dao.FilterSpec;
import com.github.chipolaris.bootforum2.dao.QuerySpec;
import com.github.chipolaris.bootforum2.domain.Person;
import com.github.chipolaris.bootforum2.domain.User;
import com.github.chipolaris.bootforum2.security.JwtAuthenticationFilter;
import com.github.chipolaris.bootforum2.service.ForumSettingService;
import com.github.chipolaris.bootforum2.service.SystemStatistic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.EnableAsync;
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

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@EnableAsync
public class SpringBootAngularApplication {

    private static final Logger logger = LoggerFactory.getLogger(SpringBootAngularApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootAngularApplication.class, args);
    }

    @Configuration
    @EnableWebSecurity // (debug=true) // remove (debug=true) for production readiness
    protected static class SecurityConfiguration {

        static final String[] SECURED_ROLES = new String[]{"ADMIN", "USER"};
        static final String[] ADMIN_ROLES = new String[]{"ADMIN"};
        // Define API paths
        private static final String API_AUTH_PATH = "/api/authenticate"; // New JWT auth endpoint
        private static final String API_USER_PROFILE_PATH = "/api/user/profile";

        // Inject the custom JWT filter
        @Autowired
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
                                    // --- START: SPA-aware security configuration ---
                                    // 1. Secure API endpoints first
                                    .requestMatchers("/api/admin/**").hasAnyRole(ADMIN_ROLES)
                                    .requestMatchers("/api/user/**").hasAnyRole(SECURED_ROLES)
                                    .requestMatchers("/api/secured/**").hasAnyRole(SECURED_ROLES)
                                    // 2. Explicitly permit public API endpoints
                                    .requestMatchers(API_AUTH_PATH, "/api/public/**").permitAll()
                                    // 3. Permit all other requests (Angular routes, static assets like .js, .css, .ico)
                                    // The PathResourceResolver will handle serving index.html for Angular routes.
                                    .anyRequest().permitAll()
                            // --- END: SPA-aware security configuration ---
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
            registry.addResourceHandler("/**").addResourceLocations("classpath:/static/browser/")
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

    @Bean @Order(1)
    CommandLineRunner validateSchema(DataSource dataSource) {
        return args -> {
            try (Connection conn = dataSource.getConnection()) {
                var rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"});
                logger.info("Tables visible to Spring Boot:");
                while (rs.next()) {
                    logger.info(" - " + rs.getString("TABLE_NAME"));
                }
            }
        };
    }

    @Bean @Order(2)
    CommandLineRunner initializeSeedData(SeedDataInitializer seedDataInitializer) {
        return args -> seedDataInitializer.initializeSeedData();
    }

    @Bean @Order(3)
    CommandLineRunner validateSeedData(DynamicDAO dynamicDAO) {
        return args -> {
            logger.info("Validating data...");

            if(dynamicDAO.exists(QuerySpec.builder(User.class).filter(FilterSpec.eq("username", "admin")).build())) {
                logger.info("User 'admin' exists (as expected)");
            }
            else {
                logger.info("User 'admin' does not exist (something wrong)");
            }

            if(dynamicDAO.exists(QuerySpec.builder(User.class).filter(FilterSpec.eq("username", "user1")).build())) {
                logger.info("User 'user1' exists (as expected)");
            }
            else {
                logger.info("User 'user1' does not exist (something wrong)");
            }

            if(dynamicDAO.exists(QuerySpec.builder(Person.class).filter(FilterSpec.eq("firstName", "Admin"))
                    .filter(FilterSpec.eq("lastName", "User")).build())) {
                logger.info("Person 'Admin User' exists (as expected)");
            }
            else {
                logger.info("Person 'Admin User' does not exist (something wrong)");
            }

            if(dynamicDAO.exists(QuerySpec.builder(Person.class).filter(FilterSpec.eq("firstName", "One"))
                    .filter(FilterSpec.eq("lastName", "User")).build())) {
                logger.info("Person 'One User' exists (as expected)");
            }
            else {
                logger.info("Person 'One User' does not exist (something wrong)");
            }
        };
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> initializeForumDefaults(
            ForumSettingService forumSettingsService) {
        return event -> {
            logger.info("Application is fully ready! Performing post-startup tasks...");

            if (forumSettingsService.isEmpty()) {
                logger.info("Initialize Forum Defaults...");
                forumSettingsService.initializeFromDefaults();
            }
            else {
                logger.info("Backfill Forum Defaults...");
                forumSettingsService.backfillMissingDefaults();
            }
        };
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> initializeSystemStatistic(SystemStatistic systemStatistic) {
        return event -> systemStatistic.initializeStatistics();
    }
}