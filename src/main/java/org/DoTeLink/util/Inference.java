package org.DoTeLink.util;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Collections;

import org.DoTeLink.output.Link;
import org.DoTeLink.output.Matrix;
import org.DoTeLink.output.Document;
import org.DoTeLink.output.Similarity;
import org.DoTeLink.output.MethodAssociationOutput;

import com.google.gson.JsonObject;

public class Inference {
	private static final double tau = 0.00001;

	public static List<Link> inferSentenceWise(Map<MethodAssociationOutput, List<Similarity>> productionMatrix) {
		List<Link> out = new ArrayList<Link>();
		for (MethodAssociationOutput relation : productionMatrix.keySet()) {
			List<Similarity> matrix = productionMatrix.get(relation);
			List<Similarity> sentenceWise = GetSentenceWiseSelection(matrix);

			List<Link> links = GetLinkPerMethodPair(sentenceWise);
			out.addAll(links);
		}

		return out;
	}

	public static List<Link> inferTestCodeSnippetWise(Map<MethodAssociationOutput, List<Similarity>> productionMatrix) {
		Map<MethodAssociationOutput, List<Similarity>> testMatrix = ChangeKeyAsTestMethod(productionMatrix);
		List<Link> out = new ArrayList<Link>();
		for (MethodAssociationOutput relation : testMatrix.keySet()) {
			List<Similarity> matrix = testMatrix.get(relation);
			List<Similarity> testCodeSnippetWise = GetTestCodeSnippetWiseSelection(matrix);

			List<Link> links = GetLinkPerMethodPair(testCodeSnippetWise);
			out.addAll(links);
		}

		return out;
	}

	public static List<Link> inferSentenceWiseBest(Map<MethodAssociationOutput, List<Similarity>> productionMatrix) {
		List<Link> out = new ArrayList<Link>();
		for (MethodAssociationOutput relation : productionMatrix.keySet()) {
			List<Similarity> matrix = productionMatrix.get(relation);
			List<Similarity> sentenceWise = GetSentenceWiseBestSelection(matrix);

			List<Link> links = GetLinkPerMethodPair(sentenceWise);
			out.addAll(links);
		}
		return out;
	}

	public static List<Link> inferTestCodeSnippetWiseBest(Map<MethodAssociationOutput, List<Similarity>> productionMatrix) {
		Map<MethodAssociationOutput, List<Similarity>> testMatrix = ChangeKeyAsTestMethod(productionMatrix);
		List<Link> out = new ArrayList<Link>();
		for (MethodAssociationOutput relation : testMatrix.keySet()) {
			List<Similarity> matrix = testMatrix.get(relation);
			List<Similarity> testCodeSnippetWise = GetTestCodeSnippetWiseBestSelection(matrix);

			List<Link> links = GetLinkPerMethodPair(testCodeSnippetWise);
			out.addAll(links);
		}

		return out;
	}

	static Map<MethodAssociationOutput, List<Similarity>> ChangeKeyAsTestMethod(Map<MethodAssociationOutput, List<Similarity>> productionMatrix) {
		// convert pm-tm matrix to tm-pm matrix
		Map<MethodAssociationOutput, List<Similarity>> out = new HashMap<>();
		Map<JsonObject, Set<JsonObject>> tmp = new HashMap<>();
		for (MethodAssociationOutput relation : productionMatrix.keySet()) {
			Set<JsonObject> pm = relation.getProductionMethods();
			Set<JsonObject> tm = relation.getTestMethods();
			for (JsonObject t : tm) {
				if (!tmp.containsKey(t))
					tmp.put(t, new HashSet<JsonObject>());
				for (JsonObject p : pm)
					tmp.get(t).add(p);
			}
		}

		Map<MethodAssociationOutput, List<Similarity>> newOut = new HashMap<>();
		for (MethodAssociationOutput relation : productionMatrix.keySet()) {
			for (JsonObject t : tmp.keySet()) {
				List<Similarity> matrix = new ArrayList<Similarity>();
				for (Similarity sim : productionMatrix.get(relation)) {
					String productionMethodName = sim.getProductionMethodName();
					String testMethodName = sim.getTestMethodName();
					if (t.get("unitTestMethod").getAsString().equals(testMethodName)) {
						for (JsonObject p : tmp.get(t)) {
							if (p.get("productionMethod").getAsString().equals(productionMethodName)) {
								matrix.add(new Similarity(productionMethodName, testMethodName, sim.getSentence(), sim.getTestCodeSnippet(), sim.getSimilarity()));
							}
						}
					}
				}
				MethodAssociationOutput ma = new MethodAssociationOutput();
				ma.addRelation(t, tmp.get(t));
				newOut.put(ma, matrix);
			}
		}

		return newOut;
	}

