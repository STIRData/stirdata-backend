package com.ails.stirdatabackend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class EndpointManager {

    @Autowired
    private List<SparqlEndpoint> endpointList;

    public SparqlEndpoint getEndpointFromNutsUri(String uri) {
        SparqlEndpoint res = null;
        for (SparqlEndpoint endpoint : endpointList) {
            System.out.println("Testing: " + endpoint.getTopLevelNuts());
            if (endpoint.getTopLevelNuts().equals(uri)) {
                res = endpoint;
                break;
            }
        }
        return res;
    }
}
