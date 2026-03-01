package com.fbp;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class FBPPicks {
    public String email;

    public String picks;
    public String tieBreaker;


    @DynamoDbPartitionKey
    public String getEmail() {
        return this.email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    @DynamoDbAttribute("picks")
    public String getPicks() {
        return this.picks;
    }

    public void setPicks(String picks) {
        this.picks = picks;
    }

    @DynamoDbAttribute("tieBreaker")
    public String gettieBreaker() {
        return this.tieBreaker;
    }

    public void settieBreaker(String tieBreaker) {
        this.tieBreaker = tieBreaker;
    }
    @Override
    public String toString() {
        return "{" +
            " email='" + getEmail() + "'" +
            ", picks='" + getPicks() + "'" +
            ", tieBreaker='" + gettieBreaker() + "'" +
            "}";
    }
}