	static List<Link> GetLinkPerMethodPair(List<Similarity> selected) {
		Map<String, Map<String, List<Similarity>>> linkMap = new HashMap<>();
		for (Similarity sim : selected) {
			String productionMethodName = sim.getProductionMethodName();
			String testMethodName = sim.getTestMethodName();

			if (!linkMap.containsKey(productionMethodName)) {
				Map<String, List<Similarity>> tmp = new HashMap<>();
				List<Similarity> newList = new ArrayList<>();
				newList.add(sim);
				tmp.put(testMethodName, newList);
				linkMap.put(productionMethodName, tmp);
			}
			else {
				Map<String, List<Similarity>> t = linkMap.get(productionMethodName);
				if (!t.containsKey(testMethodName)) {
					List<Similarity> newList = new ArrayList<>();
					newList.add(sim);
					linkMap.get(productionMethodName).put(testMethodName, newList);
				}
				else {
					linkMap.get(productionMethodName).get(testMethodName).add(sim);
				}
			}
		}

		List<Link> out = new ArrayList<Link>();
		for (String productionMethodName : linkMap.keySet()) {
			for (String testMethodName : linkMap.get(productionMethodName).keySet()) {
				List<Similarity> similarities = linkMap.get(productionMethodName).get(testMethodName);
				Link link = new Link(productionMethodName, testMethodName);
				for (Similarity sim : similarities) {
					link.addLink(sim.getSentence(), sim.getTestCodeSnippet(), sim.getSimilarity());
				}
				out.add(link);
			}
		}

		return out;
	}

	static List<Link> GetLinkPerMethodPair(List<Similarity> sentenceWise, List<Similarity> testCodeSnippetWise) {
		Map<String, Map<String, List<Similarity>>> linkMap = new HashMap<>();
		Set<Similarity> selected = new HashSet<Similarity>(sentenceWise);
		selected.addAll(testCodeSnippetWise);

		for (Similarity sim : selected) {
			String productionMethodName = sim.getProductionMethodName();
			String testMethodName = sim.getTestMethodName();

			if (!linkMap.containsKey(productionMethodName)) {
				Map<String, List<Similarity>> tmp = new HashMap<>();
				List<Similarity> newList = new ArrayList<>();
				newList.add(sim);
				tmp.put(testMethodName, newList);
				linkMap.put(productionMethodName, tmp);
			}
			else {
				Map<String, List<Similarity>> t = linkMap.get(productionMethodName);
				if (!t.containsKey(testMethodName)) {
					List<Similarity> newList = new ArrayList<>();
					newList.add(sim);
					linkMap.get(productionMethodName).put(testMethodName, newList);
				}
				else {
					linkMap.get(productionMethodName).get(testMethodName).add(sim);
				}
			}
		}

		List<Link> out = new ArrayList<Link>();
		for (String productionMethodName : linkMap.keySet()) {
			for (String testMethodName : linkMap.get(productionMethodName).keySet()) {
				List<Similarity> similarities = linkMap.get(productionMethodName).get(testMethodName);
				Link link = new Link(productionMethodName, testMethodName);
				for (Similarity sim : similarities) {
					link.addLink(sim.getSentence(), sim.getTestCodeSnippet(), sim.getSimilarity());
				}

				out.add(link);
			}
		}

		return out;
	}

