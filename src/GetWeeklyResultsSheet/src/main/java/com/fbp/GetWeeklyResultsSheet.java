package com.fbp;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/*
This class retrieves the weekly results for each user based on their picks and the actual game results for the week.
It queries the FBPWeeklyResults table for the current week and returns the results sorted by the
number of correct picks. It also updates the winner field for the user with the most correct picks.
This is used by the front end to display the weekly results sheet for each user.
*/
public class GetWeeklyResultsSheet {
    private static final Map<String, String> headers = Map.of(
            "Content-Type", "application/json",
            "Access-Control-Allow-Origin", "*",
            "Access-Control-Allow-Methods", "GET, POST, OPTIONS",
            "Access-Control-Allow-Headers", "Content-Type,Authorization");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize response body", e);
        }
    }

    public APIGatewayProxyResponseEvent getWeeklyResultsSheet(APIGatewayProxyRequestEvent request) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(headers);
        // Handle OPTIONS preflight request
        if ("OPTIONS".equals(request.getHttpMethod())) {
            return response.withStatusCode(200).withBody("");
        }
        FBPLogAction logEntry = new FBPLogAction();
        logEntry.setEmail("fbpadmin@my-fbp.com");
        logEntry.setAction("GetWeeklyResultsSheet");

        System.out.println("=== Starting getWeeklyResultsSheet() ===");
        Integer week = FBPUtils.getCurrentWeek();
        if (week == null) {
            logEntry.setLevel("ERROR");
            logEntry.setDetails("Could not get week from FBPConfig table");
            FBPUtils.logAction(logEntry);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withHeaders(headers)
                    .withBody(toJson(Map.of("error", "Could not get week from FBPConfig table")));
        }
        System.out.println("week: " + week);

        logEntry.setWeek(week.toString());
        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();

        String tableName = System.getenv("FBPWeeklyResultsTableName");
        System.out.println("Querying DynamoDB table: " + tableName + " for week: " + week);
        DynamoDbTable<FBPWeeklyResult> table = enhancedClient.table(tableName,
                TableSchema.fromClass(FBPWeeklyResult.class));

        try {
            System.out.println("Querying weekly results for week: " + week);
            String weekIndexName = System.getenv().getOrDefault("FBPWeeklyResultsWeekIndexName", "WeekIndex");
            DynamoDbIndex<FBPWeeklyResult> weekIndex = table.index(weekIndexName);

            QueryEnhancedRequest weekQuery = QueryEnhancedRequest.builder()
                    .queryConditional(QueryConditional.keyEqualTo(
                            Key.builder().partitionValue(week.doubleValue()).build()))
                    .build();

            List<FBPWeeklyResult> picksRows = weekIndex.query(weekQuery)
                    .stream()
                    .flatMap(page -> page.items().stream())
                    .sorted(Comparator.comparingInt(FBPWeeklyResult::getCorrectPicks).reversed())
                    .collect(Collectors.toList());
            // Set the winner field for top player.
            if (!picksRows.isEmpty()) {
                picksRows.get(0).setWinner(true);
            }

            // Copy to S3 bucket for front end to access.
            FBPUtils.copyToS3(toJson(picksRows), "fbp-admin/weekly-results-sheet.json");
            // Update the winner field in DynamoDB for the top player.
            for (FBPWeeklyResult result : picksRows) {
                if (result.getWinner() != null && result.getWinner()) {
                    table.updateItem(result);
                    break; // Only one winner, so we can break after updating the first one.
                }
            }

            System.out.println("Retrieved " + picksRows.size() + " items from DynamoDB for week " + week);
            logEntry.setLevel("INFO");
            logEntry.setDetails("Successfully retrieved weekly results for week: " + week);
            FBPUtils.logAction(logEntry);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withHeaders(headers)
                    .withBody(toJson(picksRows));
        } catch (Exception e) {
            logEntry.setLevel("ERROR");
            logEntry.setDetails("Exception occurred: " + e.getMessage());
            FBPUtils.logAction(logEntry);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withHeaders(headers)
                    .withBody(toJson(Map.of("error", e.getMessage())));
        }
    }
}
