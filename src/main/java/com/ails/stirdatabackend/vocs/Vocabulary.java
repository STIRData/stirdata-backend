package com.ails.stirdatabackend.vocs;

//import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
//import org.reflections.Reflections;

public class Vocabulary {
	protected static Model model = ModelFactory.createDefaultModel();
	protected static NodeFactory nodeFactory = new NodeFactory();
	
	protected static Map<String,String> nsMap = new HashMap<>();
	
	static {

//		Reflections reflections = new Reflections("edu.ntua.isci.ac.lod.vocabularies");
//		for (Class<? extends Vocabulary> clazz : reflections.getSubTypesOf(Vocabulary.class)) {
//			try {
//				Field prefix = clazz.getDeclaredField("PREFIX");
//				Field namespace = clazz.getDeclaredField("NAMESPACE");
//				
//				nsMap.put((String)namespace.get(null), (String)prefix.get(null));
//
//			} catch (Exception ex) {
//			}
//		}
	}
	
	public static void register(String namespace, String prefix) {
		nsMap.put(namespace, prefix);
	}

	public static Model standarizeNamespaces(Model model) {

		Map<String, String> prefixMap = new HashMap<>();

		for (Map.Entry<String,String> entry : model.getNsPrefixMap().entrySet()) {
			String prefix = entry.getKey();
			String uri = entry.getValue();
//			if (prefix.matches("^ns[0-9]+$")) {
				String newPrefix = nsMap.get(uri);
				if (newPrefix != null) {
					prefixMap.put(newPrefix, uri);
				}
//			} else {
//				prefixMap.put(prefix, uri);
//			}
		}
		
		model.clearNsPrefixMap();
		model.setNsPrefixes(prefixMap);
		
		return model;

	}
	

}
