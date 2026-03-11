package com.fbp;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class GetFBPUser {
       public APIGatewayProxyResponseEvent getFBPUser(APIGatewayProxyRequestEvent request) {
            Map<String, String> headers = new HashMap<>();
            headers.put("Access-Control-Allow-Origin", "*");
            headers.put("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");
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
                APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
                response.setStatusCode(400);
                response.setBody("{\"error\": \"Email is required in request body\"}");
                response.setHeaders(headers);
                return response;
            }
            
            // Your DynamoDB logic
            DynamoDbClient dynamoDB = DynamoDbClient.builder().build();
            DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient
                    .builder()
                    .dynamoDbClient(dynamoDB)
                    .build();
            
            String tableName = System.getenv("FBPUsersTableName");
            DynamoDbTable<FBPUser> table = enhancedClient.table(tableName, TableSchema.fromBean(FBPUser.class));
            FBPUser fbpUser=table.getItem(Key.builder().partitionValue(email).build());
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(200);
            response.setBody(mapper.writeValueAsString(fbpUser));
            response.setHeaders(headers);
            return response;
            
        } catch (Exception e) {
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(500);
            response.setBody("{\"error\": \"" + e.getMessage() + "\"}");
            response.setHeaders(headers);
            return response;
        }

    }
}
