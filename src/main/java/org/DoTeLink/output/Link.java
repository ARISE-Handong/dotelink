package org.DoTeLink.output;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import com.google.gson.JsonObject;

public class Link {
	private String productionMethodName;
	private String testMethodName;
	private List<Map<Document, Document>> links;

	public Link(String productionMethodName, String testMethodName) {
		this.productionMethodName = productionMethodName;
		this.testMethodName = testMethodName;
		this.links = new ArrayList<>();
	}

	public boolean hasMethodRelation(String productionMethodName, String testMethodName) {
		return this.productionMethodName.equals(productionMethodName) && this.testMethodName.equals(testMethodName);
	}

	public boolean hasLink(Map<Document, Document> link) {
		Document s = (Document)link.keySet().toArray()[0];
		Document t = (Document)link.values().toArray()[0];

		for (Map<Document, Document> _link : this.links) {
			if (((Document)_link.keySet().toArray()[0]).equals(s) && ((Document)_link.values().toArray()[0]).equals(t))
				return true;
		}

		return false;
	}

	public void addAllLinks(List<Map<Document, Document>> links) {
		this.links.addAll(links);
	}

	public void addLink(Document sentence, Document testCodeSnippet) {
		Map<Document, Document> _link = new HashMap<>();
		_link.put(sentence, testCodeSnippet);
		this.links.add(_link);
	}

	public String getProductionMethodName() {
		return this.productionMethodName;
	}

	public String getTestMethodName() {
		return this.testMethodName;
	}

	public List<Map<Document, Document>> getLinks() {
		return this.links;
	}
}
