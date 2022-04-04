package com.ails.stirdatabackend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.ails.stirdatabackend.model.Code;
import com.ails.stirdatabackend.model.CountryDB;

public class PlaceTree {
	
    private PlaceNode root;
    
	private Map<Code, PlaceNode> leavesMap;
	
	public PlaceTree(NutsService nutsService, CountryDB cc) {
		root = nutsService.buildPlaceTree(cc, false);
		
		Set<PlaceNode> leaves = new HashSet<>();
		root.leaves(leaves);
		
		leavesMap = new HashMap<>();
		
		for (PlaceNode pn : leaves) {
			leavesMap.put(pn.getNode().getCode(), pn);
		}
	}

	public PlaceNode getRoot() {
		return root;
	}

	public void setRoots(PlaceNode root) {
		this.root = root;
	}

	public Map<Code, PlaceNode> getLeaves() {
		return leavesMap;
	}

	public void setLeaves(Map<Code, PlaceNode> leavesMap) {
		this.leavesMap = leavesMap;
	}
}
