package com.ails.stirdatabackend.payload;


import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CodeLabel {

	public String code;
	public String label;
	public String uri;
	
	public String geometry;
	
	public CodeLabel(String code, String label) {
		this.code = code;
		this.label = label;
	}

	public CodeLabel(String code, String label, String uri) {
		this.code = code;
		this.label = label;
		this.uri = uri;
	}
	
	public int hashCode() {
		return code.hashCode() + label.hashCode() + uri.hashCode();
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof CodeLabel)) {
			return false;
		}
		
		CodeLabel cl = (CodeLabel)obj;
				
		return (code == cl.code || code != null && cl.code != null && cl.equals(cl.code)) &&
				(label == cl.label || label != null && cl.label != null && label.equals(cl.label)) &&
				(uri == cl.uri || uri != null && cl.uri != null && uri.equals(cl.uri));
	}

}