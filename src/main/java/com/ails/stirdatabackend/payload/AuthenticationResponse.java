package com.ails.stirdatabackend.payload;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthenticationResponse {
    private String tokenType = "Bearer";
    private String token;

    public AuthenticationResponse(String token) {
        this.token = token;
    }
}
