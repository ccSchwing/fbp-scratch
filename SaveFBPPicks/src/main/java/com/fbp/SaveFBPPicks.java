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



public class SaveFBPPicks {
    public APIGatewayProxyResponseEvent saveFBPPicks(APIGatewayProxyRequestEvent request)
        throws JsonMappingException, JsonProcessingException {
        try{ 
        ObjectMapper objectMapper = new ObjectMapper();
        FBPPicks fbpPicks = objectMapper.readValue(request.getBody(), FBPPicks.class);
        DynamoDbClient dynamoDB = DynamoDbClient.builder().build();

        String tableName = System.getenv("FBPPicksTableName");

         PutItemRequest putItemRequest =
            PutItemRequest.builder()
                .tableName(tableName)
                .item(java.util.Map.of(
                    "email", AttributeValue.builder().s(fbpPicks.getEmail()).build(),
                    "picks", AttributeValue.builder().s(fbpPicks.getPicks()).build(),
                    "tieBreaker", AttributeValue.builder().s(fbpPicks.gettieBreaker()).build()
                ))
                .build();
        dynamoDB.putItem(putItemRequest);
        System.out.println("Picks saved: " + fbpPicks.getPicks());
        System.out.println("Table Name from ENV: " + tableName);
        return new APIGatewayProxyResponseEvent().withStatusCode(200)
            .withHeaders(Map.of(
                "Access-Control-Allow-Origin", "*",
                "Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token",
                "Access-Control-Allow-Methods", "POST,OPTIONS"
            ))
            .withBody("Picks saved successfully");
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
