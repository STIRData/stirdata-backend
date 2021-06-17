package com.ails.stirdatabackend.controller;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ails.stirdatabackend.model.SparqlEndpoint;

public class URIDescriptor {

	private String prefix;
	private String labelProperty;
	private SparqlEndpoint endpoint;
	
	public URIDescriptor(String prefix, String labelProperty, SparqlEndpoint endpoint) {
		this.prefix = prefix;
		this.labelProperty = labelProperty;
		this.endpoint = endpoint;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

    public static URIDescriptor findPrefix(String c, Set<URIDescriptor> prefixes) {
    	URIDescriptor result = null;
    	
    	for (URIDescriptor prefix : prefixes) {
    		Pattern p = Pattern.compile("^(" + Matcher.quoteReplacement(prefix.getPrefix()) + ")");
    		
    		Matcher m = p.matcher(c);
    		if (m.find()) {
    			String f = m.group(1);
    			if (result == null || f.length() > result.getPrefix().length()) {
    				result = prefix;
    			}
    		}
    	}

    	return result;
    }

	public SparqlEndpoint getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(SparqlEndpoint endpoint) {
		this.endpoint = endpoint;
	}

	public String getLabelProperty() {
		return labelProperty;
	}

	public void setLabelProperty(String labelProperty) {
		this.labelProperty = labelProperty;
	}
	
}
