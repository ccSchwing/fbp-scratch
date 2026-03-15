package com.fbp;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

public class SaveFBPPicks {
        public APIGatewayProxyResponseEvent saveFBPPicks(APIGatewayProxyRequestEvent request)
                        throws JsonMappingException, JsonProcessingException {
                APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
                Map<String, String> headers = new HashMap<>();
                headers.put("Access-Control-Allow-Origin", "*");
                headers.put("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
                headers.put("Access-Control-Allow-Headers",
                                "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");
                response.setHeaders(headers);

                // Handle OPTIONS preflight request
                if ("OPTIONS".equals(request.getHttpMethod())) {
                        response.setStatusCode(200);
                        response.setBody("");
                        return response;
                }
                System.out.println("=== Environment Variables ===");
                System.getenv().forEach((key, value) -> System.out.println(key + " = " + value));
                System.out.println("=============================");
                System.out.println("=============================");
                // ...existing code...
                String body = request.getBody();
                FBPPicks fbpPicks = new ObjectMapper().readValue(body, FBPPicks.class);
                Boolean b64 = request.getIsBase64Encoded();
                System.out.println("isBase64Encoded=" + b64);
                System.out.println("body length=" + (body == null ? 0 : body.length()));
                // Add CORS headers to ALL responses
                try {
                        String week = FBPUtils.getCurrentWeek();
                        if (week == null) {
                                response = new APIGatewayProxyResponseEvent();
                                response.setStatusCode(500);
                                response.setBody("{\"error\": \"Could not determine current week\"}");
                                response.setHeaders(headers);
                                return response;
                        }

                        // get DisplayName from FBPUser table based on email in FBPPicks
                        String displayName = FBPUtils.getDisplayName(fbpPicks.getEmail());
                        if (displayName == null) {
                                response = new APIGatewayProxyResponseEvent();
                                response.setStatusCode(500);
                                response.setBody("{\"error\": \"Could not determine display name for email: "
                                                + fbpPicks.getEmail() + "\"}");
                                response.setHeaders(headers);
                                return response;
                        }
                        // ObjectMapper objectMapper = new ObjectMapper();
                        // FBPPicks fbpPicks = objectMapper.readValue(request.getBody(),
                        // FBPPicks.class);
                        DynamoDbClient dynamoDB = DynamoDbClient.builder().build();

                        String tableName = System.getenv("FBPPicksTableName");
                        // Need the week number from config table to save picks against correct week
                        PutItemRequest putItemRequest = PutItemRequest.builder()
                                        .tableName(tableName)
                                        .item(java.util.Map.of(
                                                        "email",
                                                        AttributeValue.builder().s(fbpPicks.getEmail()).build(),
                                                        "picks",
                                                        AttributeValue.builder().s(fbpPicks.getPicks()).build(),
                                                        "tieBreaker",
                                                        AttributeValue.builder().s(fbpPicks.gettieBreaker()).build(),
                                                        "week", AttributeValue.builder().s(week).build(),
                                                        "displayName",
                                                        AttributeValue.builder().s(displayName).build()))
                                        .build();
                        dynamoDB.putItem(putItemRequest);
                        System.out.println("Picks saved: " + fbpPicks.getPicks());
                        System.out.println("Table Name from ENV: " + tableName);
                        FBPLogAction logCurrentAction = new FBPLogAction();
                        logCurrentAction.setWeek(week);
                        logCurrentAction.setEmail(fbpPicks.getEmail());
                        logCurrentAction.setAction("Save Picks");
                        logCurrentAction.setDetails("Picks saved successfully: " +
                                        fbpPicks.getPicks() +
                                        ":" +
                                        fbpPicks.gettieBreaker());
                        logCurrentAction.setLevel("INFO");
                        com.fbp.FBPUtils.logAction(logCurrentAction);
                        return new APIGatewayProxyResponseEvent().withStatusCode(200)
                                        .withHeaders(Map.of(
                                                        "Access-Control-Allow-Origin", "*",
                                                        "Access-Control-Allow-Headers",
                                                        "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token",
                                                        "Access-Control-Allow-Methods", "POST,OPTIONS"))
                                        .withBody("{\"message\": \"Picks saved successfully\"}");
                } catch (Exception e) {
                        // Call logging function here for error
                        FBPLogAction logCurrentAction = new FBPLogAction();
                        logCurrentAction.setWeek("unknown");
                        String sortKeyDate = ZonedDateTime.now(ZoneId.of("America/New_York")).toString();
                        logCurrentAction.setDate(new DateTime(sortKeyDate).toDate());
                        logCurrentAction.setEmail("unknown");
                        logCurrentAction.setAction("Error");
                        logCurrentAction.setDetails("Error processing: " + e.getMessage());
                        logCurrentAction.setLevel("ERROR");
                        com.fbp.FBPUtils.logAction(logCurrentAction);
                        return new APIGatewayProxyResponseEvent().withStatusCode(500)
                                        .withHeaders(Map.of(
                                                        "Access-Control-Allow-Origin", "*",
                                                        "Access-Control-Allow-Headers",
                                                        "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token",
                                                        "Access-Control-Allow-Methods", "POST,OPTIONS"))
                                        .withBody("{\"error\": \"Error processing order: "
                                                        + e.getMessage().replace("\"", "'") + "\"}");
                }
        }

}
