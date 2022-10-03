package org.DoTeLink.output;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import com.google.gson.JsonObject;

public class Document {
	private String methodName;
	private String text;
	private JsonObject loc;
	private List<String> tokens;
	private Map<String, Double> weight;

	public Document() {}

	public Document(String methodName, String str, JsonObject loc) {
		this.methodName = methodName;
		this.text = str;
		this.loc = loc;
		this.tokens = new ArrayList<String>();
		this.weight = new HashMap<String, Double>();
	}

	public void addToken(String token) {
		this.tokens.add(token);
	}

	public void setWeight(String term, double w) {
		this.weight.put(term, w);
	}

	public List<String> getToken() {
		return this.tokens;
	}

	public Map<String, Double> getWeight() {
		return this.weight;
	}

	public String getText() {
		return this.text;
	}

	public String getMethodName() {
		return this.methodName;
	}

	public JsonObject getLocation() {
		return this.loc;
	}

	@Override
	public boolean equals(Object o) {
		JsonObject loc = ((Document)o).getLocation();
		String text = ((Document)o).getText();
		return this.loc.equals(loc);
	}
}
