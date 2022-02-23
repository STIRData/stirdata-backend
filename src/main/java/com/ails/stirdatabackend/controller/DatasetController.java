package com.ails.stirdatabackend.controller;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryConfiguration;
import com.ails.stirdatabackend.model.Dimension;
import com.ails.stirdatabackend.model.Statistic;
import com.ails.stirdatabackend.payload.CountryResponse;
import com.ails.stirdatabackend.payload.Interval;
import com.ails.stirdatabackend.repository.StatisticsRepository;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/datasets")
@Tag(name = "Dataset API")
public class DatasetController {

	@Autowired
	@Qualifier("labels-cache")
	private Cache labelsCache;

	@Autowired
	@Qualifier("prefixes")
	private Set<URIDescriptor> prefixes;

    @Autowired
    @Qualifier("country-configurations")
    private Map<String, CountryConfiguration> countryConfigurations;
    
    @Autowired
    StatisticsRepository statisticsRepository;
    
	@GetMapping(produces = "application/json")
	public ResponseEntity<?> info()  {
		List<CountryResponse> results = new ArrayList<>();
			
		for (CountryConfiguration cc : countryConfigurations.values()) {
			CountryResponse ci = new CountryResponse();
			ci.setCountry(cc.getCountryCode(), cc.getCountryLabel());
			
			if (cc.isNace()) {
				ci.addActivity(Code.naceRev2Namespace);
			}
			if (cc.isNuts()) {
				ci.addPlace(Code.nutsNamespace);
			}
			if (cc.isLau()) {
				ci.addPlace(Code.lauNamespace);
			}

			if (cc.isFoundingDate()) {
				ci.setFoundingDate(new Interval(cc.getFoundingDateFrom(), cc.getFoundingDateTo()));
			}
			
			if (cc.isDissolutionDate()) {
				ci.setDissolutionDate(new Interval(cc.getDissolutionDateFrom(), cc.getDissolutionDateTo()));
			}
			
			ci.setSparqlEndpoint(cc.getDataEndpoint());
			ci.setLastUpdated(cc.getLastUpdated());
			ci.setSource(cc.getSource());
			
			List<Statistic> list = statisticsRepository.findByCountryAndDimension(cc.getCountryCode(), Dimension.DATA);
			if (!list.isEmpty()) {
				ci.setLegalEntityCount(list.get(0).getCount());
			}
			String acc = cc.getAccrualPeriodicity();
			if (acc.startsWith("http://publications.europa.eu/resource/authority/frequency/DAILY")) {
				ci.setAccrualPeriodicity(acc.substring("http://publications.europa.eu/resource/authority/frequency/".length()).toLowerCase());
			}
			
			results.add(ci);
		}
		
		Collections.sort(results, new Comparator<CountryResponse>() {

			@Override
			public int compare(CountryResponse o1, CountryResponse o2) {
				if (o1.getCountry() == null) {
					return -1;
				} else if (o2.getCountry() == null) {
					return 1;
				}
				return o1.getCountry().getLabel().compareTo(o2.getCountry().getLabel());
			}
			
		});
		
		return ResponseEntity.ok(results);
	}
	
	
	@GetMapping(value = "/label", produces = "application/json")
	public String label(@RequestParam String resource)  {

		Element e = labelsCache.get(resource);
		if (e != null) {
			return (String)e.getObjectValue();
		}
		
		Writer sw = new StringWriter();
		
		try {
			if (!(resource.startsWith("http://") || resource.startsWith("https://"))) {
				return sw.toString();
			}
		
			URIDescriptor prefix = URIDescriptor.findPrefix(resource, prefixes);
			if (prefix == null) {
				return sw.toString();
			}
			

			String sparql = 						
						"CONSTRUCT { " + "  <" + resource + "> <" + prefix.getLabelProperty() + "> ?label } " +
						"WHERE { " +
						"  <" + resource + "> <" + prefix.getLabelProperty() + "> ?label } " ;				
										
				
//			System.out.println(resource);
//			System.out.println(prefix);
//			System.out.println(vi.getGraph() + " " + vi.getEndpoint());
//			System.out.println(QueryFactory.create(sparql, Syntax.syntaxSPARQL_11));
			
			try (QueryExecution qe = QueryExecutionFactory.sparqlService(prefix.getEndpoint(), QueryFactory.create(sparql, Syntax.syntaxSPARQL_11))) {
				Model model = qe.execConstruct();
	
				RDFDataMgr.write(sw, model, RDFFormat.JSONLD_EXPAND_PRETTY);
			}
			
			labelsCache.put(new Element(resource, sw.toString()));
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		
		return sw.toString();
	}	
}
