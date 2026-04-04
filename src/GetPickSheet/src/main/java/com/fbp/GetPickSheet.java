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

/*
 * This class is responsible for retrieving the Schedule sheet for the current week.
 * The schedule sheet contains the list of games for the current week.
 * The pick sheet is returned as a JSON object.
 * This is where the users will get the list of games to make their picks for the week.
  */
public class GetPickSheet {
    public APIGatewayProxyResponseEvent getPickSheet(APIGatewayProxyRequestEvent request)
            throws JsonProcessingException {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Content-type", "application/json");
        headers.put("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");
        FBPLogAction logEntry = new FBPLogAction();
        logEntry.setAction("GetPickSheet");
        logEntry.setEmail("fbpadmin@my-fbp.com");
        Integer week = FBPUtils.getCurrentWeek();
        if (week == null) {
            logEntry.setLevel("ERROR");
            logEntry.setDetails("Could not determine the current week");
            logEntry.setWeek(week != null ? week.toString() : "N/A");
            FBPUtils.logAction(logEntry);
            response.setHeaders(headers);
            response.setStatusCode(400);
            response.setBody("{\"error\": \"Could not determine the current week\"}");
            return response;
        }
        logEntry.setWeek(week.toString());

        DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();

        DynamoDbTable<FBPPickSheet> table = enhancedClient.table(System.getenv("FBPScheduleTableName"),
                TableSchema.fromClass(FBPPickSheet.class));
        try {
            List<FBPPickSheet> pickSheets = table
                    .query(QueryConditional.keyEqualTo(Key.builder().partitionValue(week).build()))
                    .items()
                    .stream()
                    .collect(Collectors.toList());

            if (pickSheets == null || pickSheets.isEmpty()) {
                logEntry.setLevel("ERROR");
                logEntry.setDetails("No pick sheet found for week " + week);
                logEntry.setWeek(week.toString());
                FBPUtils.logAction(logEntry);
                response.setHeaders(headers);
                response.setStatusCode(404);
                response.setBody("{\"error\": \"No pick sheet found for week " + week + "\"}");
                return response;
            }
            response.setStatusCode(200);
            response.setHeaders(headers);
            response.setBody(new ObjectMapper().writeValueAsString(pickSheets));
            logEntry.setLevel("INFO");
            logEntry.setDetails("Successfully retrieved pick sheet for week " + week);
            logEntry.setWeek(week.toString());
            FBPUtils.logAction(logEntry);

            return response;
        } catch (Exception e) {
            response.setHeaders(headers);
            response.setStatusCode(500);
            response.setBody("{\"error\": \"" + e.getMessage() + "\"}");
            logEntry.setLevel("ERROR");
            logEntry.setDetails("Error retrieving pick sheet for week " + week + ": " + e.getMessage());
            logEntry.setWeek(week.toString());
            FBPUtils.logAction(logEntry);
            return response;
        }
    }
}