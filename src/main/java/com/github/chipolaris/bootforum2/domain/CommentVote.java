package com.github.chipolaris.bootforum2.domain;

import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

@Entity
@Table(name="COMMENT_VOTE_T")
@TableGenerator(name="CommentVoteIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
        pkColumnValue="COMMENT_VOTE_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
public class CommentVote extends BaseEntity {

    @Id
    @GeneratedValue(strategy=GenerationType.TABLE, generator="CommentVoteIdGenerator")
    private Long id;

    @Column(name="VOTE_UP_COUNT")
    private int voteUpCount;

    @Column(name="VOTE_DOWN_COUNT")
    private int voteDownCount;

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY)
    @JoinTable(name="COMMENT_VOTE_VOTE_T",
            joinColumns={@JoinColumn(name="COMMENT_VOTE_ID", foreignKey = @ForeignKey(name="FK_COMMEN_VOT_VOT_COMMEN_VOTE"))},
            inverseJoinColumns={@JoinColumn(name="VOTE_ID", foreignKey = @ForeignKey(name="FK_COMMEN_VOT_VOT_VOTE"))},
            indexes = {@Index(name="IDX_COMMEN_VOTE_VOTE", columnList = "COMMENT_VOTE_ID,VOTE_ID")})
    private Set<Vote> votes;

    @Override
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public int getVoteUpCount() {
        return voteUpCount;
    }
    public void setVoteUpCount(int voteUpCount) {
        this.voteUpCount = voteUpCount;
    }

    public int getVoteDownCount() {
        return voteDownCount;
    }
    public void setVoteDownCount(int voteDownCount) {
        this.voteDownCount = voteDownCount;
    }

    public Set<Vote> getVotes() {
        return votes;
    }
    public void setVotes(Set<Vote> votes) {
        this.votes = votes;
    }
}