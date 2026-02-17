package com.fbp;

import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

public class GetFBPUser {
    public APIGatewayProxyResponseEvent getFBPUser(APIGatewayProxyRequestEvent request)
            throws JsonMappingException, Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        DynamoDbClient dynamoDB = DynamoDbClient.builder().build();
        ScanResponse scanResult = 
        dynamoDB.scan(ScanRequest.builder()
                .tableName(System.getenv("FBPUsers"))
                .build());
        List<SingleFBPUser> fbpUserList = scanResult
                .items().stream()
                .map((item) -> objectMapper.convertValue(item, SingleFBPUser.class))
                .collect(Collectors.toList());
        String JsonOutput = objectMapper.writeValueAsString(fbpUserList);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(JsonOutput);
    }
}
