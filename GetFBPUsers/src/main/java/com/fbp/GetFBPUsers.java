package com.fbp.aws.lambda.orders;

import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fbp.Order;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

public class GetFBPUsers {
    public APIGatewayProxyResponseEvent getOrders(APIGatewayProxyRequestEvent request)
            throws JsonMappingException, Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        // Order order = new Order(909, "MacBook Pro", 1);
        DynamoDbClient dynamoDB = DynamoDbClient.builder().build();
        ScanResponse scanResult = 
        dynamoDB.scan(ScanRequest.builder()
        .tableName(System.getenv("ORDERS_TABLE"))
        .build());
        List<Order> orders = scanResult
                .items().stream()
                .map(item -> new Order(
                        Integer.parseInt(item.get("id").n()),
                        item.get("itemName").s(),
                        Integer.parseInt(item.get("quantity").n())))
                .collect(Collectors.toList());
        String JsonOutput = objectMapper.writeValueAsString(orders);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(JsonOutput);
    }
}
