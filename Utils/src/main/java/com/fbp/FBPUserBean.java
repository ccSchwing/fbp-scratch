package com.fbp;


import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class FBPUserBean {
    private String email;
    private String firstName;
    private String lastName;
    private String displayName;

    @DynamoDbPartitionKey
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

     @DynamoDbAttribute("firstName")
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

     @DynamoDbAttribute("lastName") 
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    @DynamoDbAttribute("displayName")
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; } 
}
