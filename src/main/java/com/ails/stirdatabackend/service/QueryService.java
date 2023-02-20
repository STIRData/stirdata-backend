package com.ails.stirdatabackend.service;

import com.ails.stirdatabackend.model.ActivityDB;
import com.ails.stirdatabackend.model.AddOn;
import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CompanyTypeDB;
import com.ails.stirdatabackend.model.CountryConfigurationsBean;
import com.ails.stirdatabackend.model.CountryDB;
import com.ails.stirdatabackend.model.PlaceDB;
import com.ails.stirdatabackend.payload.Address;
import com.ails.stirdatabackend.payload.CodeLabel;
import com.ails.stirdatabackend.payload.EndpointResponse;
import com.ails.stirdatabackend.payload.LegalEntity;
import com.ails.stirdatabackend.payload.Page;
import com.ails.stirdatabackend.payload.QueryResponse;
import com.ails.stirdatabackend.service.NutsService.PlaceSelection;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdError;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.writer.JsonLDWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
public class QueryService {

	private final static Logger logger = LoggerFactory.getLogger(QueryService.class);

    @Autowired
    private NutsService nutsService;

    @Autowired
    private NaceService naceService;

    @Autowired
    private CompanyTypeService companyTypeService;

    @Autowired
    private DataService dataService;

	@Autowired
	@Qualifier("country-configurations")
    private CountryConfigurationsBean countryConfigurations;
	
	@Autowired
	@Qualifier("model-jsonld-context")
    private JsonLDWriteContext context;
	
    @Value("${page.size}")
    private int pageSize;

    @Autowired
	@Qualifier("country-addons")
    private Map<String, Map<String,AddOn>> addons;  
    
