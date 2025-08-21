package com.github.chipolaris.bootforum2.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "FORUM_SETTING_T")
@TableGenerator(name="ForumSettingIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
        pkColumnValue="FORUM_SETTING_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
public class ForumSetting extends BaseEntity {

    public static ForumSetting newInstance(String category, String key) {
        ForumSetting newInstance = new ForumSetting();
        newInstance.setCategory(category);
        newInstance.setKeyName(key);
        return newInstance;
    }

    @PrePersist @PreUpdate
    public void onUpdate() {
        this.setUpdateDate(LocalDateTime.now());
    }

    @Id
    @GeneratedValue(strategy=GenerationType.TABLE, generator="ForumSettingIdGenerator")
    private Long id;

    @Column(name = "category", nullable = false, length = 50)
    private String category; // e.g., "general", "users", "system"

    @Column(name = "key_name", nullable = false, length = 100)
    private String keyName; // e.g., "siteName", "registration.type"

    @Lob
    @Column(name = "setting_value", nullable = false) // "value" is a reserved keyword in some SQL databases
    private String value; // stored as JSON or string

    @Column(name = "value_type", nullable = false, length = 20)
    private String valueType; // "string", "boolean", "int", "json"

    @Override
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    // Getters & Setters

    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }

    public String getKeyName() {
        return keyName;
    }
    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

    public String getValueType() {
        return valueType;
    }
    public void setValueType(String valueType) {
        this.valueType = valueType;
    }
}

