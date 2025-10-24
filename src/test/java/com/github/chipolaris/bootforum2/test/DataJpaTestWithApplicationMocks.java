package com.github.chipolaris.bootforum2.test;

import com.github.chipolaris.bootforum2.config.SeedDataInitializer;
import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.security.JwtAuthenticationFilter;
import com.github.chipolaris.bootforum2.service.ForumSettingService;
import com.github.chipolaris.bootforum2.service.SystemStatistic;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoBeans; // Import the container annotation

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom meta-annotation for JPA slice tests.
 * <p>
 * This annotation bundles {@link DataJpaTest} with all the {@link MockitoBean}
 * definitions required to satisfy the dependencies of the main application
 * context (e.g., CommandLineRunners, ApplicationListeners).
 * <p>
 * Using this annotation keeps repository tests clean and centralizes the
 * mock configuration in one place.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
// Mock beans required by SpringBootAngularApplication to allow the test context to load
@MockitoBeans({ // Use the container annotation and array syntax
        @MockitoBean(types = JwtAuthenticationFilter.class),
        @MockitoBean(types = SeedDataInitializer.class),
        @MockitoBean(types = DynamicDAO.class),
        @MockitoBean(types = ForumSettingService.class),
        @MockitoBean(types = SystemStatistic.class)
})
public @interface DataJpaTestWithApplicationMocks {
}
