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
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class GetPickSheet {
    public APIGatewayProxyResponseEvent getPickSheet(APIGatewayProxyRequestEvent request)
            throws JsonProcessingException {

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        headers.put("Access-Control-Allow-Headers",
                "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");
        String week = getCurrentWeek();

        if (week == null || week.isBlank()) {
            response.setHeaders(headers);
            response.setStatusCode(400);
            response.setBody("{\"error\": \"Could not determine the current week\"}");
            return response;
        }

        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();

        DynamoDbTable<FBPPickSheet> table = enhancedClient.table(System.getenv("FBPScheduleTableName"),
                TableSchema.fromClass(FBPPickSheet.class));
        try {
            List<FBPPickSheet> pickSheets = table
                    .query(QueryConditional.keyEqualTo(Key.builder().partitionValue(Double.parseDouble(week)).build()))
                    .items()
                    .stream()
                    .collect(Collectors.toList());

            if (pickSheets == null || pickSheets.isEmpty()) {
                response.setHeaders(headers);
                response.setStatusCode(404);
                response.setBody("{\"error\": \"No pick sheet found for week " + week + "\"}");
                return response;
            }
            response.setStatusCode(200);
            response.setHeaders(headers);
            response.setBody(new ObjectMapper().writeValueAsString(pickSheets));
            return response;
        } catch (Exception e) {
            response.setHeaders(headers);
            response.setStatusCode(500);
            response.setBody("{\"error\": \"" + e.getMessage() + "\"}");
            return response;
        }
    }


    public String getCurrentWeek() {
        System.out.println("=== Starting getCurrentWeek() ===");

        // Check environment variable first
        String tableName = System.getenv("FBPConfigTableName");
        System.out.println("Environment variable FBPConfigTableName: " + tableName);

        if (tableName == null || tableName.isEmpty()) {
            System.err.println("ERROR: Environment variable FBPConfigTableName is not set or empty");
            return null;
        }

        try {
            System.out.println("Creating DynamoDB client...");
            DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
            DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(dynamoDbClient)
                    .build();

            System.out.println("Creating table reference...");
            DynamoDbTable<FBPConfig> table = enhancedClient.table(tableName, TableSchema.fromBean(FBPConfig.class));

            System.out.println("Starting table scan...");
            PageIterable<FBPConfig> configPages = table.scan();

            System.out.println("Iterating through scan results...");
            int itemCount = 0;
            for (FBPConfig config : configPages.items()) {
                itemCount++;
                System.out.println("Found item #" + itemCount);

                if (config != null) {
                    String week = config.getWeek();
                    System.out.println("Week value: " + week);
                    return week;
                } else {
                    System.out.println("Config object is null");
                    return null;
                }
            }

            System.out.println("Total items found: " + itemCount);
            if (itemCount == 0) {
                System.out.println("No items found in table - table might be empty");
                return null;
            }

        } catch (Exception e) {
            System.err
                    .println("EXCEPTION in getCurrentWeek(): " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        System.out.println("Reached end of function - returning null");
        return null;
    }
}