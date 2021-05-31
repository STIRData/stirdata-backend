package com.ails.stirdatabackend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
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

    public HashMap<SparqlEndpoint, List<String>> getEndpointsByNuts(List<String> nutsUri) {
        HashMap<SparqlEndpoint, List<String>> response = new HashMap<SparqlEndpoint, List<String>>();
        for (String uri : nutsUri) {
            SparqlEndpoint tmp = getEndpointFromNutsUri(uri);
            if (response.containsKey(tmp)) {
                response.get(tmp).add(uri);
            }
            else {
                response.put(tmp, new ArrayList<String>());
            }
        }
        return response;
    }
}
