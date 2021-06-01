package com.ails.stirdatabackend.model;

import com.ails.stirdatabackend.service.NutsService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@NoArgsConstructor
public class EndpointManager {

    @Autowired
    private NutsService nutsService;

    @Autowired
    private List<SparqlEndpoint> endpointList;


    public SparqlEndpoint getEndpointFromNutsUri(String uri) {
        SparqlEndpoint res = null;
        String topLevelNut = nutsService.getTopLevelNuts(uri);
        for (SparqlEndpoint endpoint : endpointList) {
            System.out.println("Testing: " + endpoint.getTopLevelNuts());
            if (endpoint.getTopLevelNuts().equals(topLevelNut)) {
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
            } else {
                List<String> tmpLst = new ArrayList<String>();
                tmpLst.add(uri);
                response.put(tmp, tmpLst);
            }
        }
        return response;
    }


}
