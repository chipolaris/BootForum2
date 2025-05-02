package com.github.chipolaris.bootforum2.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.TableGenerator;

@Entity
@Table(name="SECURITY_CHALLENGE_T")
@TableGenerator(name="SecurityChallengeIdGenerator", table="ENTITY_ID_T", pkColumnName="GEN_KEY",
        pkColumnValue="SECURITY_CHALLENGE_ID", valueColumnName="GEN_VALUE", initialValue = 1000, allocationSize=10)
public class SecurityChallenge extends BaseEntity {

    @Id
    @GeneratedValue(strategy=GenerationType.TABLE, generator="SecurityChallengeIdGenerator")
    private Long id;

    @ManyToOne
    @JoinColumn(name="USER_ID", foreignKey = @ForeignKey(name = "FK_SECURI_CHALLE_USER"))
    private User user;

    @Column(name="QUESTION", length=200)
    private String question;

    @Column(name="ANSWER", length=50)
    private String answer;

    @Override
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }

    public String getQuestion() {
        return question;
    }
    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }
    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
