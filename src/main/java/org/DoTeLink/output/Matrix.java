package org.DoTeLink.output;

import java.util.Map;
import java.util.HashMap;

import com.google.gson.JsonObject;

public class Matrix {
	private String productionMethodName;
	private String testMethodName;
	private Map<Document, Map<Document, Double>> similarityMatrix;

	public Matrix(String productionMethodName, String testMethodName, Document sentence, Document testCodeSnippet, double similarity) {
		this.productionMethodName = productionMethodName;
		this.testMethodName = testMethodName;
		this.similarityMatrix = new HashMap<>();
		this.similarityMatrix.put(sentence, new HashMap<>());
		this.similarityMatrix.get(sentence).put(testCodeSnippet, similarity);
	}

	public void setProductionMethodName(String name) {
		this.productionMethodName = name;
	}

	public void setTestMethodName(String name) {
		this.testMethodName = name;
	}

	public void setSimilarityMatrix(Document sentence, Document testCodeSnippet, double similarity) {
		if (!this.similarityMatrix.containsKey(sentence))
			this.similarityMatrix.put(sentence, new HashMap<>());
		this.similarityMatrix.get(sentence).put(testCodeSnippet, similarity);
	}

	public boolean hasMethodRelation(String productionMethodName, String testMethodName) {
		return this.productionMethodName == productionMethodName && this.testMethodName == testMethodName;
	}

	public void add(Document sentence, Document testCodeSnippet, double similarity) {
		if (!this.similarityMatrix.containsKey(sentence))
			this.similarityMatrix.put(sentence, new HashMap<Document, Double>());
		this.similarityMatrix.get(sentence).put(testCodeSnippet, similarity);
	}

	public Map<Document, Map<Document, Double>> getSimilarityMatrix() {
		return this.similarityMatrix;
	}
}
