package com.fbp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class GetWeeklyResults {
    public APIGatewayProxyResponseEvent getWeeklyResults(APIGatewayProxyRequestEvent request) throws JsonProcessingException {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        headers.put("Access-Control-Allow-Headers",
                "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");
        // Handle OPTIONS preflight request
        if ("OPTIONS".equals(request.getHttpMethod())) {
            response.setStatusCode(200);
            response.setBody("");
            response.setHeaders(headers);
            return response;
        }
        System.out.println("=== Starting getScheduleSheet() ===");
        Integer week = FBPUtils.getCurrentWeek();
        System.out.println("Determined week: " + week);

         if (week == null ){
             return new APIGatewayProxyResponseEvent()
                 .withStatusCode(400)
                 .withHeaders(headers)
                 .withBody(new ObjectMapper().writeValueAsString(Map.of("error", "Could not get week from FBPConfig table")));
        }


        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDbClient)
            .build();

        DynamoDbTable<FBPPicksResult> table =
            enhancedClient.table(System.getenv("FBPScheduleTableName"), TableSchema.fromClass(FBPPicksResult.class));
        try {
            System.out.println("Querying for schedule sheet for week: " + week);
            List<FBPPicksResult> picksRows = 
            table.query(QueryConditional.keyEqualTo(Key.builder().partitionValue(week).build()))
                .items()
                .stream()
                .collect(Collectors.toList());
            getPicksResultsForAllUsers(week, picksRows);
            System.out.println("Retrieved " + picksRows.size() + " items from DynamoDB for week " + week);

            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(headers)
                .withBody(new ObjectMapper().writeValueAsString(picksRows));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(500)
                .withHeaders(headers)
                .withBody(new ObjectMapper().writeValueAsString(Map.of("error", e.getMessage())));
        }
    }
    private void getPicksResultsForAllUsers(double week, List<FBPPicksResult> picksRows) throws JsonProcessingException {
        // This method will get the picks for all users for the given week and update the FBP-Users table with the results of the picks for each user.
        // You will need to implement this method to get the picks for all users and update the FBP-Users table with the results of the picks for each user.
        // This will be used to display the results of the picks for each user in the frontend.
        
        // Get all users from FBP-Users table
        ObjectMapper mapper = new ObjectMapper();
        DynamoDbClient dynamoDB = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDB).build();
        DynamoDbTable<FBPPicks> picksTable = enhancedClient.table(System.getenv("FBPPicksTableName"), TableSchema.fromClass(FBPPicks.class));
        List<FBPPicks> picksList = picksTable.scan().items().stream().collect(Collectors.toList());
        System.out.println("Retrieved " + picksList.size() + " users from FBP-Picks table");
        
        for (FBPPicks userPicks : picksList) {
            System.out.println("Processing picks for email: " + userPicks.getEmail());
            String userPicksStr = userPicks.getPicks();                         // This is the HA string
            System.out.println("User picks: " + userPicksStr);
            // Iterate through the picksRows and compare the user's picks to the actual results for each game
            // and determine how many picks are correct and how many are incorrect.
            Integer correctPicks = 0;
            Integer incorrectPicks = 0;
            Integer actualCorrectPicks = 0;
            Integer actualIncorrectPicks = 0;
            for (FBPPicksResult result : picksRows) {

                String[] picksArray = userPicksStr.split(",");
                for (String pickStr : picksArray) {
                    for(char c: pickStr.toCharArray()) {
                        // Check the result for Home and Away and compare with Winnder.
                        if(result.getHomeTeam().equals(result.getWinner()) && c == 'H') {
                            correctPicks += 1;
                        } else if(result.getAwayTeam().equals(result.getWinner()) && c == 'A') {
                            correctPicks += 1;
                        } else {
                            incorrectPicks += 1;
                        }
                    }
                }
                actualCorrectPicks = correctPicks;
                actualIncorrectPicks = incorrectPicks;
                correctPicks=0;
                incorrectPicks=0;                   
          } // End of picksRows loop
            System.out.println("User " + userPicks.getEmail() + " has " + actualCorrectPicks + " correct picks and " + actualIncorrectPicks + " incorrect picks for week " + week);
            // Update the FBP-Users table with the results of the picks for each user.
            // You will need to implement this logic to update the FBP-Users table with the results of the picks for each user.
             // For example, you could update the FBP-Users table with the following fields:
            // - email (string) - the email of the user
            // - week (double) - the week number- correctPicks (number) - the number of correct picks for the user for the given week
            // - incorrectPicks (number) - the number of incorrect picks for the user for the given week
            // - totalPicks (number) - the total number of picks for the user for the given week
            // - points (number) - the total points for the user for the given week (e.g. 1 point for each correct pick, 0 points for each incorrect pick)
             DynamoDbTable<FBPUser> userTable = enhancedClient.table(System.getenv("FBPUsersTableName"), TableSchema.fromClass(FBPUser.class));
             FBPUser user = userTable.getItem(Key.builder().partitionValue(userPicks.getEmail()).build());
             if(user == null) {
                 System.out.println("User not found in FBP-Users table for email: " + userPicks.getEmail());
                 continue;
            }
            // For the first week of the season, we will set the totalCorrectPicks and totalIncorrectPicks
            // to the actual correct and incorrect picks for the week.
            // For subsequent weeks, we will add the actual correct and incorrect picks
            // for the week to the existing totalCorrectPicks and totalIncorrectPicks for the user.
            if(week == 1) {
                user.setTotalCorrectPicks(actualCorrectPicks);
                user.setTotalIncorrectPicks(actualIncorrectPicks);
            } else {
                user.setTotalCorrectPicks(user.getTotalCorrectPicks() + actualCorrectPicks);
                user.setTotalIncorrectPicks(user.getTotalIncorrectPicks() + actualIncorrectPicks);
            }
            userTable.updateItem(user);
            // Set FBP-Weekly-Results table with the results for each user for the given week.
            DynamoDbTable<FBPWeeklyResult> weeklyResultTable = enhancedClient.table(System.getenv("FBPWeeklyResultsTableName"), TableSchema.fromClass(FBPWeeklyResult.class));
            FBPWeeklyResult weeklyResult = new FBPWeeklyResult();
            weeklyResult.setEmail(userPicks.getEmail());
            weeklyResult.setDisplayName(user.getDisplayName());
            weeklyResult.setWeek(week);
            weeklyResult.setCorrectPicks(actualCorrectPicks);
            weeklyResult.setIncorrectPicks(actualIncorrectPicks);
            weeklyResult.setWinner(false); // You will need to implement logic to determine the winner for the week and set this field accordingly.
            weeklyResultTable.putItem(weeklyResult);
            System.out.println("Updated FBP-Users table and FBP-Weekly-Results table for user: " + userPicks.getEmail());
        }
    }
}