package com.github.chipolaris.bootforum2.domain;

import jakarta.persistence.*;

import java.io.File;
import java.time.LocalDateTime;

@Entity
@Table(name="AVATAR_T")
@TableGenerator(name="AvatarIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
        pkColumnValue="AVATAR_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
public class Avatar extends BaseEntity {

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

    @Id
    @GeneratedValue(strategy=GenerationType.TABLE, generator="AvatarIdGenerator")
    private Long id;

    @Column(name="USER_NAME", length=50, unique = true, nullable = false)
    private String userName;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name="FILE_INFO_ID", foreignKey = @ForeignKey(name="FK_AVATAR_FILE_INFO"))
    private FileInfo file;

    @Override
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public FileInfo getFile() {
        return file;
    }
    public void setFile(FileInfo file) {
        this.file = file;
    }
}