package com.fbp;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class FBPLogRecord {
    private String email;                // This will be the email address of the user
    private ZonedDateTime timestamp;
    private String level;
    private String action;
    private String details;

    // return createdDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    // this.createdDate = ZonedDateTime.parse(isoString);




    @DynamoDbPartitionKey
    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    @DynamoDbSortKey
    public ZonedDateTime getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = ZonedDateTime.now(ZoneId.of("America/New_York"));
    }
    @DynamoDbAttribute("level")
    public String getLevel() {
        return this.level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
    @DynamoDbAttribute("action")
    public String getAction() {
        return this.action;
    }

    public void setAction(String action) {
        this.action = action;
    }
    @DynamoDbAttribute("details")
    public String getDetails() {
        return this.details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
    public FBPLogRecord() {
    }

    
    public FBPLogRecord(String email, ZonedDateTime timestamp, String level, String action, String details) {
        this.email = email;
        this.timestamp = timestamp;
        this.level = level;
        this.action = action;
        this.details = details;
    }

    @Override
    public String toString() {
        return "{" +
            " email='" + getEmail() + "'" +
            ", timestamp='" + getTimestamp() + "'" +
            ", level='" + getLevel() + "'" +
            ", action='" + getAction() + "'" +
            ", details='" + getDetails() + "'" +
            "}";
    }
}