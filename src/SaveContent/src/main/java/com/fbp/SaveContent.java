package com.fbp;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.dynamodb.endpoints.internal.Value;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class SaveContent implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final S3Client s3Client = S3Client.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String bucketName = System.getenv("BUCKET_NAME");
    private FBPLogAction logEntry = new FBPLogAction();
    private final Integer week = FBPUtils.getCurrentWeek();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        logEntry.setEmail("fbpadmin@my-fbp.com");
        logEntry.setAction("SaveContent");
        logEntry.setWeek(week.toString());
        try {
            context.getLogger().log("Processing save content request");
            System.out.println("Processing save content request");
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

            // Add CORS headers to ALL responses
            Map<String, String> headers = new HashMap<>();
            headers.put("Access-Control-Allow-Origin", "*");
            headers.put("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            headers.put("Access-Control-Allow-Headers",
                    "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");
            response.setHeaders(headers);

            // Handle OPTIONS preflight request
            if ("OPTIONS".equals(request.getHttpMethod())) {
                response.setStatusCode(200);
                response.setBody("");
                return response;
            }
        } catch (Exception e) {
            System.out.println("Error handling CORS preflight: " + e.getMessage());
            // Return error response with CORS headers
            APIGatewayProxyResponseEvent errorResponse = new APIGatewayProxyResponseEvent();
            Map<String, String> errorHeaders = new HashMap<>();
            errorHeaders.put("Access-Control-Allow-Origin", "*");
            errorResponse.setHeaders(errorHeaders);
            errorResponse.setStatusCode(500);
            errorResponse.setBody("{\"error\": \"Internal server error\"}");
            return errorResponse;
        }
        try {
            // Parse request body
            JsonNode requestBody = objectMapper.readTree(request.getBody());
            String content = requestBody.get("content").asText();
            String sourceUrl = requestBody.has("url") ? requestBody.get("url").asText() : "unknown";
            String timestamp = requestBody.has("timestamp") ? requestBody.get("timestamp").asText()
                    : ZonedDateTime.now().toString();

            // Create organized S3 key
            String key = fileNameFromSourceUrl(sourceUrl);

            // Upload to S3
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("text/html; charset=utf-8")
                    // .metadata(metadata)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromString(content));

            System.out.println("Successfully saved content to S3: " + key);

            // Return success response
            logEntry.setLevel("INFO");
            logEntry.setDetails("Successfully saved content to S3 with key: " + key);
            FBPUtils.logAction(logEntry);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("message", "Content saved successfully");
            responseBody.put("key", key);
            responseBody.put("timestamp", timestamp);
            responseBody.put("size", content.length());

            return createResponse(200, responseBody);

        } catch (Exception e) {
            logEntry.setLevel("ERROR");
            logEntry.setDetails("Exception occurred: " + e.getMessage());
            FBPUtils.logAction(logEntry);
            System.out.println("Error processing request: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to save content");
            errorResponse.put("message", e.getMessage());

            return createResponse(500, errorResponse);
        }
    }

    // Helper method to escape HTML characters
    private String escapeHtml(String input) {
        if (input == null)
            return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    private static String fileNameFromSourceUrl(String sourceUrl) {
        URI uri = URI.create(sourceUrl);
        String rawPath = uri.getPath(); // excludes query/fragment
        String name = Paths.get(rawPath).getFileName().toString();
        return URLDecoder.decode(name, StandardCharsets.UTF_8); // handles %20, etc.
    }

    private APIGatewayProxyResponseEvent createResponse(int statusCode, Object body) {
        Map<String, String> headers = new HashMap<>();

        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*");
        headers.put("Access-Control-Allow-Headers",
                "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");
        // headers.put("Access-Control-Allow-Headers", "Content-Type");
        headers.put("Access-Control-Allow-Methods", "POST, OPTIONS");
        try {
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(statusCode);
            response.setHeaders(headers);
            response.setBody(objectMapper.writeValueAsString(body));
            return response;

        } catch (Exception e) {
            logEntry.setLevel("ERROR");
            logEntry.setDetails("Exception occurred while creating error response: " + e.getMessage());
            FBPUtils.logAction(logEntry);
            System.out.println("Error creating response: " + e.getMessage());
            APIGatewayProxyResponseEvent errorResponse = new APIGatewayProxyResponseEvent();
            errorResponse.setStatusCode(500);
            errorResponse.setHeaders(headers);
            errorResponse.setBody("{\"success\": false, \"error\": \"Failed to save static content.\"}");
            return errorResponse;
        }
    }
}
