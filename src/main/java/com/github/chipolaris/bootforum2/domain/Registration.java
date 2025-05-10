package com.github.chipolaris.bootforum2.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name="REGISTRATION_T")
@TableGenerator(name="RegistrationIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
        pkColumnValue="REGISTRATION_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
public class Registration extends BaseEntity {

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.setCreateDate(now);
        this.setUpdateDate(now);
    }

    @PreUpdate
    public void preUpdate() {
        this.setUpdateDate(LocalDateTime.now());
    }

    // persisted attributes
    @Id
    @GeneratedValue(strategy=GenerationType.TABLE, generator="RegistrationIdGenerator")
    private Long id;

    @Column(name="REGISTRATION_KEY", length=80)
    private String registrationKey;

    @Column(name="USERNAME", length=30)
    private String username;

    @Column(name="EMAIL", length=100)
    private String email;

    @Column(name="PASSWORD", length=200)
    private String password;

    @Override
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getRegistrationKey() {
        return registrationKey;
    }

    public void setRegistrationKey(String registrationKey) {
        this.registrationKey = registrationKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}