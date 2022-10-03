package org.DoTeLink.output;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import com.google.gson.JsonObject;

public class Similarity {
	private String productionMethodName;
	private String testMethodName;
	private Document sentence;
	private Document testCodeSnippet;
	private double similarity;

	public Similarity(String productionMethodName, String testMethodName, Document sentence, Document testCodeSnippet, double similarity) {
		this.productionMethodName = productionMethodName;
		this.testMethodName = testMethodName;
		this.sentence = sentence;
		this.testCodeSnippet = testCodeSnippet;
		this.similarity = similarity;
	}

	public void updateSimilarity(double similarity) {
		this.similarity = similarity;
	}

	public String getTestMethodName() {
		return this.testMethodName;
	}

	public String getProductionMethodName() {
		return this.productionMethodName;
	}

	public Document getSentence() {
		return this.sentence;
	}

	public Document getTestCodeSnippet() {
		return this.testCodeSnippet;
	}

	public double getSimilarity() {
		return this.similarity;
	}
}
