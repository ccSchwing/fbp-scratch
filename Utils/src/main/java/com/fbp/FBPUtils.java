package com.fbp;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;


import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

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

            String sortKeyDate = (logCurrentAction.getDate() != null)
                    ? logCurrentAction.getDate().toInstant().atZone(nyZone).format(skFormatter)
                    : ZonedDateTime.now(nyZone).format(skFormatter);
            PutItemRequest putItemRequest = PutItemRequest.builder()
                    .tableName(tableName)
                    .item(java.util.Map.of(
                            "email", AttributeValue.builder().s(logCurrentAction.getEmail()).build(),
                            "week", AttributeValue.builder().s(logCurrentAction.getWeek()).build(),
                            "timestamp", AttributeValue.builder().s(sortKeyDate).build(),
                            "level", AttributeValue.builder().s(logCurrentAction.getLevel()).build(),
                            "action", AttributeValue.builder().s(logCurrentAction.getAction()).build(),
                            "details", AttributeValue.builder().s(logCurrentAction.getDetails()).build()))
                    .build();
            dynamoDB.putItem(putItemRequest);
            // Implement logging logic here, e.g., write to DynamoDB or CloudWatch
        } catch (Exception e) {
            System.err.println("Failed to log action: " + e.getMessage());
        }
    }
}