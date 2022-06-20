package com.ails.stirdatabackend.payload;

import java.sql.Date;
import java.util.List;

public class CompanyResponse {

	private String legalName;
	private List<String> activities;
	private Date foundingDate;
	private Date dissolutionDate;
//	
//    "  ?entity a <http://www.w3.org/ns/regorg#RegisteredOrganization> . " + 
//    "  ?entity <http://www.w3.org/ns/regorg#legalName> ?entityName . " +
//    "  ?entity <http://www.w3.org/ns/regorg#orgActivity> ?nace . " +
//	"  ?entity <http://www.w3.org/ns/org#siteAddress> ?address . ?address ?ap ?ao . " + 
//    "  ?entity <https://schema.org/foundingDate> ?foundingDate . }" +	 
}
