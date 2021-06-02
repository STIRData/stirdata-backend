package com.ails.stirdatabackend.payload;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EndpointResponse {
    private String endpointName;
    private JsonNode response;

    public EndpointResponse(String endpointName, JsonNode response) {
        this.endpointName = endpointName;
        this.response = response;
    }
}
