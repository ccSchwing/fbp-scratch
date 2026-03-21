package com.fbp;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.List;
public class GetWeeklyResultsSheet {

    private static final Map<String, String> headers = Map.of("Content-Type", "application/json",
            "Access-Control-Allow-Origin", "*",
            "Access-Control-Allow-Methods", "GET, POST, OPTIONS",
            "Access-Control-Allow-Headers", "Content-Type,Authorization");
    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(headers);
    ObjectMapper objectMapper = new ObjectMapper();

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize response body", e);
        }
    }

   public APIGatewayProxyResponseEvent getWeeklyResultsSheet(APIGatewayProxyRequestEvent request) {
       // This function will be called by a scheduled event every week to create the weekly result sheet.
       // It will get the schedule for the week, get the picks for the week, get the results for the week,
       // and then create the weekly result sheet in DynamoDB.
        System.out.println("=== Starting getWeeklyResultsSheet() ===");
        Integer week = FBPUtils.getCurrentWeek();
        System.out.println("Determined week: " + week);

         if (week == null){ 
             return new APIGatewayProxyResponseEvent()
                 .withStatusCode(400)
                 .withHeaders(headers)
                 .withBody(toJson(Map.of("error", "Could not get week from FBPConfig table")));
        }

        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDbClient)
            .build();

        DynamoDbTable<FBPWeeklyResult> table =
            enhancedClient.table(System.getenv("FBPWeeklyResultsTableName"), TableSchema.fromClass(FBPWeeklyResult.class));
        try {
            System.out.println("Fetching weekly result sheet for week: " + week);
            String weekIndexName = System.getenv().getOrDefault("FBPWeeklyResultsWeekIndexName", "WeekIndex");
            DynamoDbIndex<FBPWeeklyResult> weekIndex = table.index(weekIndexName);

            QueryEnhancedRequest weekQuery = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue(week).build()))
                .build();

            List<FBPWeeklyResult> weeklyResultRows = weekIndex.query(weekQuery)
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
            if (weeklyResultRows != null && !weeklyResultRows.isEmpty()) {
                System.out.println("Fetched weekly result sheet for week: " + week);
                System.out.print("Number of rows of Picks:" + weeklyResultRows.size());
                List<FBPWeeklyResult> sortedRows = weeklyResultRows.stream()
                    .sorted(Comparator.comparingInt(FBPWeeklyResult::getCorrectPicks)
                        .reversed()
                        .thenComparing(FBPWeeklyResult::getIncorrectPicks)
                        .thenComparing(FBPWeeklyResult::getDisplayName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(FBPWeeklyResult::getEmail, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());
                String sortedRowsJSON = toJson(sortedRows);
                return response.withStatusCode(200).withBody(sortedRowsJSON);
            } else {
                System.out.println("No weekly result sheet found for week: " + week);
                return response.withStatusCode(404).withBody("No weekly result sheet found for week: " + week);
            }
            }
        catch (RuntimeException e) {
            System.out.println("Error fetching weekly result sheet for week: " + week);
            System.err.println(e.getMessage());
            return response.withStatusCode(400).withBody("Error fetching weekly result sheet for week: " + week);
        }
   } 

}
