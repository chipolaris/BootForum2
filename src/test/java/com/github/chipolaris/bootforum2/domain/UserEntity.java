package com.github.chipolaris.bootforum2.domain;

/**
 * Domain class for unit testing JPA operations
 */

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Integer age;
    private String status;

    // Default constructor for JPA
    public UserEntity() {}

    public UserEntity(String name, Integer age, String status) {
        this.name = name;
        this.age = age;
        this.status = status;
    }

    // Getters only — no setters needed for tests

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }

    public String getStatus() {
        return status;
    }
}

