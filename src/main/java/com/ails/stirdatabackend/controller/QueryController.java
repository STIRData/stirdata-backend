package com.ails.stirdatabackend.controller;

import com.ails.stirdatabackend.configuration.CountryConfiguration;
import com.ails.stirdatabackend.configuration.Dimension;
import com.ails.stirdatabackend.payload.EndpointResponse;
import com.ails.stirdatabackend.service.QueryService;
import com.ails.stirdatabackend.service.QueryService.StatisticResult;
import com.ails.stirdatabackend.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    @Autowired
    private TestService testService;

    @Autowired
    private QueryService queryService;

    @Autowired
    @Qualifier("country-configurations")
    private Map<String, CountryConfiguration> countryConfigurations;
    
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
                                          @RequestParam(required = false) Optional<String> foundingStartDate,
                                          @RequestParam(required = false) Optional<String> foundingEndDate,
                                          @RequestParam(required = false) Optional<String> dissolutionStartDate,
                                          @RequestParam(required = false) Optional<String> dissolutionEndDate,
                                          @RequestParam(required = false) Optional<String> country,
                                          @RequestParam(required = false, defaultValue="1") int page) {
        List<EndpointResponse> res = queryService.paginatedQuery(NUTS.orElse(null), NACE.orElse(null), foundingStartDate.orElse(null), foundingEndDate.orElse(null), dissolutionStartDate.orElse(null), dissolutionEndDate.orElse(null), page, country.orElse(null) );
        return ResponseEntity.ok(res);
    }
    
    @GetMapping("/grouped")
    public ResponseEntity<?> groupByQuery(@RequestParam(required = false) Optional<List<String>> NUTS,
                                          @RequestParam(required = false) Optional<List<String>> NACE,
                                          @RequestParam(required = false) Optional<String> foundingStartDate,
                                          @RequestParam(required = false) Optional<String> foundingEndDate,
                                          @RequestParam(required = false) Optional<String> dissolutionStartDate,
                                          @RequestParam(required = false) Optional<String> dissolutionEndDate,
                                          @RequestParam() boolean gnace,
                                          @RequestParam() boolean gnuts3) {
        List<EndpointResponse> res = queryService.groupedQuery(NUTS.orElse(null), NACE.orElse(null), foundingStartDate.orElse(null), foundingEndDate.orElse(null), dissolutionStartDate.orElse(null), dissolutionEndDate.orElse(null), gnace, gnuts3 );
        return ResponseEntity.ok(res);
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<?> statistics(@RequestParam String country, 
    		                            @RequestParam Dimension dimension, 
    		                            @RequestParam Optional<String> top,
    		                            @RequestParam(defaultValue = "false") boolean allLevels,
    		                            @RequestParam(required = false) Optional<List<String>> NUTS,
                                        @RequestParam(required = false) Optional<List<String>> NACE,
                                        @RequestParam(required = false) Optional<String> foundingStartDate,
                                        @RequestParam(required = false) Optional<String> foundingEndDate,
                                        @RequestParam(required = false) Optional<String> dissolutionStartDate,
                                        @RequestParam(required = false) Optional<String> dissolutionEndDate) {
    	CountryConfiguration cc = countryConfigurations.get(country);
    	
    	if (cc != null) {
    		List<StatisticResult> res = new ArrayList<>();
    		
    		if (dimension == Dimension.NUTS || dimension == Dimension.NACE) {
	    		queryService.statistics(cc, dimension, top.orElse(null), NUTS.orElse(null), NACE.orElse(null), foundingStartDate.orElse(null), foundingEndDate.orElse(null), dissolutionStartDate.orElse(null), dissolutionEndDate.orElse(null), res);
	    		
	    		if (allLevels) {
	    			for (int i = 0; i < res.size(); i++) {
	    				queryService.statistics(cc, dimension, res.get(i).getUri(), NUTS.orElse(null), NACE.orElse(null), foundingStartDate.orElse(null), foundingEndDate.orElse(null), dissolutionStartDate.orElse(null), dissolutionEndDate.orElse(null), res);
	    			}
	    		}
    		} else if (dimension == Dimension.FOUNDING || dimension == Dimension.DISSOLUTION) {
    			try {
			    	String fromDate = null;
			    	String toDate = null;

    				if (top.isPresent()) {
    			    	Pattern p = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})?--(\\d{4}-\\d{2}-\\d{2})?");
    			    	
    			    	Matcher m = p.matcher(top.get());
    			    	if (m.find()) {
    			    		fromDate = m.group(1);
    			    		toDate = m.group(2);
    			    	}
    				}
    				
	    			queryService.dateStatistics(cc, dimension, fromDate, toDate, null, NUTS.orElse(null), NACE.orElse(null), foundingStartDate.orElse(null), foundingEndDate.orElse(null), dissolutionStartDate.orElse(null), dissolutionEndDate.orElse(null), res);
		    		if (allLevels) {
		    			for (int i = 0; i < res.size(); i++) {
		    				queryService.dateStatistics(cc, dimension, res.get(i).getFromDate(), res.get(i).getToDate(), res.get(i).getInterval(), NUTS.orElse(null), NACE.orElse(null), foundingStartDate.orElse(null), foundingEndDate.orElse(null), dissolutionStartDate.orElse(null), dissolutionEndDate.orElse(null), res);
		    			}
		    		}
    			} catch (Exception e) {
    				e.printStackTrace();
    				return ResponseEntity.badRequest().build();
    			}
    		}
    		return ResponseEntity.ok(res);
    	} else {
    		return ResponseEntity.notFound().build();
    	}
    }
}