    public LegalEntity lookupEntity(String uri) {
    	CountryDB cc = dataService.findCountry(uri);
        
       	String sparqlConstruct = 
        		"CONSTRUCT { " + 
                        "  ?entity a <http://www.w3.org/ns/legal#LegalEntity> . " + 
                        "  ?entity <http://www.w3.org/ns/legal#legalName> ?entityName . " +
                        "  ?entity <http://www.w3.org/ns/legal#companyType> ?companyType . " +
                        "  ?entity <http://www.w3.org/ns/legal#companyActivity> ?nace . " +
                		"  ?entity <http://www.w3.org/ns/legal#registeredAddress> ?address . ?address ?ap ?ao . " + 
                        "  ?entity <https://schema.org/foundingDate> ?foundingDate . " +	           		
                        "  ?entity <https://schema.org/leiCode> ?leiCode . } " +
        		" WHERE { " +
                cc.getEntitySparql() + " " +
                "OPTIONAL { " + cc.getLegalNameSparql() + " } " + 
                (cc.isDissolutionDate() ? cc.getActiveSparql() : "") + " " +
                "OPTIONAL { " + cc.getAddressSparql() + " ?address ?ap ?ao . } " +
	            "OPTIONAL { " + cc.getCompanyTypeSparql() + " } " +
	            "OPTIONAL { " + cc.getNaceSparql() + " } " +
                "OPTIONAL { " + cc.getFoundingDateSparql() + " } " +
                "OPTIONAL { " + cc.getLeiCodeSparql() + " } " +
                "VALUES ?entity { <" + uri + "> } } ";

       	LegalEntity entity = null;
       	
//       	System.out.println(QueryFactory.create(sparqlConstruct, Syntax.syntaxARQ));
       	
       	try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), QueryFactory.create(sparqlConstruct, Syntax.syntaxARQ))) {
            Model model = qe.execConstruct();
                
            Map<String, Object> jn = (Map)JsonLDWriter.toJsonLDJavaAPI((RDFFormat.JSONLDVariant)RDFFormat.JSONLD_COMPACT_PRETTY.getVariant(), DatasetFactory.wrap(model).asDatasetGraph(), null, null, context);
//            jn.put("@context", "https://dev.stirdata.eu/api/data/context/stirdata.jsonld");
            
            List<Map<String, Object>> graph = (List)jn.get("@graph");
            if (graph == null) {
            	graph = new ArrayList<>();
            	graph.add(jn);
            }
                
            Map<String, Address> addresses = null;
            addresses = new HashMap<>();
	                
            for (Map<String, Object> entry : graph) {
            	if (entry.get("@type").equals("http://www.w3.org/ns/locn#Address")) {
            		addresses.put((String)entry.get("@id"), createAddressFromJsonld(entry, cc));
            	}
            }

            for (Map<String, Object> entry : graph) {
            	if (entry.get("@type").equals("http://www.w3.org/ns/legal#LegalEntity")) {
            		entity = new LegalEntity((String)entry.get("@id"));
            		createLegalEntityFromJsonld(entity, entry, cc, addresses);
            	}
            }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
        }
       	
        Map<String,AddOn> countryAddons = addons.get(cc.getCode());
        if (countryAddons != null) {
        	List list = new ArrayList<>();
        	for (Map.Entry<String, AddOn> entry : countryAddons.entrySet()) {
        		Object res = entry.getValue().ask(uri);
        		if (res != null) {
        			list.add(res);
        		}
        	}
        	
        	if (!list.isEmpty()) {
        		entity.setAddOns(list);
        	}
        }
       	
        
        return entity;
    }

	
    public List<QueryResponse> paginatedQuery(List<Code> nutsLauCodes, List<Code> naceCodes, Code founding, Code dissolution, int page, boolean details) {
        
    	List<QueryResponse> responseList = new ArrayList<>();
        
//    	System.out.println(nutsLauCodes);
    	
    	Map<CountryDB, PlaceSelection> countryPlaceMap;
    	if (nutsLauCodes != null) {
    		countryPlaceMap = nutsService.getEndpointsByNuts(nutsLauCodes);
    	} else {
    		countryPlaceMap = nutsService.getEndpointsByNuts(); 
    	}
    	
        int offset = (page - 1) * pageSize;
        
//        ObjectMapper mapper = new ObjectMapper();
        
        for (Map.Entry<CountryDB, PlaceSelection> ccEntry : countryPlaceMap.entrySet()) {
        	CountryDB cc = ccEntry.getKey();
        	PlaceSelection places = ccEntry.getValue();
        	
//        	System.out.println(places);
//        	System.out.println(naceCodes);
        	
        	List<String> naceLeafUris = cc.getNaceEndpoint() == null ? null : naceService.getLocalNaceLeafUris(cc, naceCodes);
        	List<String> nutsLeafUris = places == null ? null : nutsService.getLocalNutsLeafUrisDB(cc, places); 
        	List<String> lauUris = places == null ? null : nutsService.getLocalLauUris(cc, places);
        	
//        	System.out.println("NUTSLEAFURIS " + nutsLeafUris);
//        	int count = 0;
        	
            Page pg = new Page();
            pg.setPageNumber(page);

            QueryResponse qr = new QueryResponse();
            qr.setCountry(new CodeLabel(cc.getCode(), cc.getLabel()));
            qr.setPage(pg);

//            System.out.println("NACELEAFURIS " +naceLeafUris);
            
        	if ((nutsLeafUris != null && nutsLeafUris.size() == 0 && lauUris != null && lauUris.size() == 0) || 
        			(naceLeafUris != null && naceLeafUris.size() == 0) || (!cc.isNace() && naceCodes != null)) {
        		pg.setPageSize(0);
        		qr.setLegalEntities(new ArrayList<>());
        		responseList.add(qr);
        		continue;
        	}
        	
        	//could use statistics table instead of this if available
        	
//        	System.out.println(countQuery);
        	SparqlQuery sparql = SparqlQuery.buildCoreQuery(cc, true, true, nutsLeafUris, lauUris, naceLeafUris, founding, dissolution);
        	
        	if (sparql != null)  {// if null it is an empty query
	            if (page == 1) {
	            	String countQuery = sparql.countSelectQuery();
		            	
//	            	System.out.println(QueryFactory.create(countQuery));
//	              System.out.println(cc.getDataEndpoint());
		             
	                try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), QueryFactory.create(countQuery, Syntax.syntaxARQ))) {
	                    ResultSet rs = qe.execSelect();
	                    while (rs.hasNext()) {
	                        QuerySolution sol = rs.next();
	                        pg.setTotalResults(sol.get("count").asLiteral().getInt());
	                    }
	                }
	            }
	            
	
	            String query = sparql.allSelectQuery(offset, pageSize);
	            
