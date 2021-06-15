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
    private int count;

    public EndpointResponse(String endpointName, JsonNode response, int count) {
        this.endpointName = endpointName;
        this.response = response;
        this.count = count;
    }
}
