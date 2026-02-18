package com.fbp;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class GetFBPUsers {
    public APIGatewayProxyResponseEvent getFBPUser(APIGatewayProxyRequestEvent request)
            throws JsonMappingException, Exception {
        // ObjectMapper objectMapper = new ObjectMapper();
        DynamoDbClient dynamoDB = DynamoDbClient.builder().build();
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient
                .builder()
                .dynamoDbClient(dynamoDB)
                .build();
        DynamoDbTable<FBPUserBean> table = enhancedClient.table("FBPUsers", TableSchema.fromBean(FBPUserBean.class));
        String email = request.getQueryStringParameters().get("email");
        FBPUserBean fbpUser=table.getItem(Key.builder().partitionValue(email).build());
        // DynamoDbClient dynamoDB = DynamoDbClient.builder().build();
        // ScanResponse scanResult = 
        // dynamoDB.scan(ScanRequest.builder()
        //         .tableName(System.getenv("FBPUsers"))
        //         .build());
        // List<FBPUser> fbpUserList = scanResult
        //         .items().stream()
        //         .map((item) -> objectMapper.convertValue(item, FBPUser.class))
        //         .collect(Collectors.toList());
        // String JsonOutput = objectMapper.writeValueAsString(fbpUserList);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                    .withHeaders(Map.of(
                        "Access-Control-Allow-Origin", "https://my-fbp.com",
                        "Content-Type", "application/json"))
                        .withBody(new ObjectMapper().writeValueAsString(fbpUser));
    }
}
