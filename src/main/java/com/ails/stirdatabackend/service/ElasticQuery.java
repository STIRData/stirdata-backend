package com.ails.stirdatabackend.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;

import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryDB;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ElasticQuery {
	private QueryBuilder query;
    
    public ElasticQuery() {
    }
    
    public static ElasticQuery buildCoreQuery(CountryDB cc, boolean active, Code nutsLauCode, Collection<Code> nutsLauCodes, Collection<Code> statNutsLauCodes, Code naceCode, Collection<Code> naceCodes, Code foundingDate, Code dissolutionDate) {
//    	System.out.println("NC " + nutsLauCode);
//    	System.out.println("NR " + nutsLauCodes);
//    	System.out.println("NS " + statNutsLauCodes);
//    	System.out.println("AC " + naceCode);
//    	System.out.println("AR " + naceCodes);
    	
    	ElasticQuery cq = new ElasticQuery();

		BoolQueryBuilder bool = QueryBuilders.boolQuery();
		
		cq.query = bool;
		
        if (active && cc.isDissolutionDate()) {
        	if (dissolutionDate == null || (dissolutionDate.getDateFrom() == null && dissolutionDate.getDateTo() == null)) {
        		bool.mustNot(QueryBuilders.existsQuery("dissolution-date"));
	        }
        }
        
        if (nutsLauCode != null || nutsLauCodes != null || statNutsLauCodes != null) {
        	BoolQueryBuilder placeQuery = QueryBuilders.boolQuery();

            if (nutsLauCode != null) {
            	placeQuery.must(QueryBuilders.termQuery("admin-unit", nutsLauCode.toUri()));
            }
            
        	if (nutsLauCodes != null) {
        		if (nutsLauCodes.size() > 0) {

		        	Set<String> s = new HashSet<>();
		        	for (Code c : nutsLauCodes) {
		        		s.add(c.toUri());
		        	}
		        	
		        	BoolQueryBuilder inPlaceQuery = QueryBuilders.boolQuery();
		            for (String uri : s) {
		            	inPlaceQuery.should(QueryBuilders.termQuery("admin-unit", uri));
		            }
		            
		            placeQuery.must(inPlaceQuery);
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
		        	
		        	BoolQueryBuilder inPlaceQuery = QueryBuilders.boolQuery();
		            for (String uri : s) {
		            	inPlaceQuery.should(QueryBuilders.termQuery("admin-unit", uri));
		            }
		            
		            placeQuery.must(inPlaceQuery);
	        	} else {
	        		return null;
	        	}
        	}
        	
        	if (placeQuery.hasClauses()) {
	            bool.must(placeQuery);
        	} else {
        		return null;
        	}
        }
        
        if (naceCode != null || naceCodes != null) {
    		BoolQueryBuilder activityQuery = QueryBuilders.boolQuery();
        	
            if (naceCode != null) {
            	activityQuery.must(QueryBuilders.termQuery("activity", naceCode.toUri()));
            }

        	if (naceCodes != null) {
        		if (naceCodes.size() > 0) {
	        		Set<String> s = new HashSet<>();
		        	for (Code c : naceCodes) {
		        		s.add(c.toUri());
		        	}
		        	
		        	BoolQueryBuilder inActivityQuery = QueryBuilders.boolQuery();
		            for (String uri : s) {
		            	inActivityQuery.should(QueryBuilders.termQuery("activity", uri));
		            }
		            
		            activityQuery.must(inActivityQuery);
	        	} else {
	        		return null;
	        	}
        	}
        	
        	if (activityQuery.hasClauses()) {
	            bool.must(activityQuery);
        	} else {
        		return null;
        	}
        }

        if (foundingDate != null && (foundingDate.getDateFrom() != null || foundingDate.getDateTo() != null)) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("founding-date");
            
            if (foundingDate.getDateFrom() != null) {
            	rangeQuery.from(foundingDate.getDateFrom().toString(), true);
            }

            if (foundingDate.getDateTo() != null) {
            	rangeQuery.to(foundingDate.getDateTo().toString(),true);
            }
            
            bool.must(rangeQuery);
        }
        
        if (dissolutionDate != null && (dissolutionDate.getDateFrom() != null || dissolutionDate.getDateTo() != null)) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("dissolution-date");
            
            if (dissolutionDate.getDateFrom() != null) {
            	rangeQuery.from(dissolutionDate.getDateFrom().toString(), true);
            }

            if (dissolutionDate.getDateTo() != null) {
            	rangeQuery.to(dissolutionDate.getDateTo().toString(),true);
            }
            
            bool.must(rangeQuery);
        }
        
//        System.out.println(cq.getQuery());
        return cq;
    }
    
 }
    

