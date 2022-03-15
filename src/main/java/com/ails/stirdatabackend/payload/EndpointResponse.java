package com.ails.stirdatabackend.payload;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EndpointResponse {
    private String endpointName;

//    private JsonNode response;
    private Object response;
    private int count;
    private String countryCode;

}
