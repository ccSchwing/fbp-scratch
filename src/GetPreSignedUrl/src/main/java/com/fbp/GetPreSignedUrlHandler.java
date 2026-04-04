package com.fbp;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class GetPreSignedUrlHandler
                implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
        private final String bucketName = System.getenv("S3_BUCKET_NAME");
        private final Region region = Region.of(System.getenv().getOrDefault("AWS_REGION", "us-east-1"));

        @Override
        public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {

                FBPLogAction logEntry = new FBPLogAction();
                logEntry.setEmail("fbpadmin@my-fbp.com");
                Integer week = FBPUtils.getCurrentWeek();
                logEntry.setWeek(week != null ? week.toString() : "unknown");
                logEntry.setAction("GetPreSignedUrl");
                System.setProperty("aws.region", region.id());
                System.out.println("Initialized GetPreSignedUrlHandler with bucket: " + bucketName + " and region: "
                                + region.id());

                Map<String, String> headers = new HashMap<>();
                headers.put("Access-Control-Allow-Origin", "https://my-fbp.com");
                headers.put("Access-Control-Allow-Headers", "Content-Type");
                headers.put("Content-Type", "application/json");

                try {
                        if (event.getHttpMethod() != null && event.getHttpMethod().equalsIgnoreCase("OPTIONS")) {
                                return new APIGatewayProxyResponseEvent()
                                                .withStatusCode(200)
                                                .withHeaders(headers)
                                                .withBody("");
                        }

                        Object fileKeyObject = null;
                        String fileKey = null;
                        if (event.getQueryStringParameters() != null) {
                                fileKeyObject = event.getQueryStringParameters().get("key");
                                fileKey = fileKeyObject != null ? fileKeyObject.toString() : null;
                        }
                        String objectType = fileKeyObject != null ? fileKeyObject.getClass().getName() : "null";
                        System.out.println("Received request with 'key' parameter: "
                                        + (fileKeyObject != null ? fileKeyObject.toString() : "null") + " (type: "
                                        + objectType + ")");

                        if (fileKey == null || fileKey.isEmpty()) {
                                logEntry.setLevel("ERROR");
                                logEntry.setDetails(
                                                "Could not determine the fileKey: fileKey is the name of the file in S3.");
                                FBPUtils.logAction(logEntry);

                                return new APIGatewayProxyResponseEvent()
                                                .withStatusCode(400)
                                                .withHeaders(headers)
                                                .withBody("{\"error\":\"Missing required parameter: key\"}");
                        }
                        S3Presigner presigner = S3Presigner.builder()
                                        .region(region)
                                        .credentialsProvider(DefaultCredentialsProvider.create())
                                        .build();

                        System.out.println(
                                        "Generating presigned URL for bucket: " + bucketName + " and key: " + fileKey);
                        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                                        .bucket(bucketName)
                                        .key(fileKey)
                                        .build();

                        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                                        .signatureDuration(Duration.ofHours(1))
                                        .getObjectRequest(getObjectRequest)
                                        .build();

                        String presignedUrl = presigner.presignGetObject(presignRequest).url().toString();
                        System.out.println("Generated presigned URL: " + presignedUrl);

                        String responseBody = String.format("{\"url\":\"%s\",\"expires\":\"%s\"}",
                                        presignedUrl,
                                        java.time.Instant.now().plus(Duration.ofHours(1)).toString());

                        logEntry.setLevel("INFO");
                        logEntry.setDetails("Generated presigned URL for key: " + fileKey);
                        FBPUtils.logAction(logEntry);
                        return new APIGatewayProxyResponseEvent()
                                        .withStatusCode(200)
                                        .withHeaders(headers)
                                        .withBody(responseBody);
                } catch (Exception e) {
                        context.getLogger().log("Error generating presigned URL: " + e.getMessage());
                        logEntry.setLevel("ERROR");
                        logEntry.setDetails("Exception occurred: " + e.getMessage());
                        logEntry.setEmail("fbpadmin@my-fbp.com");
                        FBPUtils.logAction(logEntry);
                        return new APIGatewayProxyResponseEvent()
                                        .withStatusCode(500)
                                        .withHeaders(headers)
                                        .withBody(String.format(
                                                        "{\"error\":\"Failed to generate presigned URL\",\"message\":\"%s\"}",
                                                        e.getMessage()));
                }
        }
}
