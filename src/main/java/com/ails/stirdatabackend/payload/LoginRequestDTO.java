package com.ails.stirdatabackend.payload;

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
@NotNull
public class LoginRequestDTO {

    private String email;
    private String password;
}
