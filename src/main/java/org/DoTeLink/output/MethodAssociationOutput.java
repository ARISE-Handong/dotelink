package org.DoTeLink.output;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;

import com.google.gson.JsonObject;

public class MethodAssociationOutput {
	private Map<JsonObject, Set<JsonObject>> relation;
	private JsonObject from;
	private Set<JsonObject> to;

	public MethodAssociationOutput() {
		this.to = new HashSet<JsonObject>();
	}

	public void addRelation(JsonObject from, Set<JsonObject> to) {
		this.from = from;
		this.to.addAll(to);
	}

	public Set<JsonObject> getProductionMethods() {
		if (this.from.has("productionMethod")) {
			Set<JsonObject> out = new HashSet<JsonObject>();
			out.add(this.from);
			return out;
		}			
		return this.to;
	}

	public Set<JsonObject> getTestMethods() {
		if (this.from.has("unitTestMethod")) {
			Set<JsonObject> out = new HashSet<JsonObject>();
			out.add(this.from);
			return out;
		}			
		return this.to;
	}

	public Map<JsonObject, Set<JsonObject>> getRelation() {
		return this.relation;
	}

	public JsonObject getFrom() {
		return this.from;
	}
	
	public Set<JsonObject> getTo() {
		return this.to;
	}
}
