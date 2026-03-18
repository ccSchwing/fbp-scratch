package com.fbp;


import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;
/*
    This class represents the results of a game for a given week, including the winner.
    It is used to get the results of the User's picks and determine how many
    are correct and how many are incorrect. It is also used to determine the winner of the tiebreaker.
    The tieBreaker is stored in the user's picks.  In the case of a tie,
    the user with the closest tieBreaker to the actual result of the tiebreaker game wins the tiebreaker.
    In the case of a tie in the tiebreaker, it's a tie.  Each player gets half of the points for that week. 

*/
@DynamoDbBean
public class FBPPicksResult {
    private double week;
    private String gameId;
    private String awayTeam;
    private String homeTeam;
    private String winner;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("Week")
    public double getWeek() { return week; }
    public void setWeek(double week) { this.week = week; }

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

    @DynamoDbAttribute("Winner")
    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }
}
