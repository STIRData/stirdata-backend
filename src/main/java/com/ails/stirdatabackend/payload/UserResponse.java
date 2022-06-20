package com.ails.stirdatabackend.payload;

import com.ails.stirdatabackend.model.User;
import com.ails.stirdatabackend.model.UserLoginType;
import com.ails.stirdatabackend.model.UserType;

import org.bson.types.ObjectId;

import lombok.Data;

@Data
public class UserResponse {
    
    private ObjectId id;
    private String firstName;
    private String lastName;
    private String email;
    private UserType userType;
    private UserLoginType userLoginType;

    public UserResponse(User u) {
        this.id = u.getId();
        this.firstName = u.getFirstName();
        this.lastName = u.getLastName();
        this.email = u.getEmail();
        this.userType = u.getUserType();
        this.userLoginType = u.getUserLoginType();
    }
    
}
