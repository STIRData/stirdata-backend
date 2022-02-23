package com.ails.stirdatabackend.controller;

import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.payload.EndpointResponse;
import com.ails.stirdatabackend.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Date;
import java.util.List;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    @Autowired
    private QueryService queryService;

    // NUTS > place , NACE > activity
    @GetMapping
    public ResponseEntity<?> performQuery(@RequestParam(required = false) List<Code> place,
                                          @RequestParam(required = false) List<Code> activity,
                                          @RequestParam(required = false) Code founding,
                                          @RequestParam(required = false) Code dissolution,
                                          @RequestParam(required = false) String country,
                                          @RequestParam(defaultValue="1") int page) {
    	
        List<EndpointResponse> res = queryService.paginatedQuery(place, activity, founding, dissolution, page, country);
        return ResponseEntity.ok(res);
    }
    
    @GetMapping("/grouped")
    public ResponseEntity<?> groupByQuery(@RequestParam(required = false) List<Code> place,
                                          @RequestParam(required = false) List<Code> activity,
                                          @RequestParam(required = false) Code founding,
                                          @RequestParam(required = false) Code dissolution,
                                          @RequestParam() boolean gnace,
                                          @RequestParam() boolean gnuts3) {
        
    	List<EndpointResponse> res = queryService.groupedQuery(place, activity, founding, dissolution, gnace, gnuts3 );
        return ResponseEntity.ok(res);
    }
    
 
}
