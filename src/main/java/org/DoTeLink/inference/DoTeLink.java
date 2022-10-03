package org.DoTeLink.inference;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.lang.reflect.Type;

import com.github.javaparser.ast.CompilationUnit;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

import org.DoTeLink.util.Inference;
import org.DoTeLink.output.Link;
import org.DoTeLink.output.Matrix;
import org.DoTeLink.output.Document;
import org.DoTeLink.output.Similarity;
import org.DoTeLink.output.MethodAssociationOutput;

import org.DoTeLink.util.MethodAssociation;
import org.DoTeLink.util.SimilarityMeasurement;

import java.lang.reflect.Type;

public class DoTeLink {
	public DoTeLink() {}

	public static JsonArray relate(JsonArray jSegProductionMethod, JsonArray jSegTestMethod, CompilationUnit testCodeCu) {
		List<MethodAssociationOutput> productionMethodAssociations =
					MethodAssociation.relateProductionMethodWise(jSegProductionMethod, jSegTestMethod, testCodeCu);
		Map<MethodAssociationOutput, List<Similarity>> productionMatrix = 
					SimilarityMeasurement.measureSentenceAndTestCodeSnippet(productionMethodAssociations);
		List<Link> sentenceWiseLinks = Inference.inferSentenceWise(productionMatrix);

		List<MethodAssociationOutput> testMethodAssociations = 
					MethodAssociation.relateTestMethodWise(jSegProductionMethod, jSegTestMethod, testCodeCu);
		Map<MethodAssociationOutput, List<Similarity>> testMatrix = 
					SimilarityMeasurement.measureSentenceAndTestCodeSnippet(testMethodAssociations);
		List<Link> testCodeSnippetWiseLinks = Inference.inferTestCodeSnippetWise(testMatrix);

		List<Link> links = Union(sentenceWiseLinks, testCodeSnippetWiseLinks);
		JsonArray jLinks = TransformOutputFormat(links);
		return jLinks;
	}

	static List<Link> Union(List<Link> sentenceWiseLinks, List<Link> testCodeSnippetWiseLinks) {
		Map<String, List<String>> methodPairs = new HashMap<String, List<String>>();
		for (Link senLink : sentenceWiseLinks) {
			String productionMethodName = senLink.getProductionMethodName();
			String testMethodName = senLink.getTestMethodName();
			if (!methodPairs.containsKey(productionMethodName))
				methodPairs.put(productionMethodName, new ArrayList<String>());
			methodPairs.get(productionMethodName).add(testMethodName);
		}

		List<Link> out = new ArrayList<Link>();
		for (Link testLink : testCodeSnippetWiseLinks) {
			String productionMethodName = testLink.getProductionMethodName();
			String testMethodName = testLink.getTestMethodName();

			if (methodPairs.containsKey(productionMethodName) && methodPairs.get(productionMethodName).contains(testMethodName)) {
				Link newLink = new Link(productionMethodName, testMethodName);
				newLink.addAllLinks(GetLinksByMethodPair(sentenceWiseLinks, productionMethodName, testMethodName));
			
				for (Map<Document, Document> link : testLink.getLinks()) {
					if (!newLink.hasLink(link)) {
						newLink.addLink((Document)link.keySet().toArray()[0], (Document)link.values().toArray()[0]);
					}
				}
				out.add(newLink);
			}
			else {
				out.add(testLink);
			}
		}
		return out;
	}

	static List<Map<Document, Document>> GetLinksByMethodPair(List<Link> sentenceWiseLinks, String productionMethodName, String testMethodName) {
		List<Map<Document, Document>> out = new ArrayList<>();
		for (Link senLink : sentenceWiseLinks) {
			if (senLink.hasMethodRelation(productionMethodName, testMethodName)) {
				out.addAll(senLink.getLinks());
			}
		}
		return out;
	}
	
	static JsonArray TransformOutputFormat(List<Link> methodPairs) {
		JsonArray jOut = new JsonArray();
		for (Link methodPair : methodPairs) {
			String productionMethodName = methodPair.getProductionMethodName();
			String testMethodName = methodPair.getTestMethodName();
			JsonObject jMethodPair = new JsonObject();
			jMethodPair.addProperty("productionMethod", productionMethodName);
			jMethodPair.addProperty("unitTestMethod", testMethodName);

			JsonArray jLinks = new JsonArray();
			List<Map<Document, Document>> links = methodPair.getLinks();
			for (Map<Document, Document> link : links) {
				Document sentence = (Document)link.keySet().toArray()[0];
				Document testCodeSnippet = (Document)link.values().toArray()[0];

				JsonObject jLink = new JsonObject();
				jLink.add("sentenceLocation", sentence.getLocation());
				jLink.add("testCodeLocation", testCodeSnippet.getLocation());

				jLinks.add(jLink);
			}
			jMethodPair.add("links", jLinks);
			jOut.add(jMethodPair);
		}

		return jOut;
	}
}
