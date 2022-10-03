package org.DoTeLink.inference;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.lang.reflect.Type;

import com.github.javaparser.ast.CompilationUnit;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

import org.DoTeLink.util.MethodAssociation;
import org.DoTeLink.output.MethodAssociationOutput;

public class MethodAll {
	public MethodAll() {}

	public static JsonArray relate(JsonArray jSegProductionMethod, JsonArray jSegTestMethod, CompilationUnit testCodeCu) {
		List<MethodAssociationOutput> ma = MethodAssociation.relateProductionMethodWise(jSegProductionMethod, jSegTestMethod, testCodeCu);
		//List<MethodAssociationOutput> ma = MethodAssociation.relateTestMethodWise(jSegProductionMethod, jSegTestMethod, testCodeCu);

		JsonArray output = TransformOutputFormat(ma);
		return output;
	}

	//static JsonArray TransformOutputFormat(Map<JsonObject, Set<JsonObject>> methodAssociation) {
	static JsonArray TransformOutputFormat(List<MethodAssociationOutput> ma) {
		Type listType = new TypeToken<List<JsonObject>>() {}.getType();
		JsonArray jClazz = new JsonArray();

		for (MethodAssociationOutput relation : ma) {
			Set<JsonObject> productionMethods = relation.getProductionMethods();
			Set<JsonObject> testMethods = relation.getTestMethods();
			for (JsonObject productionMethod : productionMethods) {
				List<JsonObject> sentences = new Gson().fromJson(productionMethod.get("sentences"), listType);

				for (JsonObject testMethod : testMethods) {
					JsonObject jMethod = new JsonObject();
					jMethod.addProperty("productionMethod", productionMethod.get("productionMethod").getAsString());
					jMethod.addProperty("unitTestMethod", testMethod.get("unitTestMethod").getAsString());

					JsonArray links = new JsonArray();
					List<JsonObject> testCodeRegions = new Gson().fromJson(testMethod.get("testCodeRegion"), listType);
					for (JsonObject sentence : sentences) {
						for (JsonObject region : testCodeRegions) {
							JsonObject link = new JsonObject();
							link.add("sentenceLocation", sentence.get("sentenceLocation"));
							link.add("testCodeLocation", region.get("testCodeLocation"));

							links.add(link);
						}
					}
					jMethod.add("links", links);		

					jClazz.add(jMethod);
				}
			}
		}
		return jClazz;
	}
}
