package com.fbp;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class GetFBPPicks {
    public APIGatewayProxyResponseEvent getFBPPicks(APIGatewayProxyRequestEvent request) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        FBPLogAction logEntry = new FBPLogAction();
        logEntry.setAction("GetFBPPicks");
        logEntry.setEmail("fbpadmin@my-fbp.com");

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
        try {
            // Parse JSON body for POST request
            ObjectMapper mapper = new ObjectMapper();
            String email = null;
            System.out.println("Request Body: " + request.getBody());
            // Add CORS headers
            if (request.getBody() != null && !request.getBody().isEmpty()) {
                JsonNode body = mapper.readTree(request.getBody());
                if (body.has("email")) {
                    email = body.get("email").asText();
                }
            }

            if (email == null || email.isEmpty()) {
                logEntry.setLevel("ERROR");
                logEntry.setDetails("Email is required in request body: " + request.getBody());
                FBPUtils.logAction(logEntry);
                response = new APIGatewayProxyResponseEvent();
                response.setStatusCode(400);
                response.setBody("{\"error\": \"Email is required in request body\"}");
                response.setHeaders(headers);
                return response;
            }
            Integer week = FBPUtils.getCurrentWeek();
            if(week == null) {
                logEntry.setLevel("ERROR");
                logEntry.setDetails("Could not determine current week");
                FBPUtils.logAction(logEntry);
                response = new APIGatewayProxyResponseEvent();
                response.setStatusCode(500);
                response.setBody("{\"error\": \"Could not determine current week\"}");
                response.setHeaders(headers);
                return response;
            }
            System.out
                    .println("Fetching FBPPicks for email: " + email + " and week: " + week); 
            // Your DynamoDB logic
            DynamoDbClient dynamoDB = DynamoDbClient.builder().build();
            DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient
                    .builder()
                    .dynamoDbClient(dynamoDB)
                    .build();

            String tableName = System.getenv("FBPPicksTableName");
            DynamoDbTable<FBPPicks> table = enhancedClient.table(tableName, TableSchema.fromBean(FBPPicks.class));

            Expression filterExpression = Expression.builder()
                    .expression("#wk = :week")
                    .putExpressionName("#wk", "week")
                    .putExpressionValue(":week", AttributeValue.builder().n(String.valueOf(week)).build())
                    .build();

            QueryEnhancedRequest queryRequest = QueryEnhancedRequest.builder()
                    .queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue(email).build()))
                    .filterExpression(filterExpression)
                    .build();

            FBPPicks fbpPicks = table.query(queryRequest).items().stream().findFirst().orElse(null);
            if (fbpPicks == null) {
                response = new APIGatewayProxyResponseEvent();
                response.setStatusCode(207);
                System.out.println("No FBPPicks found for email: " + email + " and week: " + week);
                logEntry.setLevel("ERROR");
                logEntry.setDetails("No FBPPicks found for email: " + email + " and week: " + week);
                FBPUtils.logAction(logEntry);
                response.setBody("{\"success\": \"No picks found for email: " + email + " and week: " + week + "\"}");
                response.setHeaders(headers);
                return response;
            }else {
                System.out.println("FBPPicks retrieved: " + fbpPicks.toString());
            }
            logEntry.setLevel("INFO");
            logEntry.setDetails("Retrieved picks: " + fbpPicks.getPicks() + 
                ":" + fbpPicks.getTieBreaker() + 
                "-" + fbpPicks.getDisplayName() + "for email:" + email + " and week:" + week);
            FBPUtils.logAction(logEntry);
            response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(200);
            response.setBody(mapper.writeValueAsString(fbpPicks));
            response.setHeaders(headers);
            return response;

        } catch (Exception e) {
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
