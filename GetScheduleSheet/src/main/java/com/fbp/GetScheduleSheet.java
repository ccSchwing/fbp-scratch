package com.fbp;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;


public class GetScheduleSheet {
    public APIGatewayProxyResponseEvent getScheduleSheet(APIGatewayProxyRequestEvent request) throws JsonProcessingException {
        String week = null;
        if (request != null && request.getQueryStringParameters() != null) {
            week = request.getQueryStringParameters().get("Week");
        }
        if ((week == null || week.isBlank()) && request != null && request.getBody() != null && !request.getBody().isBlank()) {
            try {
                Map<String, String> body = new ObjectMapper().readValue(request.getBody(), new TypeReference<Map<String, String>>() {
                });
                if (body != null) {
                    week = body.get("Week");
                }
            } catch (Exception e) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withHeaders(Map.of(
                        "Access-Control-Allow-Origin", "https://my-fbp.com",
                        "Content-Type", "application/json"))
                    .withBody(new ObjectMapper().writeValueAsString(Map.of("error", "Failed to parse request body: " + e.getMessage())));
            }
        }
        if (week == null || week.isBlank()) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(400)
                .withHeaders(Map.of(
                    "Access-Control-Allow-Origin", "https://my-fbp.com",
                    "Content-Type", "application/json"))
                .withBody(new ObjectMapper().writeValueAsString(Map.of("error", "Missing required query parameter: week")));
        }

        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
            .dynamoDbClient(dynamoDbClient)
            .build();

        DynamoDbTable<FBPPickSheet> table =
            enhancedClient.table(System.getenv("FBPScheduleTableName"), TableSchema.fromClass(FBPPickSheet.class));
        try {
            List<FBPPickSheet> pickSheets = table.query(QueryConditional.keyEqualTo(Key.builder().partitionValue(week).build()))
                .items()
                .stream()
                .collect(Collectors.toList());
            
            if (pickSheets == null || pickSheets.isEmpty()) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(404)
                    .withHeaders(Map.of(
                        "Access-Control-Allow-Origin", "https://my-fbp.com",
                        "Content-Type", "application/json"))
                    .withBody(new ObjectMapper().writeValueAsString(Map.of("error", "No pick sheet found for week " + week)));
            }
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(Map.of(
                    "Access-Control-Allow-Origin", "https://my-fbp.com",
                    "Content-Type", "application/json"))
                .withBody(new ObjectMapper().writeValueAsString(pickSheets));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(500)
                .withHeaders(Map.of(
                    "Access-Control-Allow-Origin", "https://my-fbp.com",
                    "Content-Type", "application/json"))
                .withBody(new ObjectMapper().writeValueAsString(Map.of("error", e.getMessage())));
        }
    }
}