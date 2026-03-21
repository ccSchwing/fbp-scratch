package com.fbp;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class FBPConfig {
    private Integer week;
    private Boolean poolOpen;

    public FBPConfig() {}

    @DynamoDbPartitionKey
    @DynamoDbAttribute("Week")  // Map to the actual DynamoDB attribute name
    public Integer getWeek() {
        return week;
    }

    public void setWeek(Integer week) {
        this.week = week;
    }

    @DynamoDbAttribute("PoolOpen")
    public Boolean getPoolOpen() {
        return poolOpen;
    }

    public void setPoolOpen(Boolean poolOpen) {
        this.poolOpen = poolOpen;
    }

    @Override
    public String toString() {
        return "FBPConfig{week='" + week + "', poolOpen='" + poolOpen + "'}";
    }
}

