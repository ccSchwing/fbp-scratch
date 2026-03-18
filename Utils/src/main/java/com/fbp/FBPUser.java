package com.fbp;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class FBPUser {
    public String firstName;
    public String lastName;
    public String email;
    public String displayName;
    public Boolean isAdmin;
    public Boolean emailPickSheet;
    public Boolean emailReminders;
    public Boolean emailGridSheet;      // Everybody's picks.
    public String defaultAlgorithm;     // Random, Favorites, Underdogs, etc.
    public Boolean isAccountLocked;
    public Boolean isPaidUser;
    public String totalCorrectPicks;
    public String totalIncorrectPicks;

    public FBPUser() {
        super();
    }

    @DynamoDbPartitionKey
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    @DynamoDbAttribute("firstName")
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    @DynamoDbAttribute("lastName")
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    @DynamoDbAttribute("displayName")
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    @DynamoDbAttribute("isAdmin")
    public Boolean getIsAdmin() {
        return isAdmin;
    }
    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }
    @DynamoDbAttribute("emailPickSheet")
    public Boolean getEmailPickSheet() {
        return emailPickSheet;
    }
    public void setEmailPickSheet(Boolean emailPickSheet) {
        this.emailPickSheet = emailPickSheet;
    }
    @DynamoDbAttribute("emailReminders")
    public Boolean getEmailReminders() {
        return emailReminders;
    }
    public void setEmailReminders(Boolean emailReminders) {
        this.emailReminders = emailReminders;
    }
    @DynamoDbAttribute("emailGridSheet")
    public Boolean getEmailGridSheet() {
        return emailGridSheet;
    }
    public void setEmailGridSheet(Boolean emailGridSheet) {
        this.emailGridSheet = emailGridSheet;
    }
    @DynamoDbAttribute("defaultAlgorithm")
    public String getDefaultAlgorithm() {
        return defaultAlgorithm;
    }
    public void setDefaultAlgorithm(String defaultAlgorithm) {
        this.defaultAlgorithm = defaultAlgorithm;
    }
    @DynamoDbAttribute("isAccountLocked")
    public Boolean getIsAccountLocked() {
        return isAccountLocked;
    }
    public void setIsAccountLocked(Boolean isAccountLocked) {
        this.isAccountLocked = isAccountLocked;
    }
    @DynamoDbAttribute("isPaidUser")
    public Boolean getIsPaidUser() {
        return isPaidUser;
    }
    public void setIsPaidUser(Boolean isPaidUser) {
        this.isPaidUser = isPaidUser;
    }
    @DynamoDbAttribute("totalCorrectPicks")
    public String getTotalCorrectPicks() {
        return totalCorrectPicks;
    }
    public void setTotalCorrectPicks(String totalCorrectPicks) {
        this.totalCorrectPicks = totalCorrectPicks;
    }
    @DynamoDbAttribute("totalIncorrectPicks")
    public String getTotalIncorrectPicks() {
        return totalIncorrectPicks;
    }
    public void setTotalIncorrectPicks(String totalIncorrectPicks) {
        this.totalIncorrectPicks = totalIncorrectPicks;
    }
}
