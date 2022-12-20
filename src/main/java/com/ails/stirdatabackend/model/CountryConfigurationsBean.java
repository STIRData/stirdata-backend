package com.ails.stirdatabackend.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CountryConfigurationsBean {
	private Map<String, CountryDB> map;
		
	public CountryConfigurationsBean() {
		map = new HashMap<>();
	}

	public Map<String, CountryDB> getMap() {
		return map;
	}

//	public void setMap(Map<String, CountryDB> map) {
//		this.map = map;
//	}

	public void put(String key, CountryDB value) {
		this.map.put(key, value);
	}
	
	public Set<String> keySet() {
		return map.keySet();
	}
	
	public Collection<CountryDB> values() {
		return map.values();
	}
	
	public CountryDB get(String key) {
		return map.get(key);
	}
	
	public CountryDB remove(String key) {
		return map.remove(key);
	}
	
	public boolean containsKey(String key) {
		return map.containsKey(key);
	}
}
