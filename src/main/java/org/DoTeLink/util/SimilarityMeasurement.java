package org.DoTeLink.util;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.lang.reflect.Type;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

import org.DoTeLink.output.Matrix;
import org.DoTeLink.output.Document;
import org.DoTeLink.output.Similarity;
import org.DoTeLink.output.MethodAssociationOutput;

public class SimilarityMeasurement {

	SimilarityMeasurement() {}

	public static Map<MethodAssociationOutput, List<Similarity>> measureSentenceAndTestCodeSnippet(List<MethodAssociationOutput> ma) {
		List<String> stopwords = new ArrayList<String>();
		List<String> primitiveType = Arrays.asList("byte", "short", "int", "long", "float", "double", "boolean", "char");
		try {
			BufferedReader br = new BufferedReader(new FileReader("./stopwords"));
			String word = br.readLine();
			while (word != null) {
				stopwords.add(word);
				word = br.readLine();
			}
		}
		catch (IOException e) { }

		Map<MethodAssociationOutput, List<Similarity>> out = new HashMap<>();

		for (MethodAssociationOutput relation : ma) {
			Set<JsonObject> productionMethods = relation.getProductionMethods();
			Set<JsonObject> testMethods = relation.getTestMethods();

			List<Document> sentences = TFIDF.preProcessing(GetSentences(productionMethods), stopwords);
			List<Document> testCodeSnippets = TFIDF.preProcessing(GetTestCodeSnippets(testMethods), primitiveType);
			Set<String> vocab = TFIDF.getVocabulary(sentences, testCodeSnippets);
			List<Document> sentenceVectors = TFIDF.vectorize(sentences, vocab);
			List<Document> testCodeSnippetVectors = TFIDF.vectorize(testCodeSnippets, vocab);

			List<Similarity> matrix = new ArrayList<Similarity>();
			for (Document sv : sentenceVectors) {
				String productionMethodName = sv.getMethodName();
				for (Document tv : testCodeSnippetVectors) {
					String testMethodName = tv.getMethodName();
					double similarity = CosineSimilarity(sv, tv, vocab);
					matrix.add(new Similarity(productionMethodName, testMethodName, sv, tv, similarity));
				}
			}

			out.put(relation, matrix);
		}

		return out;
	}

	static double GetMaxSimilarity(List<Similarity> matrices) {
		double max = 0.0;
		for (Similarity mat : matrices) {
			if (max < mat.getSimilarity()) {
				max = mat.getSimilarity();
			}
		}
		return max;
	}

	static double GetMinSimilarity(List<Similarity> matrices) {
		double min = Double.MAX_VALUE;
		for (Similarity mat : matrices) {
			double sim = mat.getSimilarity();
			if (sim < min)
				min = sim;
		}
		return min;
	}

	static double CosineSimilarity(Document sentence, Document testCodeSnippet, Set<String> vocab) {
		double sentenceNorm = 0.0;
		Map<String, Double> sentenceWeight = sentence.getWeight();
		for (String token : sentenceWeight.keySet()) {
			sentenceNorm += Math.pow(sentenceWeight.get(token), 2);
		}

		double tcsNorm = 0.0;
		Map<String, Double> tcsWeight = testCodeSnippet.getWeight();
		for (String token : tcsWeight.keySet()) {
			tcsNorm += Math.pow(tcsWeight.get(token), 2);
		}

		double dotProduct = 0.0;
		List<String> sentenceToken = sentence.getToken();
		List<String> tcsToken = testCodeSnippet.getToken();
		for (String v : vocab) {
			if (sentenceToken.contains(v) && tcsToken.contains(v)) {
				double w1 = sentenceWeight.get(v);
				double w2 = tcsWeight.get(v);
				dotProduct += (w1 * w2);
			}
		}

		double out = 0.0;
		if (sentenceNorm != 0.0 && tcsNorm != 0.0) {
			out = (dotProduct / (Math.sqrt(sentenceNorm) * Math.sqrt(tcsNorm)));
		}

		return out;
	}

	static List<Document> GetSentences(Set<JsonObject> productionMethods) {
		List<Document> out = new ArrayList<Document>();

		Type listType = new TypeToken<List<JsonObject>>() {}.getType();
		for (JsonObject productionMethod : productionMethods) {
			String name = productionMethod.get("productionMethod").getAsString();
			List<JsonObject> sentences = new Gson().fromJson(productionMethod.get("sentences"), listType);
			for (JsonObject sentence : sentences) {
				out.add(new Document(name, sentence.get("sentenceText").getAsString(), sentence.get("sentenceLocation").getAsJsonObject()));
			}
		}		
		return out;
	}	

	static List<Document> GetTestCodeSnippets(Set<JsonObject> testMethods) {
		List<Document> out = new ArrayList<Document>();

		Type listType = new TypeToken<List<JsonObject>>() {}.getType();
		for (JsonObject testMethod : testMethods) {
			List<JsonObject> regions = new Gson().fromJson(testMethod.get("testCodeRegion"), listType);
			String name = testMethod.get("unitTestMethod").getAsString();
			String annotation = testMethod.get("annotation").getAsString();
			String feature = "";
			if (annotation.contains("(")) {
				feature = annotation.substring(annotation.indexOf("(") + 1, annotation.indexOf(")"));
			}

			for (JsonObject region : regions) {
				out.add(new Document(name, feature + " " + region.get("slice").getAsString(), region.get("testCodeLocation").getAsJsonObject()));
			}
		}

		return out;
	}
}
