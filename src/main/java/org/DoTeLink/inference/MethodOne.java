package org.DoTeLink.inference;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.lang.reflect.Type;

import com.github.javaparser.ast.CompilationUnit;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

import org.DoTeLink.util.MethodAssociation;
import org.DoTeLink.output.Link;
import org.DoTeLink.output.Document;
import org.DoTeLink.output.MethodAssociationOutput;

public class MethodOne {
	public MethodOne() {}

	public static JsonArray relate(JsonArray jSegProductionMethod, JsonArray jSegTestMethod, CompilationUnit testCodeCu) {
		List<MethodAssociationOutput> ma = MethodAssociation.relateProductionMethodWise(jSegProductionMethod, jSegTestMethod, testCodeCu);

		List<Link> randLink = GetRandomOneLinkPerMethodAssociation(ma);

		JsonArray output = TransformOutputFormat(randLink);
		return output;
	}

	static List<Link> GetRandomOneLinkPerMethodAssociation(List<MethodAssociationOutput> ma) {
		Random rand = new Random();
		List<Link> out = new ArrayList<>();
		Type listType = new TypeToken<List<JsonObject>>() {}.getType();

		for (MethodAssociationOutput relation : ma) {
			JsonObject productionMethod = relation.getFrom();
			String productionMethodName = productionMethod.get("productionMethod").getAsString();
			List<JsonObject> sentences = new Gson().fromJson(productionMethod.get("sentences"), listType);

			List<Link> links = new ArrayList<>();
			for (JsonObject testMethod : relation.getTo()) {
				String testMethodName = testMethod.get("unitTestMethod").getAsString();
				List<JsonObject> testCodeRegions = new Gson().fromJson(testMethod.get("testCodeRegion"), listType);

				for (JsonObject sentence : sentences) {
					for (JsonObject region : testCodeRegions) {
						Link __link = GenerateLink(productionMethodName, testMethodName, sentence, region);
						links.add(__link);
					}
				}
			}
			if (links.size() > 0) {
				int randomIdx = rand.nextInt(links.size());
				out.add(links.get(randomIdx));
			}
		}
		return out;
	}

	static Link GenerateLink(String productionMethodName, String testMethodName, JsonObject sentence, JsonObject testCodeRegion) {
		Link out = new Link(productionMethodName, testMethodName);
		Document senDoc = new Document(productionMethodName, sentence.get("sentenceText").getAsString(), sentence.get("sentenceLocation").getAsJsonObject());
		Document tcsDoc = new Document(testMethodName, testCodeRegion.get("slice").getAsString(), testCodeRegion.get("testCodeLocation").getAsJsonObject());
		out.addLink(senDoc, tcsDoc);
		return out;
	}

	static JsonArray TransformOutputFormat(List<Link> randLink) {
		JsonArray jOut = new JsonArray();
		for (Link methodPair : randLink) {
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
