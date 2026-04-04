package com.fbp;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

public class GetAllFBPPicks {
    public APIGatewayProxyResponseEvent getAllFBPPicks(APIGatewayProxyRequestEvent request) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
        headers.put("Access-Control-Allow-Headers",
                "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");
        // Handle OPTIONS preflight request
        FBPLogAction logEntry = new FBPLogAction();
        if ("OPTIONS".equals(request.getHttpMethod())) {
            response.setStatusCode(200);
            response.setBody("");
            response.setHeaders(headers);
            return response;
        }
        try {
            logEntry.setAction("GetAllFBPPicks");
            logEntry.setEmail("fbpadmin@my-fbp.com");
            Integer week = FBPUtils.getCurrentWeek();
            if (week == null) {
                logEntry.setLevel("ERROR");
                logEntry.setDetails("Could not determine current week");
                FBPUtils.logAction(logEntry);
                response = new APIGatewayProxyResponseEvent();
                response.setStatusCode(500);
                response.setBody("{\"error\": \"Could not determine current week\"}");
                response.setHeaders(headers);
                return response;
            }
            logEntry.setWeek(week.toString());
            // Parse JSON body for POST request
            ObjectMapper mapper = new ObjectMapper();

            System.out.println("Current week: " + week);
            // Your DynamoDB logic
            DynamoDbClient dynamoDB = DynamoDbClient.builder().build();
            DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient
                    .builder()
                    .dynamoDbClient(dynamoDB)
                    .build();

            String tableName = System.getenv("FBPPicksTableName");
            DynamoDbTable<FBPPicks> table = enhancedClient.table(tableName, TableSchema.fromBean(FBPPicks.class));

            Expression filterExpression = Expression.builder()
                    .expression("week = :weekValue")
                    .expressionValues(Map.of(":weekValue", AttributeValue.builder().n(week.toString()).build()))
                    .build();

            ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                    .filterExpression(filterExpression)
                    .build();

            List<FBPPicks> fbpPicks = table.scan(scanRequest)
                    .items()
                    .stream()
                    .sorted(Comparator.comparing(FBPPicks::getDisplayName,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .collect(Collectors.toList());

            System.out.println("FBPPicks size: " + fbpPicks.size());
            if (fbpPicks == null || fbpPicks.isEmpty()) {
                logEntry = new FBPLogAction();
                logEntry.setAction("GetAllFBPPicks");
                logEntry.setEmail("fbpadmin@my-fbp.com");
                logEntry.setLevel("ERROR");
                logEntry.setDetails("No picks found for week: " + week);
                FBPUtils.logAction(logEntry);
                response = new APIGatewayProxyResponseEvent();
                response.setStatusCode(400);
                System.out.println("No FBPPicks found for week: " + week);
                response.setBody("{\"error\": \"No picks found for week: " + week + "\"}");
                response.setHeaders(headers);
                return response;
            } else {
                System.out.println("FBPPicks retrieved: " + fbpPicks.toString());
            }
            logEntry.setLevel("INFO");
            logEntry.setDetails("Retrieved " + fbpPicks.size() + " picks for week: " + week);
            FBPUtils.logAction(logEntry);
            response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(200);
            response.setBody(mapper.writeValueAsString(fbpPicks));
            response.setHeaders(headers);
            return response;

        } catch (Exception e) {
            logEntry = new FBPLogAction();
            logEntry.setAction("GetAllFBPPicks");
            logEntry.setEmail("fbpadmin@my-fbp.com");
            logEntry.setLevel("ERROR");
            logEntry.setDetails("Exception occurred: " + e.getMessage());
            FBPUtils.logAction(logEntry);
            response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(500);
            response.setBody("{\"error\": \"" + e.getMessage() + "\"}");
            response.setHeaders(headers);
            return response;
        }

    }
}
