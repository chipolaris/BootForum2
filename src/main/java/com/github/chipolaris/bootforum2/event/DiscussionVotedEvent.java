package com.github.chipolaris.bootforum2.event;

import com.github.chipolaris.bootforum2.domain.Comment;
import com.github.chipolaris.bootforum2.domain.Discussion;
import org.springframework.context.ApplicationEvent;

public class DiscussionVotedEvent extends ApplicationEvent {

    private final Discussion discussion;
    private final String voterUsername;
    private final short voteValue;

    public DiscussionVotedEvent(Object source, Discussion discussion, String voterUsername, short voteValue) {
        super(source);
        this.discussion = discussion;
        this.voterUsername = voterUsername;
        this.voteValue = voteValue;
    }

    public Discussion getDiscussion() {
        return discussion;
    }

    public String getVoterUsername() {
        return voterUsername;
    }

    public short getVoteValue() {
        return voteValue;
    }
}