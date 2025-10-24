package com.github.chipolaris.bootforum2.repository;

import com.github.chipolaris.bootforum2.config.SeedDataInitializer;
import com.github.chipolaris.bootforum2.dao.DynamicDAO;
import com.github.chipolaris.bootforum2.domain.Person;
import com.github.chipolaris.bootforum2.security.JwtAuthenticationFilter;
import com.github.chipolaris.bootforum2.service.ForumSettingService;
import com.github.chipolaris.bootforum2.service.SystemStatistic;
import com.github.chipolaris.bootforum2.test.DataJpaTestWithApplicationMocks;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTestWithApplicationMocks
public class PersonRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PersonRepository personRepository;

    @Test
    void whenExistsByEmail_andEmailExists_thenReturnTrue() {
        // given
        Person person = new Person();
        person.setEmail("test@example.com");
        person.setFirstName("Test");
        person.setLastName("User");
        entityManager.persistAndFlush(person);

        // when
        boolean result = personRepository.existsByEmail("test@example.com");

        // then
        assertThat(result).isTrue();
    }

    @Test
    void whenExistsByEmail_andEmailDoesNotExist_thenReturnFalse() {
        // given
        // No person with the target email is persisted

        // when
        boolean result = personRepository.existsByEmail("nonexistent@example.com");

        // then
        assertThat(result).isFalse();
    }

    @Test
    void whenExistsByEmail_andEmailIsMixedCase_thenFindsLowercaseVersion() {
        // given
        // The @PrePersist hook in Person.java will convert this to lowercase
        Person person = new Person();
        person.setEmail("MixedCase.EMAIL@example.com");
        person.setFirstName("Test");
        person.setLastName("User");
        entityManager.persistAndFlush(person);

        // when
        boolean foundLowercase = personRepository.existsByEmail("mixedcase.email@example.com");
        boolean foundUppercase = personRepository.existsByEmail("MixedCase.EMAIL@example.com");

        // then
        assertThat(foundLowercase).isTrue();
        // This should be false because the email in the DB is stored as lowercase
        assertThat(foundUppercase).isFalse();
    }

    @Test
    void whenExistsByFirstNameAndLastName_andPersonExists_thenReturnTrue() {
        // given
        Person person = new Person();
        person.setFirstName("John");
        person.setLastName("Doe");
        entityManager.persistAndFlush(person);

        // when
        boolean result = personRepository.existsByFirstNameAndLastName("John", "Doe");

        // then
        assertThat(result).isTrue();
    }

    @Test
    void whenExistsByFirstNameAndLastName_andFirstNameMismatches_thenReturnFalse() {
        // given
        Person person = new Person();
        person.setFirstName("John");
        person.setLastName("Doe");
        entityManager.persistAndFlush(person);

        // when
        boolean result = personRepository.existsByFirstNameAndLastName("Jane", "Doe");

        // then
        assertThat(result).isFalse();
    }

    @Test
    void whenExistsByFirstNameAndLastName_andLastNameMismatches_thenReturnFalse() {
        // given
        Person person = new Person();
        person.setFirstName("John");
        person.setLastName("Doe");
        entityManager.persistAndFlush(person);

        // when
        boolean result = personRepository.existsByFirstNameAndLastName("John", "Smith");

        // then
        assertThat(result).isFalse();
    }

    @Test
    void whenExistsByFirstNameAndLastName_andCaseMismatches_thenReturnFalse() {
        // given
        Person person = new Person();
        person.setFirstName("John");
        person.setLastName("Doe");
        entityManager.persistAndFlush(person);

        // when
        boolean result = personRepository.existsByFirstNameAndLastName("john", "doe");

        // then
        assertThat(result).isFalse();
    }
}