package com.github.chipolaris.bootforum2.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

@Entity
@Table(name="FILE_INFO_T")
@TableGenerator(name="FileInfoIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
        pkColumnValue="FILE_INFO_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
public class FileInfo extends BaseEntity {

    @Id
    @GeneratedValue(strategy=GenerationType.TABLE, generator="FileInfoIdGenerator")
    private Long id;

    @Column(name="DESCRIPTION", length=200)
    private String description;

    @Column(name="MIME_TYPE", length=100)
    private String mimeType;

    @Column(name="PATH", length=2000)
    private String path;

    @Override
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getMimeType() {
        return mimeType;
    }
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
}