	static List<Similarity> GetTestCodeSnippetWiseSelection(List<Similarity> matrix) {
		double popMean = Mean(matrix);
		Map<Document, List<Similarity>> max = new HashMap<>();
		for (Similarity pair : matrix) {
			Document testCodeSnippet = pair.getTestCodeSnippet();
			Document sentence = pair.getSentence();
			if (max.containsKey(testCodeSnippet))
				continue;

			List<Similarity> testCodeSnippetMatrix = GetMatrixByTestCodeSnippet(matrix, testCodeSnippet);
			double testCodeSnippetVar = Variance(testCodeSnippetMatrix);
			if (testCodeSnippetVar < tau)
				continue;

			double maxSim = 0.0;
			for (Similarity sim : testCodeSnippetMatrix) {
				double similarity = sim.getSimilarity();
				if (similarity == 0.0)
					continue;

				if (similarity <= popMean)
					continue;

				if (maxSim < similarity) {
					maxSim = similarity;
					List<Similarity> tmp = new ArrayList<Similarity>();
					tmp.add(sim);
					max.put(testCodeSnippet, tmp);
				}
				else if (maxSim == similarity) { 
					if (!max.containsKey(testCodeSnippet))
						max.put(testCodeSnippet, new ArrayList<Similarity>());
					max.get(testCodeSnippet).add(sim);
				}
			}
		}

		List<Similarity> out = new ArrayList<Similarity>();
		for (Document testCodeSnippet : max.keySet()) {
			for (Similarity sim : max.get(testCodeSnippet)) {
				out.add(sim);
			}
		}

		return out;
	}

	static List<Similarity> GetTestCodeSnippetWiseBestSelection(List<Similarity> matrix) {
		Map<Document, Map<Double, List<Similarity>>> sortedMap = new HashMap<Document, Map<Double, List<Similarity>>>();

		for (Similarity pair : matrix) {
			Document testCodeSnippet = pair.getTestCodeSnippet();
			if (sortedMap.containsKey(testCodeSnippet))
				continue;

			List<Similarity> testCodeSnippetMatrix = GetMatrixByTestCodeSnippet(matrix, testCodeSnippet);
			double testCodeSnippetVar = Variance(testCodeSnippetMatrix);
			if (testCodeSnippetVar < tau)
				continue;

			for (Similarity sim : testCodeSnippetMatrix) {
				double similarity = sim.getSimilarity();
				if (similarity == 0.0)
					continue;

				if (!sortedMap.containsKey(testCodeSnippet))
					sortedMap.put(testCodeSnippet, new TreeMap<Double, List<Similarity>>(Collections.reverseOrder()));
				if (!sortedMap.get(testCodeSnippet).containsKey(similarity))
					sortedMap.get(testCodeSnippet).put(similarity, new ArrayList<Similarity>());
				sortedMap.get(testCodeSnippet).get(similarity).add(sim);
			}
		}
		
		double tmpSimilarity = 0.0;
		Set<Similarity> __best = new HashSet<Similarity>();
		for (Document testCodeSnippet : sortedMap.keySet()) {
			for (Double similarity : sortedMap.get(testCodeSnippet).keySet()) {
				if (tmpSimilarity < similarity) {
					__best.clear();
					for (Similarity sim : sortedMap.get(testCodeSnippet).get(similarity)) {
						__best.add(sim);
					}
				}
				else if (tmpSimilarity == similarity) {
					for (Similarity sim : sortedMap.get(testCodeSnippet).get(similarity)) {
						__best.add(sim);
					}
				}
			}
		}

		List<Similarity> out = new ArrayList<Similarity>();
		for (Similarity __b : __best)
			out.add(__b);

		return out;
	}

