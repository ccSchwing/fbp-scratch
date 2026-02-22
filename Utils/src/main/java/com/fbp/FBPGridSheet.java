package com.fbp;


import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class FBPGridSheet {
    private String week;
    private String gameId;
    private String awayTeam;
    private String homeTeam;
    private String date;
    private String spread;
    private String finalWithSpread;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("Week")
    public String getWeek() { return week; }
    public void setWeek(String week) { this.week = week; }

    @DynamoDbSortKey
    @DynamoDbAttribute("GameId")
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    @DynamoDbAttribute("Away")
    public String getAwayTeam() { return awayTeam; }
    public void setAwayTeam(String awayTeam) { this.awayTeam = awayTeam; }
    
    @DynamoDbAttribute("Home")
    public String getHomeTeam() { return homeTeam; }
    public void setHomeTeam(String homeTeam) { this.homeTeam = homeTeam; }

    @DynamoDbAttribute("Date")
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    @DynamoDbAttribute("FinalWithSpread")
    public String getFinalWithSpread() { return finalWithSpread; }
    public void setFinalWithSpread(String finalWithSpread) { this.finalWithSpread = finalWithSpread; }

    @DynamoDbAttribute("Spread")
    public String getSpread() { return spread; }
    public void setSpread(String spread) { this.spread = spread; }
}