//	           System.out.println(QueryFactory.create(query));
	
	            Map<String, LegalEntity> companies = new LinkedHashMap<>();
	
	//            StringWriter sw = new StringWriter();
	            try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), QueryFactory.create(query, Syntax.syntaxARQ))) {
	                ResultSet rs = qe.execSelect();
	                while (rs.hasNext()) {
	                    QuerySolution sol = rs.next();
	                    String uri = sol.get("entity").asResource().toString();
	                    companies.put(uri, new LegalEntity(uri));
	                }
	            }
	            
	//            System.out.println(companies);
	
	            if (!companies.isEmpty()) {
	            	
	            	String values = "";
	                for (String uri : companies.keySet()) {
	                    values += "<" + uri + "> ";
	                 }
	
	            	String sparqlConstruct = null;
	            	
	            	if (details) {
	            		sparqlConstruct = 
	                		"CONSTRUCT { " + 
	                                "  ?entity a <http://www.w3.org/ns/legal#LegalEntity> . " + 
	                                "  ?entity <http://www.w3.org/ns/legal#legalName> ?entityName . " +
	                                "  ?entity <http://www.w3.org/ns/legal#companyType> ?companyType . " +
	                                "  ?entity <http://www.w3.org/ns/legal#companyActivity> ?nace . " +
	                        		"  ?entity <http://www.w3.org/ns/legal#registeredAddress> ?address . ?address ?ap ?ao . " + 
	                                "  ?entity <https://schema.org/foundingDate> ?foundingDate . }" +	           		
		            		" WHERE { " +
	                        cc.getEntitySparql() + " " +
	                        "OPTIONAL { " + cc.getLegalNameSparql() + " } " + 
	                        (cc.isDissolutionDate() ? cc.getActiveSparql() : "") + " " +
	                        "OPTIONAL { " + cc.getAddressSparql() + " ?address ?ap ?ao . } " + 
	        	            "OPTIONAL { " + cc.getCompanyTypeSparql() + " } " +
	        	            "OPTIONAL { " + cc.getNaceSparql() + " } " +
		                    "OPTIONAL { " + cc.getFoundingDateSparql() + " } " +
		                    "VALUES ?entity { " + values + "} } ";
	            	} else {
	            		sparqlConstruct =
		            		"CONSTRUCT { " + 
		                            "  ?entity a <http://www.w3.org/ns/legal#LegalEntity> . " + 
		                            "  ?entity <http://www.w3.org/ns/legal#legalName> ?entityName . " +
		                            "  ?entity <https://schema.org/foundingDate> ?foundingDate . }" +	           		
		            		" WHERE { " +
		                    cc.getEntitySparql() + " " +
		                    cc.getLegalNameSparql() + " " + 
		                    "OPTIONAL { " + cc.getFoundingDateSparql() + " } " +
		                    "VALUES ?entity { " + values + "} } ";
	            	}
		
//	                System.out.println(QueryFactory.create(sparqlConstruct));
		            try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), QueryFactory.create(sparqlConstruct, Syntax.syntaxARQ))) {
		                Model model = qe.execConstruct();
		                
		                Map<String,Object> jn = (Map)JsonLDWriter.toJsonLDJavaAPI((RDFFormat.JSONLDVariant)RDFFormat.JSONLD_COMPACT_PRETTY.getVariant(), DatasetFactory.wrap(model).asDatasetGraph(), null, null, context);
		                
		                List<Map<String, Object>> graph = (List)jn.get("@graph");
		                
		                Map<String, Address> addresses = null;
		                if (details) {
			                addresses = new HashMap<>();
			                
			                for (Map<String, Object> entry : graph) {
			                	if (entry.get("@type").equals("http://www.w3.org/ns/locn#Address")) {
			                		addresses.put((String)entry.get("@id"), createAddressFromJsonld(entry, cc));
			                	}
			                }
	
			                for (Map<String, Object> entry : graph) {
			                	if (entry.get("@type").equals("http://www.w3.org/ns/legal#LegalEntity")) {
			                		createLegalEntityFromJsonld(companies.get((String)entry.get("@id")), entry, cc, addresses);
			                	}
			                }
		                } else {
			                for (Map<String, Object> entry : graph) {
		                		createLegalEntityFromJsonld(companies.get((String)entry.get("@id")), entry, cc, addresses);
			                }
		                }
	
		            } catch (IOException e) {
		    			// TODO Auto-generated catch block
		    			e.printStackTrace();
		            }
	
		            pg.setPageSize(companies.size());
		            qr.setLegalEntities(new ArrayList<>(companies.values()));
	
		            responseList.add(qr);
		            
	            } else {
	                pg.setPageSize(0);
	                qr.setLegalEntities(new ArrayList<>());
	                
	                responseList.add(qr);
	            }
        	} else {
                pg.setPageSize(0);
                qr.setLegalEntities(new ArrayList<>());
                
                responseList.add(qr);
            } 
        }
        
        return responseList;
    }
    
    public List<EndpointResponse> groupedQuery(List<Code> nutsLauCodes, List<Code> naceCodes, Code founding, Code dissolution, boolean gnace, boolean gnuts3) {
        
    	List<EndpointResponse> responseList = new ArrayList<>();
//        
    	Map<CountryDB, PlaceSelection> countryPlaceMap;
    	if (nutsLauCodes != null) {
    		countryPlaceMap = nutsService.getEndpointsByNuts(nutsLauCodes);
    	} else {
    		countryPlaceMap = nutsService.getEndpointsByNuts(); 
    	}
    	
        ObjectMapper mapper = new ObjectMapper();

        for (Map.Entry<CountryDB, PlaceSelection> ccEntry : countryPlaceMap.entrySet()) {
        	CountryDB cc = ccEntry.getKey();
        	PlaceSelection places = ccEntry.getValue();

        	boolean nace = gnace;
            boolean nuts3 = gnuts3;
//	        boolean lau = glau;
            
        	List<String> naceLeafUris = cc.getNaceEndpoint() == null ? null : naceService.getLocalNaceLeafUris(cc, naceCodes);
        	List<String> nutsLeafUris = places == null ? null : nutsService.getLocalNutsLeafUrisDB(cc, places); 
        	List<String> lauUris = places == null ? null : nutsService.getLocalLauUris(cc, places);
        	
        	if (naceLeafUris == null) {
        		nace = false;
        	}
        	
        	if (nutsLeafUris == null) {
        		nuts3 = false;
        	}
        	
//	        	if (lauUrisList == null) {
//	        		lau = false;
//	        	}

        	SparqlQuery sparql = SparqlQuery.buildCoreQuery(cc, true, false, nutsLeafUris, lauUris, naceLeafUris, founding, dissolution); 

        	if ((nutsLeafUris != null && nutsLeafUris.size() == 0) || (naceLeafUris != null && naceLeafUris.size() == 0)) {
        		responseList.add(new EndpointResponse(cc.getLabel(), mapper.createArrayNode(), 0, null));
//        		return responseList;
        		continue;
        	}
        	
            
            String query = sparql.groupBySelectQuery(nuts3, nace);
//	            System.out.println(sparql);
//            System.out.println(QueryFactory.create(query));

            String json;
            try (QueryExecution qe = QueryExecutionFactory.sparqlService(cc.getDataEndpoint(), query)) {
                ResultSet rs = qe.execSelect();
                ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                ResultSetFormatter.outputAsJSON(outStream, rs);
                json = outStream.toString();
            }
            
            try {
                responseList.add(new EndpointResponse(cc.getLabel(), mapper.readTree(json), 0, null));
            } catch (Exception e) {
                e.printStackTrace();
//                return null;
                continue;
            }
        }
        	        
        return responseList;
    }
    
	public LegalEntity createLegalEntityFromJsonld(LegalEntity lg, Map<String, Object> map, CountryDB cc, Map<String, Address> addressMap) {
		
		Object legalNameObj = map.get("legalName");
		if (legalNameObj != null) {
    		if (legalNameObj instanceof String) {
    			lg.addLegalName((String)legalNameObj);
    		} else if (legalNameObj instanceof Map) {
    			lg.addLegalName((String)((Map)legalNameObj).get("@value"), (String)((Map)legalNameObj).get("@language"));
    		} else if (legalNameObj instanceof List) {
				for (Object s : (List)legalNameObj) {
		    		if (s instanceof String) {
		    			lg.addLegalName((String)s);
		    		} else if (s instanceof Map) {
		    			lg.addLegalName((String)((Map)s).get("@value"), (String)((Map)s).get("@language"));
		    		}
				}
			}
		}
		
		Object companyTypeObj = map.get("companyType");
		if (companyTypeObj != null) {
			if (companyTypeObj instanceof String) {
				if (cc.getCompanyTypePrefix() != null) {
					CompanyTypeDB companyType = companyTypeService.getByCode(Code.fromCompanyTypeUri((String)companyTypeObj, cc));
					if (companyType != null) {
						lg.setCompanyType(new CodeLabel(companyType.getCode().toString(), companyType.getLabel(cc.getPreferredCompanyTypeLanguage()), (String)companyTypeObj));
					}
				}
				
//			} else if (companyTypeObj instanceof List) {
//				for (Object s : (List)companyTypeObj) {
//					CompanyTypeDB companyType = companyTypeService.getByCode(Code.fromCompanyTypeUri((String)s, cc));
//
//					if (companyType != null) {
//						lg.setCompanyType(new CodeLabel(companyType.getCode().toString(), companyType.getLabel(cc.getPreferredCompanyTypeLanguage()), (String)s));
//					}
//
//				}
			}
		}
		
		Object leiCode = map.get("leiCode");
		if (leiCode != null) {
    		if (leiCode instanceof String) {
    			lg.setLeiCode((String)leiCode);
    		} else if (legalNameObj instanceof List) {
				for (Object s : (List)leiCode) {
		    		if (s instanceof String) {
		    			lg.setLeiCode((String)s);
		    		}
				}
			}
		}
		
		Object registeredAddressObj = map.get("registeredAddress");
		if (registeredAddressObj != null) {
			if (registeredAddressObj instanceof String) {
				lg.addRegisteredAddress(addressMap.get(registeredAddressObj));
			} else if (registeredAddressObj instanceof List) {
				for (Object s : (List)registeredAddressObj) {
					lg.addRegisteredAddress(addressMap.get((String)s));
				}
			}
		}
		
		Object companyActivityObj = map.get("companyActivity");
		if (companyActivityObj != null) {
			if (companyActivityObj instanceof String) {
				
				ActivityDB activity = naceService.getByCode(Code.fromNaceUri((String)companyActivityObj, cc));
				if (activity != null) {
//					lg.addCompanyActivity(new CodeLabel(activity.getCode().toString(), activity.getLabel(cc.getPreferredNaceLanguage()), (String)companyActivityObj));
					if (activity.getLevel() <= 4) {
						activity = activity.getExactMatch() != null ? activity.getExactMatch() : activity; 
						lg.addCompanyActivity(new CodeLabel(activity.getCode().toString(), activity.getLabel("en"), activity.getCode().toUri()));
					} else {
						List<CodeLabel> codeLabels = new ArrayList<>();
						
						CodeLabel res = new CodeLabel(activity.getCode().toString(), activity.getLabel(cc.getPreferredNaceLanguage()), (String)companyActivityObj);
						codeLabels.add(res);
						
						while (activity.getLevel() > 4) {
							activity = activity.getParent();
							
							if (activity.getLevel() > 4) {
								CodeLabel res2 = new CodeLabel(activity.getCode().toString(), activity.getLabel(cc.getPreferredNaceLanguage()), activity.getCode().localNaceToUri(cc));
								codeLabels.add(res2);
								res = res2;
							} else {
								activity = activity.getExactMatch();
								CodeLabel res2 = new CodeLabel(activity.getCode().toString(), activity.getLabel("en"), activity.getCode().toUri());
								codeLabels.add(res2);
								res = res2;
							}
						}

						CodeLabel cl = codeLabels.get(codeLabels.size() - 1); 
						lg.addCompanyActivity(cl);
						for (int i = codeLabels.size() - 2; i >= 0; i--) {
							cl.setChild(codeLabels.get(i));
							cl = codeLabels.get(i);
						}
					}
					
				}
				
			} else if (companyActivityObj instanceof List) {
				for (Object s : (List)companyActivityObj) {
					ActivityDB activity = naceService.getByCode(Code.fromNaceUri((String)s, cc));

					if (activity != null) {
//						lg.addCompanyActivity(new CodeLabel(activity.getCode().toString(), activity.getLabel(cc.getPreferredNaceLanguage()), (String)s));
						if (activity.getLevel() <= 4) {
							activity = activity.getExactMatch() != null ? activity.getExactMatch() : activity;
							lg.addCompanyActivity(new CodeLabel(activity.getCode().toString(), activity.getLabel("en"), activity.getCode().localNaceToUri(cc)));
						} else {
							List<CodeLabel> codeLabels = new ArrayList<>();
							
//							activity = activity.getLevel4OrHigherNaceRev2Activity();
							CodeLabel res = new CodeLabel(activity.getCode().toString(), activity.getLabel(cc.getPreferredNaceLanguage()), (String)s);
//							lg.addCompanyActivity(res);
							codeLabels.add(res);
							
							while (activity.getLevel() > 4) {
								activity = activity.getParent();
								
								if (activity.getLevel() > 4) {
									CodeLabel res2 = new CodeLabel(activity.getCode().toString(), activity.getLabel(cc.getPreferredNaceLanguage()), activity.getCode().localNaceToUri(cc));
									codeLabels.add(res2);
									res = res2;
								} else {
									activity = activity.getExactMatch();
									CodeLabel res2 = new CodeLabel(activity.getCode().toString(), activity.getLabel("en"), activity.getCode().toUri());
									codeLabels.add(res2);
									res = res2;
								}
							}

							CodeLabel cl = codeLabels.get(codeLabels.size() - 1); 
							lg.addCompanyActivity(cl);
							for (int i = codeLabels.size() - 2; i >= 0; i--) {
								cl.setChild(codeLabels.get(i));
								cl = codeLabels.get(i);
							}
						}
					}

				}
			}
		}
 		
		Object foundingDateObj = map.get("foundingDate");
		if (foundingDateObj != null) {
			lg.setFoundingDate(Date.valueOf((String)foundingDateObj));
		}

//		Object dissolutionDateObj = map.get("dissolutionDate");
//		if (dissolutionDateObj != null) {
//			lg.setDissolutionDate(Date.valueOf((String)dissolutionDateObj));
//		}
		
		return lg;
	}
	
	public Address createAddressFromJsonld(Map<String, Object> map, CountryDB cc) {
		
//		System.out.println(map);
		Address address = new Address();
		
		Object adminUnitL1Obj = map.get("adminUnitL1");
		if (adminUnitL1Obj != null) {
			if (adminUnitL1Obj instanceof String) {
				address.setAdminUnitL1((String)adminUnitL1Obj);
			} else if (adminUnitL1Obj instanceof Map) {
				address.setAdminUnitL1((String)((Map)adminUnitL1Obj).get("@value"));
			} else if (adminUnitL1Obj instanceof List) {
				Object element = ((List)adminUnitL1Obj).get(0); // choose first -- should fix
				if (element instanceof Map) {
					address.setAdminUnitL1((String)((Map)element).get("@value")); 
				} else if (element instanceof String) {
					address.setAdminUnitL1((String)element);
				}
			}
		}

		Object adminUnitL2Obj = map.get("adminUnitL2");
		if (adminUnitL2Obj != null) { 
			if (adminUnitL2Obj instanceof String) {
				address.setAdminUnitL2((String)adminUnitL2Obj);
			} else if (adminUnitL2Obj instanceof Map) {
				address.setAdminUnitL2((String)((Map)adminUnitL2Obj).get("@value"));
			} else if (adminUnitL2Obj instanceof List) {
				Object element = ((List)adminUnitL2Obj).get(0); // choose first -- should fix
				if (element instanceof Map) {
					address.setAdminUnitL2((String)((Map)element).get("@value")); 
				} else if (element instanceof String) {
					address.setAdminUnitL2((String)element);
				}
			}
		}

		Object fullAddressObj = map.get("fullAddress");
		
		System.out.println(fullAddressObj);
		
		if (fullAddressObj != null) {
			if (fullAddressObj instanceof String) {
				address.setFullAddress((String)fullAddressObj);
			} else if (fullAddressObj instanceof Map) {
				address.setFullAddress((String)((Map)fullAddressObj).get("@value"));
			} else if (fullAddressObj instanceof List) {
				Object element = ((List)fullAddressObj).get(0); // choose first -- should fix
				if (element instanceof Map) {
					address.setFullAddress((String)((Map)element).get("@value")); 
				} else if (element instanceof String) {
					address.setFullAddress((String)element);
				}
			}
		}
		
		Object postCodeObj = map.get("postCode");
		if (postCodeObj != null) {
			if (postCodeObj instanceof String) {
				address.setPostCode((String)postCodeObj);
			} else if (postCodeObj instanceof Map) {
				address.setPostCode((String)((Map)postCodeObj).get("@value"));
			} else if (postCodeObj instanceof List) {
				Object element = ((List)postCodeObj).get(0); // choose first -- should fix
				if (element instanceof Map) {
					address.setPostCode((String)((Map)element).get("@value")); 
				} else if (element instanceof String) {
					address.setPostCode((String)element);
				}
			}			
		}
		
		Object postNameObj = map.get("postName");
		if (postNameObj != null) {
			if (postNameObj instanceof String) {
				address.setPostName((String)postNameObj);
			} else if (postNameObj instanceof Map) {
				address.setPostName((String)((Map)postNameObj).get("@value"));
			} else if (postNameObj instanceof List) {
				Object element = ((List)postNameObj).get(0); // choose first -- should fix
				if (element instanceof Map) {
					address.setPostName((String)((Map)element).get("@value")); 
				} else if (element instanceof String) {
					address.setPostName((String)element);
				}
			}						
		}
		
		Object thoroughfareObj = map.get("thoroughfare");
		if (thoroughfareObj != null) {
			if (thoroughfareObj instanceof String) {
				address.setThoroughfare((String)thoroughfareObj);
			} else if (thoroughfareObj instanceof Map) {
				address.setThoroughfare((String)((Map)thoroughfareObj).get("@value"));
			} else if (thoroughfareObj instanceof List) {
				Object element = ((List)thoroughfareObj).get(0); // choose first -- should fix
				if (element instanceof Map) {
					address.setThoroughfare((String)((Map)element).get("@value")); 
				} else if (element instanceof String) {
					address.setThoroughfare((String)element);
				}
			}
		}
		
		Object locatorNameObj = map.get("locatorName");
		if (locatorNameObj != null) {
			if (locatorNameObj instanceof String) {
				address.setLocatorName((String)locatorNameObj);
			} else if (locatorNameObj instanceof Map) {
				address.setLocatorName((String)((Map)locatorNameObj).get("@value"));
			} else if (locatorNameObj instanceof List) {
				Object element = ((List)locatorNameObj).get(0); // choose first -- should fix
				if (element instanceof Map) {
					address.setLocatorName((String)((Map)element).get("@value")); 
				} else if (element instanceof String) {
					address.setLocatorName((String)element);
				}
			}		
		}
		
		Object locatorDesignatorObj = map.get("locatorDesignator");
		if (locatorDesignatorObj != null) {
			if (locatorDesignatorObj instanceof String) {
				address.setLocatorDesignator((String)locatorDesignatorObj);
			} else if (locatorDesignatorObj instanceof Map) {
				address.setLocatorDesignator((String)((Map)locatorDesignatorObj).get("@value"));
			} else if (locatorDesignatorObj instanceof List) {
				Object element = ((List)locatorDesignatorObj).get(0); // choose first -- should fix
				if (element instanceof Map) {
					address.setLocatorDesignator((String)((Map)element).get("@value")); 
				} else if (element instanceof String) {
					address.setLocatorDesignator((String)element);
				}				
			}		
		}
		
		Object adminUnitObj = map.get("adminUnit");
		if (adminUnitObj != null) {
			List adminUnitList = null;
			if (adminUnitObj instanceof String) {
				adminUnitList = new ArrayList<>();
				adminUnitList.add(adminUnitObj);
			} else if (adminUnitObj instanceof List) {
				adminUnitList = (List)adminUnitObj;
			}
			
			if (adminUnitList != null) {
				for (Object s : (List)adminUnitList) {
					PlaceDB nuts3 = nutsService.getByCode(Code.fromNutsUri((String)s));
					if (nuts3 != null) {
						address.setNuts3(new CodeLabel(nuts3.getCode().toString(), nuts3.getLatinName() != null ? nuts3.getLatinName() : nuts3.getNationalName(), (String)s));
						continue;
					}	
					
					PlaceDB lau = nutsService.getByCode(Code.fromLauUri((String)s, cc));
					if (lau != null) {
						address.setLau(new CodeLabel(lau.getCode().toString(), lau.getLatinName() != null ? lau.getLatinName() : lau.getNationalName(), (String)s));
						continue;
					}
				}
			}
		}	
		
		return address;
	}


}