	static List<Similarity> GetSentenceWiseSelection(List<Similarity> matrix) {
		double popMean = Mean(matrix);
		Map<Document, List<Similarity>> max = new HashMap<>();
		for (Similarity pair : matrix) {
			Document sentence = pair.getSentence();
			if (max.containsKey(sentence))
				continue;

			List<Similarity> sentenceMatrix = GetMatrixBySentence(matrix, sentence);
			double sentenceVar = Variance(sentenceMatrix);
			if (sentenceVar < tau)
				continue;

			double maxSim = 0.0;
			for (Similarity sim : sentenceMatrix) {
				double similarity = sim.getSimilarity();

				if (similarity == 0.0)
					continue;

				if (similarity <= popMean)
					continue;

				if (maxSim < similarity) {
					maxSim = similarity;
					List<Similarity> tmp = new ArrayList<Similarity>();
					tmp.add(sim);
					max.put(sentence, tmp);
				}
				else if (maxSim == similarity) { 
					if (!max.containsKey(sentence))
						max.put(sentence, new ArrayList<Similarity>());
					max.get(sentence).add(sim);
				}
			}
		}

		List<Similarity> out = new ArrayList<Similarity>();
		for (Document sentence : max.keySet()) {
			for (Similarity sim : max.get(sentence)) {
				out.add(sim);
			}
		}

		return out;
	}

	static List<Similarity> GetSentenceWiseBestSelection(List<Similarity> matrix) {
		Map<Document, Map<Double, List<Similarity>>> sortedMap = new HashMap<Document, Map<Double, List<Similarity>>>();

		for (Similarity pair : matrix) {
			Document sentence = pair.getSentence();
			List<Similarity> sentenceMatrix = GetMatrixBySentence(matrix, sentence);
			double sentenceVar = Variance(sentenceMatrix);
			if (sentenceVar < tau)
				continue;

			for (Similarity sim : sentenceMatrix) {
				double similarity = sim.getSimilarity();
				if (similarity == 0.0)
					continue;

				if (!sortedMap.containsKey(sentence))
					sortedMap.put(sentence, new TreeMap<Double, List<Similarity>>(Collections.reverseOrder()));
				if (!sortedMap.get(sentence).containsKey(similarity))
					sortedMap.get(sentence).put(similarity, new ArrayList<Similarity>());
				sortedMap.get(sentence).get(similarity).add(sim);
			}
		}

		double tmpSimilarity = 0.0;
		Set<Similarity> __best = new HashSet<Similarity>();
		for (Document sentence : sortedMap.keySet()) {
			for (Double similarity : sortedMap.get(sentence).keySet()) {
				if (tmpSimilarity < similarity) {
					__best.clear();
					tmpSimilarity = similarity; 
					for (Similarity sim : sortedMap.get(sentence).get(similarity)) {
						__best.add(sim);
					}
				}
				else if (tmpSimilarity == similarity) {
					for (Similarity sim : sortedMap.get(sentence).get(similarity)) {
						__best.add(sim);
					}
				}
			}
		}

		List<Similarity> out = new ArrayList<Similarity>();
		for (Similarity __b : __best)
			out.add(__b);

		return out;
	}

	static List<Similarity> GetMatrixBySentence(List<Similarity> matrix, Document sentence) {
		List<Similarity> out = new ArrayList<Similarity>();
		for (Similarity sim : matrix) {
			if (sim.getSentence().equals(sentence)) {
				out.add(sim);
			}
		}
		return out;
	}

	static List<Similarity> GetMatrixByTestCodeSnippet(List<Similarity> matrix, Document testCodeSnippet) {
		List<Similarity> out = new ArrayList<Similarity>();
		for (Similarity sim : matrix) {
			if (sim.getTestCodeSnippet().equals(testCodeSnippet)) {
				out.add(sim);
			}
		}
		return out;
	}

	static double Mean(List<Similarity> matrix) {
		double sum = 0.0;
		for (Similarity mat : matrix) {
			sum += mat.getSimilarity();
		}
		return sum / matrix.size();
	}

	static double Variance(List<Similarity> matrix) {
		double mu = Mean(matrix);
		double sigma2 = 0.0;

		for (Similarity mat : matrix) {
			double s = mat.getSimilarity();
			double deviation = s - mu;

			sigma2 += Math.pow(deviation, 2);
		}
		return sigma2 / matrix.size();
	}
}
