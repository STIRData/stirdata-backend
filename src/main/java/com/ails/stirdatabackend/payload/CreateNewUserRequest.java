package com.ails.stirdatabackend.payload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateNewUserRequest {

    private String email;
    private String password;
    private String firstName;
    private String lastName;

}
