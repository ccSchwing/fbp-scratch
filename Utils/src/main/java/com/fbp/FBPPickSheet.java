package com.fbp;

import com.fasterxml.jackson.annotation.JsonProperty;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class FBPPickSheet {
    private String week;
    private String gameId;
    private String awayTeam;
    private String homeTeam;
    private String date;
    private String underdog;
    private String spread;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("Week")
    @JsonProperty("Week")
    public String getWeek() { return week; }
    public void setWeek(String week) { this.week = week; }

    @DynamoDbSortKey
    @DynamoDbAttribute("GameId")
    @JsonProperty("GameId")
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    @DynamoDbAttribute("Away")
    @JsonProperty("Away")
    public String getAwayTeam() { return awayTeam; }
    public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }
    
    @DynamoDbAttribute("Home")
    @JsonProperty("Home")
    public String getHomeTeam() { return homeTeam; }
    public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }

    @DynamoDbAttribute("Date")
    @JsonProperty("Date")
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    @DynamoDbAttribute("Underdog")
    @JsonProperty("Underdog")
    public String getUnderdog() { return underdog; }
    public void setUnderdog(String underdog) { this.underdog = underdog; }

    @DynamoDbAttribute("Spread")
    @JsonProperty("Spread")
    public String getSpread() { return spread; }
    public void setSpread(String spread) { this.spread = spread; }
}
