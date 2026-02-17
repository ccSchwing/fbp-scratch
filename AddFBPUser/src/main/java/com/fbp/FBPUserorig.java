package com.fbp;

public class FBPUserorig {
    public String fbpUser;

    public String firstName;
    public String lastName;
    public String email;
    public String displayName;

    public FBPUserorig() {
        super();
    }


public FBPUserorig(String firstName, String lastName, String email, String displayName) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.displayName = displayName;
}

public String getFbpUser() {
        return this.fbpUser;
    }

    public void setFbpUser(String fbpUser) {
        this.fbpUser = fbpUser;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return this.displayName;
    }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
