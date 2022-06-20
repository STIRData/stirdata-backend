package com.ails.stirdatabackend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.ails.stirdatabackend.model.PlaceDB;

public class PlaceNode {
	private PlaceDB node;
	private List<PlaceNode> next;
	
	private int count;
	
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
			s = s + node.getCode() + " : " + count + "\n";
		}
		
		if (next != null) {
			for (PlaceNode pn : next) {
				s += pn.toString(level + 1);
			}
		}
		
		return s;
	}
	
	public void zero() {
		count = 0;
		if (next != null) {
			for (PlaceNode pn : next) {
				pn.zero();
			}
		}
	}
	
	public void sum() {
		if (next != null) {
			for (PlaceNode pn : next) {
				pn.sum();
				
				count += pn.count;
			}
		}
	}	
	
	public void leaves(Set<PlaceNode> set) {
		if (next == null) {
			set.add(this);
		} else {
			for (PlaceNode pn : next) {
				pn.leaves(set);
			}
		}
	}		
	
	public String toString() {
		return toString(0);
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

}
