package com.fbp;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;


import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FBPUtils {
    public static Integer getCurrentWeek() {
        System.out.println("=== Starting getCurrentWeek() ===");

        // Check environment variable first
        String tableName = System
                .getenv("FBPConfigTableName");
        System.out.println("Environment variable FBPConfigTableName: " + tableName);

        if (tableName == null || tableName.isEmpty()) {
            System.err.println("ERROR: Environment variable FBPConfigTableName is not set or empty");
            return null;
        }

        try {
            System.out.println("Creating DynamoDB clients...");
            DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
            DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(dynamoDbClient)
                    .build();

            System.out.println("Creating table reference...");
            DynamoDbTable<FBPConfig> table = enhancedClient.table(tableName, TableSchema.fromBean(FBPConfig.class));

            System.out.println("Starting table scan...");
            PageIterable<FBPConfig> configPages = table.scan();

            System.out.println("Iterating through scan results...");
            int itemCount = 0;
            for (FBPConfig config : configPages.items()) {
                itemCount++;
                System.out.println("Found item #" + itemCount);

                if (config != null) {
                    Integer week = config.getWeek();
                    System.out.println("Week value: " + week);
                    return week;
                } else {
                    System.out.println("Config object is null");
                }
            }

            System.out.println("Total items found: " + itemCount);
            if (itemCount == 0) {
                System.out.println("No items found in table - table might be empty");
                return null;
            }
            return null;

        } catch (Exception e) {
            System.err
                    .println("EXCEPTION in getCurrentWeek(): " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return null;

        }
    }

    // Get all of the users from FBP-Users.  Used by the GetWeeklyResults lambda to get the list of
    //  users to update with the results of the picks for each user for the week.
    public static List<FBPUser> getAllUsers() {
        System.out.println("=== Starting getAllUsers() ===");
        String tableName = System
                .getenv("FBPUsersTableName");
        System.out.println("Environment variable FBPUsersTableName: " + tableName);
        if (tableName == null || tableName.isEmpty()) {
            System.err.println("ERROR: Environment variable FBPUsersTableName is not set or empty");
            return null;
        }
        try {
            System.out.println("Creating DynamoDB clients...");
            DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
            DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(dynamoDbClient)
                    .build();

            System.out.println("Creating table reference...");
            DynamoDbTable<FBPUser> table = enhancedClient.table(tableName, TableSchema.fromBean(FBPUser.class));

            System.out.println("Starting table scan...");
            PageIterable<FBPUser> userPages = table.scan();

            System.out.println("Iterating through scan results...");
            List<FBPUser> users = userPages.items().stream().collect(Collectors.toList());
            System.out.println("Total users found: " + users.size());
            return users;

        } catch (Exception e) {
            System.err
                    .println("EXCEPTION in getAllUsers(): " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
        public static String getDisplayName(String email) {
        System.out.println("=== Starting getDisplayName() ===");

        // Check environment variable first
        String tableName = System
                .getenv("FBPUsersTableName");
        System.out.println("Environment variable FBPUsersTableName: " + tableName);

        if (tableName == null || tableName.isEmpty()) {
            System.err.println("ERROR: Environment variable FBPUsersTableName is not set or empty");
            return null;
        }

        try {
            System.out.println("Creating DynamoDB clients...");
            DynamoDbClient dynamoDbClient = DynamoDbClient.builder().build();
            DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                    .dynamoDbClient(dynamoDbClient)
                    .build();

            System.out.println("Creating table reference...");
            DynamoDbTable<FBPUser> table = enhancedClient.table(tableName, TableSchema.fromBean(FBPUser.class));

            System.out.println("Starting table scan...");
            PageIterable<FBPUser> userPages = table.scan();

            System.out.println("Iterating through scan results...");
            int itemCount = 0;
            for (FBPUser user : userPages.items()) {
                itemCount++;
                System.out.println("Found item #" + itemCount);

                if (user != null) {
                    if(user.getEmail() != null && user.getEmail().equals(email)) {
                    String displayName = user.getDisplayName();
                    System.out.println("DisplayName value: " + displayName);
                    return displayName;
                    }

                } else {
                    System.out.println("Config object is null");
                }
            }

            System.out.println("Total items found: " + itemCount);
            if (itemCount == 0) {
                System.out.println("No items found in table - table might be empty");
                return null;
            }
            return null;

        } catch (Exception e) {
            System.err
                    .println("EXCEPTION in getDisplayName(): " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            return null;

        }
    }

    public static void logAction(com.fbp.FBPLogAction logCurrentAction) {
        System.out.println("LOG [" + logCurrentAction.getLevel() + "] - User: " + logCurrentAction.getEmail() + ", Action: "
                + logCurrentAction.getAction() + ", Details: " + logCurrentAction.getDetails());
        String tableName = System.getenv("FBPLogsTableName");
        DynamoDbClient dynamoDB = DynamoDbClient.builder().build();
        try {
            ZoneId nyZone = ZoneId.of("America/New_York");
            DateTimeFormatter skFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            logCurrentAction.setDate(new java.util.Date());
            String timestamp = (logCurrentAction.getDate() != null)
                    ? logCurrentAction.getDate().toInstant().atZone(nyZone).format(skFormatter)
                    : ZonedDateTime.now(nyZone).format(skFormatter);
            System.out.println("logAction - timestamp: " + logCurrentAction.getDate());

            // Using composite key: email (partition key) + timestamp (sort key)
            PutItemRequest putItemRequest = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(java.util.Map.of(
                            "email", AttributeValue.builder().s(logCurrentAction.getEmail()).build(),
                            "timestamp", AttributeValue.builder().s(timestamp).build(),
                            "week", AttributeValue.builder().s(logCurrentAction.getWeek()).build(),
                            "level", AttributeValue.builder().s(logCurrentAction.getLevel()).build(),
                            "action", AttributeValue.builder().s(logCurrentAction.getAction()).build(),
                            "details", AttributeValue.builder().s(logCurrentAction.getDetails()).build()))
                    .build();
            dynamoDB.putItem(putItemRequest);
            System.out.println("Successfully logged action with composite key - email: " + logCurrentAction.getEmail() + ", timestamp: " + timestamp);
        } catch (Exception e) {
            System.err.println("Failed to log action: " + e.getMessage());
        }
    }
    public static void copyToS3(String content, String s3Key) {
        String bucketName = System.getenv("BUCKET_NAME");
        System.out.println("Environment variable BUCKET_NAME: " + bucketName);
        System.out.println("Copying results to S3: " + s3Key);

        if (bucketName == null || bucketName.isEmpty()) {
            System.err.println("ERROR: Environment variable BUCKET_NAME is not set or empty");
            return;
        }
        try {
            S3Client s3Client = S3Client.builder().build();
            s3Client.putObject(builder -> builder.bucket(bucketName).key(s3Key),
                    software.amazon.awssdk.core.sync.RequestBody.fromString(content));
            System.out.println("Successfully copied results to S3: s3://" + bucketName + s3Key);
        } catch (Exception e) {
            System.err.println("Failed to copy results to S3: " + e.getMessage());
        }
    }

    public static JsonNode getPoolConfig() {
        try {
            String functionName = System.getenv("GET_POOL_CONFIG_FUNCTION");
            if (functionName == null || functionName.isEmpty()) {
                functionName = "GetPoolConfig";
            }
            LambdaClient lambdaClient = LambdaClient.create();
            InvokeRequest request = InvokeRequest.builder()
                    .functionName(functionName)
                    .payload(SdkBytes.fromUtf8String("{}"))
                    .build();
            InvokeResponse response = lambdaClient.invoke(request);
            String responseJson = response.payload().asUtf8String();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(responseJson);
            return mapper.readTree(root.get("body").asText());
        } catch (Exception e) {
            System.err.println("EXCEPTION in getPoolConfig(): " + e.getMessage());
            return null;
        }
    }
}