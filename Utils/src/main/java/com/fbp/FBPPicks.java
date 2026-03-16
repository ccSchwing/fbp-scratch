package com.fbp;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class FBPPicks {
    public String email;
    public String displayName;
    public String picks;
    public Integer tieBreaker;
    public Integer week;


    @DynamoDbPartitionKey
    public String getEmail() {
        return this.email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    @DynamoDbAttribute("displayName")
    public String getDisplayName() {
        return this.displayName;
    }
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @DynamoDbAttribute("picks")
    public String getPicks() {
        return this.picks;
    }

    public void setPicks(String picks) {
        this.picks = picks;
    }

    @DynamoDbAttribute("tieBreaker")
    public Integer getTieBreaker() {
        return this.tieBreaker;
    }

    public void setTieBreaker(Integer tieBreaker) {
        this.tieBreaker = tieBreaker;
    }

    @DynamoDbAttribute("week")
    public Integer getWeek() {
        return this.week;
    }
    public void setWeek(Integer week) {
        this.week = week;
    }

    @Override
    public String toString() {
        return "{" +
            " email='" + getEmail() + "'" +
            ", displayName='" + getDisplayName() + "'" +
            ", picks='" + getPicks() + "'" +
            ", tieBreaker='" + getTieBreaker() + "'" +
            ", week='" + getWeek() + "'" +
            "}";
    }
}