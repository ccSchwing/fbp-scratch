package com.fbp;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;



public class AddFBPUser {
    public APIGatewayProxyResponseEvent createFBPUser(APIGatewayProxyRequestEvent request)
        throws JsonMappingException, JsonProcessingException {
        try{ 
        ObjectMapper objectMapper = new ObjectMapper();
        Utils.FBPUser fbpUser = objectMapper.readValue(request.getBody(), FBPUser.class);
        DynamoDbClient dynamoDB = DynamoDbClient.builder().build();

        String tableName = System.getenv("FBPUsers");
        PutItemRequest putItemRequest =
            PutItemRequest.builder()
                .tableName(tableName)
                .item(java.util.Map.of(
                    "firstName", AttributeValue.builder().s(fbpUser.firstName).build(),
                    "lastName", AttributeValue.builder().s(fbpUser.lastName).build(),
                    "email", AttributeValue.builder().s(fbpUser.email).build(),
                    "displayName", AttributeValue.builder().s(fbpUser.displayName).build()
                ))
                .build();
        dynamoDB.putItem(putItemRequest);
        System.out.println("User created: " + fbpUser.displayName);
        System.out.println("Table Name from ENV: " + tableName);
        String responseMessage = String.format("FBP User created: FirstName=%s, LastName=%s, Email=%s, DisplayName=%s", fbpUser.firstName, fbpUser.lastName, fbpUser.email, fbpUser.displayName);
        return new APIGatewayProxyResponseEvent().withStatusCode(200)
            .withHeaders(Map.of(
                "Access-Control-Allow-Origin", "*",
                "Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token",
                "Access-Control-Allow-Methods", "POST,OPTIONS"
            ))
            .withBody(responseMessage);
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent().withStatusCode(500)
                .withHeaders(Map.of(
                    "Access-Control-Allow-Origin", "*",
                    "Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token",
                    "Access-Control-Allow-Methods", "POST,OPTIONS"
                ))
                .withBody("Error processing order: " + e.getMessage());
        }
    }
}
