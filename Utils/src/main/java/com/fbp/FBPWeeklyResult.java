package com.fbp;


import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
/*
    This class represents the results of a game for a given week, including the winner.
    It is used to get the results of the User's picks and determine how many
    are correct and how many are incorrect. It is also used to determine the winner of the tiebreaker.
    The tieBreaker is stored in the user's picks.  In the case of a tie,
    the user with the closest tieBreaker to the actual result of the tiebreaker game wins the tiebreaker.
    In the case of a tie in the tiebreaker, it's a tie.  Each player gets half of the points for that week. 

*/
@DynamoDbBean
public class FBPWeeklyResult {
    private String email;
    private String displayName;
    private double week;
    private Integer correctPicks;
    private Integer incorrectPicks;
    private Boolean winner;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("email")
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @DynamoDbAttribute("DisplayName")
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    @DynamoDbAttribute("CorrectPicks")
    public Integer getCorrectPicks() { return correctPicks; }
    public void setCorrectPicks(Integer correctPicks) { this.correctPicks = correctPicks; }

    @DynamoDbAttribute("IncorrectPicks")
    public Integer getIncorrectPicks() { return incorrectPicks; }
    public void setIncorrectPicks(Integer incorrectPicks) { this.incorrectPicks = incorrectPicks; }

    @DynamoDbSecondaryPartitionKey(indexNames = {"WeekIndex"})
    @DynamoDbAttribute("Week")
    public double getWeek() { return week; }
    public void setWeek(double week) { this.week = week; }

    @DynamoDbAttribute("Winner")
    public Boolean getWinner() { return winner; }
    public void setWinner(Boolean winner) { this.winner = winner; }
}
