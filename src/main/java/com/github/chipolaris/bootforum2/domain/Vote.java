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
@Table(name="VOTE_T")
@TableGenerator(name="VoteIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
        pkColumnValue="VOTE_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
public class Vote extends BaseEntity {

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if(this.getCreateDate() == null) {
            this.setCreateDate(now);
        }
        if(this.getUpdateDate() == null) {
            this.setUpdateDate(now);
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.setUpdateDate(LocalDateTime.now());
    }

    @Id
    @GeneratedValue(strategy=GenerationType.TABLE, generator="VoteIdGenerator")
    private Long id;

    @Column(name="VOTER_NAME", length=50)
    private String voterName;

    @Column(name="VOTE_VALUE")
    private short voteValue;

    @Override
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getVoterName() {
        return voterName;
    }
    public void setVoterName(String voterName) {
        this.voterName = voterName;
    }

    public short getVoteValue() {
        return voteValue;
    }
    public void setVoteValue(short voteValue) {
        this.voteValue = voteValue;
    }
}