package com.ails.stirdatabackend.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryDB;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ExistsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.RangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.json.JsonData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ElasticQuery {
	private BoolQuery.Builder query;
    
    public ElasticQuery() {
    }
    
    public static ElasticQuery buildCoreQuery(CountryDB cc, boolean active, Code nutsLauCode, Collection<Code> nutsLauCodes, Collection<Code> statNutsLauCodes, Code naceCode, Collection<Code> naceCodes, Code foundingDate, Code dissolutionDate) {
//    	System.out.println("NC " + nutsLauCode);
//    	System.out.println("NR " + nutsLauCodes);
//    	System.out.println("NS " + statNutsLauCodes);
//    	System.out.println("AC " + naceCode);
//    	System.out.println("AR " + naceCodes);
    	
    	ElasticQuery cq = new ElasticQuery();

		BoolQuery.Builder bool = QueryBuilders.bool();
		
		cq.query = bool;
		
        if (active && cc.isDissolutionDate()) {
        	if (dissolutionDate == null || (dissolutionDate.getDateFrom() == null && dissolutionDate.getDateTo() == null)) {
        		bool.mustNot(ExistsQuery.of(q -> q.field("dissolution-date"))._toQuery());
	        }
        }
        
        if (nutsLauCode != null || nutsLauCodes != null || statNutsLauCodes != null) {
        	BoolQuery.Builder placeQuery = QueryBuilders.bool();

            if (nutsLauCode != null) {
            	placeQuery.must(TermQuery.of(q -> q.field("admin-unit").value(nutsLauCode.toUri()))._toQuery());
            }
            
        	if (nutsLauCodes != null) {
        		if (nutsLauCodes.size() > 0) {

		        	Set<String> s = new HashSet<>();
		        	for (Code c : nutsLauCodes) {
		        		s.add(c.toUri());
		        	}
		        	
		        	BoolQuery.Builder inPlaceQuery = QueryBuilders.bool();
		            for (String uri : s) {
		            	inPlaceQuery.should(TermQuery.of(q -> q.field("admin-unit").value(uri))._toQuery());
		            }
		            
		            placeQuery.must(inPlaceQuery.build()._toQuery());
        		} else {
        			return null;
        		}
        	}
        	
        	if (statNutsLauCodes != null) {
        		if (statNutsLauCodes.size() > 0) {
	
		        	Set<String> s = new HashSet<>();
		        	for (Code c : statNutsLauCodes) {
		        		s.add(c.toUri());
		        	}
		        	
		        	BoolQuery.Builder inPlaceQuery = QueryBuilders.bool();
		            for (String uri : s) {
		            	inPlaceQuery.should(TermQuery.of(q -> q.field("admin-unit").value(uri))._toQuery());
		            }
		            
		            placeQuery.must(inPlaceQuery.build()._toQuery());
	        	} else {
	        		return null;
	        	}
        	}
        	
        	BoolQuery pq = placeQuery.build();
        	
        	if (pq.must().size() > 0) {
	            bool.must(pq._toQuery());
        	} else {
        		return null;
        	}
        }
        
        if (naceCode != null || naceCodes != null) {
    		BoolQuery.Builder activityQuery = QueryBuilders.bool();
        	
            if (naceCode != null) {
            	activityQuery.must(TermQuery.of(q -> q.field("activity").value(naceCode.toUri()))._toQuery());
            }

        	if (naceCodes != null) {
        		if (naceCodes.size() > 0) {
	        		Set<String> s = new HashSet<>();
		        	for (Code c : naceCodes) {
		        		s.add(c.toUri());
		        	}
		        	
		        	BoolQuery.Builder inActivityQuery = QueryBuilders.bool();
		            for (String uri : s) {
		            	inActivityQuery.should(TermQuery.of(q -> q.field("activity").value(uri))._toQuery());
		            }
		            
		            activityQuery.must(inActivityQuery.build()._toQuery());
	        	} else {
	        		return null;
	        	}
        	}
        	
        	BoolQuery aq = activityQuery.build();
        	
        	if (aq.must().size() > 0) {
	            bool.must(aq._toQuery());
        	} else {
        		return null;
        	}
        }

        if (foundingDate != null && (foundingDate.getDateFrom() != null || foundingDate.getDateTo() != null)) {
//            RangeQuery.Builder rangeQuery = QueryBuilders.range("founding-date");
        	RangeQuery.Builder rangeQuery = QueryBuilders.range();
            
            if (foundingDate.getDateFrom() != null) {
            	rangeQuery.field("founding-date").gte(JsonData.of(foundingDate.getDateFrom()));
//            	rangeQuery.from(foundingDate.getDateFrom().toString(), true);
            }

            if (foundingDate.getDateTo() != null) {
            	rangeQuery.field("founding-date").lte(JsonData.of(foundingDate.getDateTo()));
//            	rangeQuery.to(foundingDate.getDateTo().toString(),true);
            }
            
            bool.must(rangeQuery.build()._toQuery());
        }
        
        if (dissolutionDate != null && (dissolutionDate.getDateFrom() != null || dissolutionDate.getDateTo() != null)) {
//            RangeQuery.Builder rangeQuery = QueryBuilders.rangeQuery("dissolution-date");
            RangeQuery.Builder rangeQuery = QueryBuilders.range();
            
            if (dissolutionDate.getDateFrom() != null) {
//            	rangeQuery.from(dissolutionDate.getDateFrom().toString(), true);
            	rangeQuery.field("dissolution-date").gte(JsonData.of(dissolutionDate.getDateFrom()));
            }

            if (dissolutionDate.getDateTo() != null) {
//            	rangeQuery.to(dissolutionDate.getDateTo().toString(),true);
            	rangeQuery.field("dissolution-date").lte(JsonData.of(dissolutionDate.getDateTo()));
            }
            
            bool.must(rangeQuery.build()._toQuery());
        }
        
        return cq;
    }
    
 }
    

