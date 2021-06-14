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
//        SparqlEndpoint res = null;
//        String topLevelNut = nutsService.getTopLevelNuts(uri);
        
        // should make sure it is a valid nuts uri first!
        int pos = uri.lastIndexOf("/");
        String topLevelNut = uri.substring(0, pos + 3);
        
        for (SparqlEndpoint endpoint : endpointList) {
        	if (endpoint.getTopLevelNuts() != null) {
	            System.out.println("Testing: " + endpoint.getTopLevelNuts());
	            if (endpoint.getTopLevelNuts().equals(topLevelNut)) {
	                return endpoint;
	            }
        	}
        }
        return null;
    }

    public HashMap<SparqlEndpoint, List<String>> getEndpointsByNuts(List<String> nutsUri) {
        HashMap<SparqlEndpoint, List<String>> response = new HashMap<SparqlEndpoint, List<String>>();
        for (String uri : nutsUri) {
            SparqlEndpoint tmp = getEndpointFromNutsUri(uri);
            System.out.println(">> " + tmp);

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
    
    public HashMap<SparqlEndpoint, List<String>> getEndpointsByNuts() {
        HashMap<SparqlEndpoint, List<String>> response = new HashMap<SparqlEndpoint, List<String>>();
        for (SparqlEndpoint se : endpointList) {
        	if (se.getTopLevelNuts() != null) {
       			response.put(se, null);
        	}
        }
        return response;
    }

}
