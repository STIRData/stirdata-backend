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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Set;

@RestController
@RequestMapping("/api/data")
public class DataController {

	@Autowired
	@Qualifier("labels-cache")
	private Cache labelsCache;

	@Autowired
	@Qualifier("prefixes")
	private Set<URIDescriptor> prefixes;

	
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
			
			try (QueryExecution qe = QueryExecutionFactory.sparqlService(prefix.getEndpoint().getSparqlEndpoint(), QueryFactory.create(sparql, Syntax.syntaxSPARQL_11))) {
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
