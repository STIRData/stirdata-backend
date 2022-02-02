package com.ails.stirdatabackend.controller;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
public class URIDescriptor {

	private String prefix;
	private String labelProperty;
	private String endpoint;
	
	public URIDescriptor(String prefix, String labelProperty, String endpoint) {
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

}
