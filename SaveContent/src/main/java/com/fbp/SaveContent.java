package com.fbp;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class SaveContent implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    
    private final S3Client s3Client = S3Client.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String bucketName = System.getenv("BUCKET_NAME");
    
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        context.getLogger().log("Processing save content request");
        
        try {
            // Parse request body
            JsonNode requestBody = objectMapper.readTree(event.getBody());
            String content = requestBody.get("content").asText();
            String sourceUrl = requestBody.has("url") ? requestBody.get("url").asText() : "unknown";
            String timestamp = requestBody.has("timestamp") ? requestBody.get("timestamp").asText() : ZonedDateTime.now().toString();
            String userAgent = requestBody.has("userAgent") ? requestBody.get("userAgent").asText() : "unknown";
            
            // Create organized S3 key
            String datePrefix = timestamp.substring(0, 10); // YYYY-MM-DD
            String key = fileNameFromSourceUrl(sourceUrl);
            // Enhance content with metadata
            // String enhancedContent = addSaveMetadata(content, sourceUrl, timestamp, userAgent, context.getAwsRequestId());
            
            // Prepare S3 metadata
            // Map<String, String> metadata = new HashMap<>();
            // metadata.put("source-url", sourceUrl);
            // metadata.put("save-timestamp", timestamp);
            // metadata.put("user-agent", userAgent);
            // metadata.put("lambda-request-id", context.getAwsRequestId());
            // metadata.put("content-length", String.valueOf(enhancedContent.length()));
            
            // Upload to S3
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("text/html; charset=utf-8")
                // .metadata(metadata)
                .build();
                
            s3Client.putObject(putRequest, RequestBody.fromString(content));
            
            context.getLogger().log("Successfully saved content to S3: " + key);
            
            // Return success response
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("message", "Content saved successfully");
            responseBody.put("key", key);
            responseBody.put("timestamp", timestamp);
            responseBody.put("size", content.length());
            
            return createResponse(200, responseBody);
            
        } catch (Exception e) {
            context.getLogger().log("Error processing request: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to save content");
            errorResponse.put("message", e.getMessage());
            
            return createResponse(500, errorResponse);
        }
    }
    
    // private String addSaveMetadata(String originalContent, String sourceUrl, String timestamp, String userAgent, String requestId) {
    // String formattedTimestamp = ZonedDateTime.parse(timestamp).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));
    
//     StringBuilder metadataBuilder = new StringBuilder();
//     metadataBuilder.append("<div id=\"save-metadata\" style=\"")
//         .append("background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); ")
//         .append("color: white; ")
//         .append("padding: 15px; ")
//         .append("margin: 20px 0; ")
//         .append("border-radius: 8px; ")
//         .append("font-family: Arial, sans-serif; ")
//         .append("box-shadow: 0 4px 6px rgba(0,0,0,0.1); ")
//         .append("\">")
//         .append("<h3 style=\"margin: 0 0 10px 0; font-size: 18px;\">📄 Page Save Information</h3>")
//         .append("<div style=\"font-size: 14px; line-height: 1.4;\">")
//         .append("<p style=\"margin: 5px 0;\"><strong>Source URL:</strong> ")
//         .append("<span style=\"word-break: break-all;\">").append(escapeHtml(sourceUrl)).append("</span></p>")
//         .append("<p style=\"margin: 5px 0;\"><strong>Saved at:</strong> ").append(formattedTimestamp).append("</p>")
//         .append("<p style=\"margin: 5px 0;\"><strong>Method:</strong> Secure API (Lambda + S3)</p>")
//         .append("<p style=\"margin: 5px 0;\"><strong>Request ID:</strong> ")
//         .append("<code style=\"background: rgba(255,255,255,0.2); padding: 2px 4px; border-radius: 3px;\">")
//         .append(requestId).append("</code></p>")
//         .append("</div>")
//         .append("</div>");
    
//     String metadataSection = metadataBuilder.toString();
    
//     // Insert metadata after opening <body> tag
//     if (originalContent.contains("<body")) {
//         return originalContent.replaceFirst("(<body[^>]*>)", "$1" + metadataSection);
//     } else {
//         return metadataSection + originalContent;
//     }
// }

// Helper method to escape HTML characters
private String escapeHtml(String input) {
    if (input == null) return "";
    return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
}

private static String fileNameFromSourceUrl(String sourceUrl) {
    URI uri = URI.create(sourceUrl);
    String rawPath = uri.getPath();                 // excludes query/fragment
    String name = Paths.get(rawPath).getFileName().toString();
    return URLDecoder.decode(name, StandardCharsets.UTF_8); // handles %20, etc.
}

    


    private APIGatewayProxyResponseEvent createResponse(int statusCode, Object body) {
            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Access-Control-Allow-Origin", "*");
            headers.put("Access-Control-Allow-Headers", "Content-Type");
            headers.put("Access-Control-Allow-Methods", "POST, OPTIONS");
        try {
            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.setStatusCode(statusCode);
            response.setHeaders(headers);
            response.setBody(objectMapper.writeValueAsString(body));
            return response;

        } catch (Exception e) {
            APIGatewayProxyResponseEvent errorResponse = new APIGatewayProxyResponseEvent();
            errorResponse.setStatusCode(500);
            errorResponse.setHeaders(headers);
            errorResponse.setBody("{\"success\": false, \"error\": \"Failed to save static content.\"}");
            return errorResponse;
        }
    }
}
