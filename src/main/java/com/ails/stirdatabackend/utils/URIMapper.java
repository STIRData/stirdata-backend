package com.ails.stirdatabackend.utils;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Component
public class URIMapper {

//    public HashMap<String, String> mapCzechNutsUri(List<String> uris) {
//        HashMap<String, String> response = new HashMap<String, String>();
//        for (String s : uris) {
//            URI uri = URI.create(s);
//            String path = uri.getPath();
//            String lastPart = path.substring(path.lastIndexOf('/') + 1);
//            response.put(s, "http://nuts.geovocab.org/id/" + lastPart);
//        }
//        return response;
//    }
//
//    public HashMap<String, String> mapEuropaNutsUri(List<String> uris) {
//        HashMap<String, String> response = new HashMap<String, String>();
//        for (String s : uris) {
//            URI uri = URI.create(s);
//            String path = uri.getPath();
//            String lastPart = path.substring(path.lastIndexOf('/') + 1);
//            response.put(s, "http://data.europa.eu/nuts/code/" + lastPart);
//        }
//        return response;
//    }


    public List<String> mapCzechNutsUri(List<String> uris) {
        List<String> response = new ArrayList<>();
        for (String s : uris) {
            URI uri = URI.create(s);
            String path = uri.getPath();
            String lastPart = path.substring(path.lastIndexOf('/') + 1);
            response.add("http://nuts.geovocab.org/id/" + lastPart);
        }
        return response;
    }

    public List<String> mapEuropaNutsUri(List<String> uris) {
//        List<String> response = new ArrayList<String>();
//        for (String s : uris) {
//            URI uri = URI.create(s);
//            String path = uri.getPath();
//            String lastPart = path.substring(path.lastIndexOf('/') + 1);
//            response.add("https://data.europa.eu/nuts/code/" + lastPart);
//        }
//        return response;
    	return uris;
    }
	
}
