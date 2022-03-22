package com.ails.stirdatabackend.service;

import java.util.ArrayList;
import java.util.List;

import com.ails.stirdatabackend.model.PlaceDB;

public class PlaceNode {
	private PlaceDB node;
	private List<PlaceNode> next;
	
	public PlaceNode() {
		
	}

	public PlaceNode(PlaceDB node) {
		this.node = node;
	}

	public void addChild(PlaceDB place) {
		if (next == null) {
			next = new ArrayList<>();
		}
		
		next.add(new PlaceNode(place));
	}
	
	public PlaceDB getNode() {
		return node;
	}
	
	public List<PlaceNode> getNext() {
		return next;
	}
	
	public String toString(int level) {
		String s = "";
		for (int i = 0; i < level; i++) {
			s += "  ";
		}
		if (node != null) {
			s = s + node.getCode() + "\n";
		}
		
		if (next != null) {
			for (PlaceNode pn : next) {
				s += pn.toString(level + 1);
			}
		}
		
		return s;
	}
	
	public String toString() {
		return toString(0);
	}

}
