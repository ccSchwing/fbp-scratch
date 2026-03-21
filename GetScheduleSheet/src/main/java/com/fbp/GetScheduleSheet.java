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

public class GetScheduleSheet {
    public APIGatewayProxyResponseEvent getScheduleSheet(APIGatewayProxyRequestEvent request)
            throws JsonProcessingException {
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

        if (week == null) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withHeaders(headers)
                    .withBody(new ObjectMapper()
                            .writeValueAsString(Map.of("error", "Could not get week from FBPConfig table")));
        }

        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();

        DynamoDbTable<FBPScheduleRow> table = enhancedClient.table(System.getenv("FBPScheduleTableName"),
                TableSchema.fromClass(FBPScheduleRow.class));
        try {
            System.out.println("Querying for schedule sheet for week: " + week);
            List<FBPScheduleRow> scheduleRows = table.query(QueryConditional.keyEqualTo(Key.builder()
                    .partitionValue(week)
                    .build()))
                    .stream()
                    .flatMap(page -> page.items().stream())
                    .collect(Collectors.toList());

            if (scheduleRows == null || scheduleRows.isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404)
                        .withHeaders(headers)
                        .withBody(new ObjectMapper()
                                .writeValueAsString(Map.of("error", "No schedule found for week " + week)));
            }
            /*
             * This is where you will calculate winners and losers for the week
             * and update the DB.
             * Each schedule row has the following fields:
             * - week (double)
             * - gameId (string)
             * - awayTeam (string)
             * - homeTeam (string)
             * - homeScore (Number) - the final score for the home team
             * - awayScore (Number) - the final score for the away team
             * - date (string)
             * - spread (Number) - the point spread for the game, with the favorite team and
             * points (e.g. "NE -3.5")
             * - finalWithSpread (Number) - the final score of the game with the spread
             * applied (e.g. "NE 24, MIA 20 (-3.5)")
             * - underdog (string) - the underdog team for the game
             */
            for (FBPScheduleRow row : scheduleRows) {
                System.out.println("Processing game: " + row.getGameId());
                System.out.println("Home Team: " + row.getHomeTeam() + ", Away Team: " + row.getAwayTeam());
                // Here you would add logic to determine the winner and loser based on the final
                // scores and spread
                // For example:
                // double homeScore = Double.parseDouble(row.getHomeScore());
                // double awayScore = Double.parseDouble(row.getAwayScore());
                // double spread = Double.parseDouble(row.getSpread().split(" ")[1]);
                // double spread = Double.parseDouble(row.getSpread());
                // String underDog = row.getUnderdog();
                // String winner;
                // if (underDog.equals(row.getHomeTeam())) {
                // if (homeScore + spread > awayScore) {
                // winner = row.getHomeTeam();
                // } else {
                // winner = row.getAwayTeam();
                // }
                // } else {
                // if (awayScore + spread > homeScore) {
                // winner = row.getAwayTeam();
                // } else {
                // winner = row.getHomeTeam();
                // }
                // }
                // System.out.println("Winner: " + winner);
                // You would then update the schedule row in the DB with the winner/loser
                // information
                FBPScheduleRow updatedRow = calculateWinnerLoser(row);
                table.updateItem(updatedRow);
            }
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(new ObjectMapper().writeValueAsString(scheduleRows));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(headers)
                    .withBody(new ObjectMapper().writeValueAsString(Map.of("error", e.getMessage())));
        }
    }

    private FBPScheduleRow calculateWinnerLoser(FBPScheduleRow row) {
        // Here you would add logic to determine the winner and loser based on the final
        // scores and spread
        // For example:
        double homeScore = row.getHomeScore();
        double awayScore = row.getAwayScore();
        // double spread = Double.parseDouble(row.getSpread().split(" ")[1]);
        double spread = row.getSpread();
        String underDog = row.getUnderdog();
        String winner;
        if (underDog.equals(row.getHomeTeam())) {
            if (homeScore + spread > awayScore) {
                winner = row.getHomeTeam();
            } else {
                winner = row.getAwayTeam();
            }
        } else {
            if (awayScore + spread > homeScore) {
                winner = row.getAwayTeam();
            } else {
                winner = row.getHomeTeam();
            }
        }
        System.out.println("Winner: " + winner);
        // You would then update the schedule row in the DB with the winner/loser
        // information
        row.setWinner(winner);
        return row;
    }

}
