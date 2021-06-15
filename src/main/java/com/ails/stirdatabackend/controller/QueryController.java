package com.ails.stirdatabackend.controller;

import com.ails.stirdatabackend.payload.EndpointResponse;
import com.ails.stirdatabackend.service.QueryService;
import com.ails.stirdatabackend.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@CrossOrigin
@RestController
@RequestMapping("/api/query")
public class QueryController {

    @Autowired
    private TestService testService;

    @Autowired
    private QueryService queryService;

    @GetMapping("/test/belgium")
    public ResponseEntity<?> testEndpointConnectionBelgium() {
        String res = testService.testSparqlQueryBelgium();
        return ResponseEntity.ok(res);
    }

    @GetMapping("/test/czech")
    public ResponseEntity<?> testEndpointConnectionCzech() {
        List<String> res = testService.testSparqlQueryCzech();
        return ResponseEntity.ok(res);
    }

    @GetMapping
    public ResponseEntity<?> performQuery(@RequestParam(required = false) Optional<List<String>> NUTS,
                                          @RequestParam(required = false) Optional<List<String>> NACE,
                                          @RequestParam(required = false) Optional<String> startDate,
                                          @RequestParam(required = false) Optional<String> endDate,
                                          @RequestParam(required = false, defaultValue="1") int page) {
//        String res = queryService.query(nutsList, naceList);
        List<EndpointResponse> res = queryService.paginatedQuery(NUTS,NACE, startDate, endDate, page );
        return ResponseEntity.ok(res);
    }
    
    @GetMapping("/grouped")
    public ResponseEntity<?> groupByQuery(@RequestParam(required = false) Optional<List<String>> NUTS,
                                          @RequestParam(required = false) Optional<List<String>> NACE,
                                          @RequestParam(required = false) Optional<String> startDate,
                                          @RequestParam(required = false) Optional<String> endDate,
                                          @RequestParam(required = true) boolean gnace,
                                          @RequestParam(required = true) boolean gnuts3) {
//        String res = queryService.query(nutsList, naceList);
        List<EndpointResponse> res = queryService.groupedQuery(NUTS,NACE, startDate, endDate, gnace, gnuts3 );
        return ResponseEntity.ok(res);
    }
}
