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
        String week = FBPUtils.getCurrentWeek();
        System.out.println("Determined week: " + week);

         if (week == null || week.isBlank()) {
             return new APIGatewayProxyResponseEvent()
                 .withStatusCode(400)
                 .withHeaders(headers)
                 .withBody(new ObjectMapper().writeValueAsString(Map.of("error", "Could not get week from FBPConfig table")));
        }

        final double weekNumber;
        try {
            weekNumber = Double.parseDouble(week);
        } catch (NumberFormatException e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withHeaders(headers)
                .withBody(new ObjectMapper().writeValueAsString(Map.of("error", "Invalid week format: " + week)));
        }

        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDbClient)
            .build();

        DynamoDbTable<FBPPicksResult> table =
            enhancedClient.table(System.getenv("FBPScheduleTableName"), TableSchema.fromClass(FBPPicksResult.class));
        try {
            System.out.println("Querying for schedule sheet for week: " + week);
            List<FBPPicksResult> picksRows = table.query(QueryConditional.keyEqualTo(Key.builder().partitionValue(weekNumber).build()))
                .items()
                .stream()
                .collect(Collectors.toList());
            
            if (picksRows == null || picksRows.isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(404)
                    .withHeaders(headers)
                    .withBody(new ObjectMapper().writeValueAsString(Map.of("error", "No schedule found for week " + week)));
            }
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
}