package org.DoTeLink.util;

import org.DoTeLink.output.Document;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.StringTokenizer;

public class TFIDF {
	static double tf(String term, Document doc) {
		double freq = 0;
		List<String> toks = doc.getToken();
		for (String word : toks) {
			if (word.equals(term))
				freq++;
		}
		return Math.log10(freq + 1);
	}

	static double idf(String term, List<Document> docs) {
		double DF = 0;
		for (Document doc : docs) {
			List<String> toks = doc.getToken();
			for (String word : toks) {
				if (word.equals(term)) {
					DF++;
					break;
				}
			}
		}

		return (docs.size() / DF);
	}

	public static List<Document> vectorize(List<Document> docs, Set<String> vocab) {
		List<Document> out = new ArrayList<Document>();
		for (Document doc : docs) {
			double norm = 0.0;
			for (String term : vocab) {
				if (doc.getToken().contains(term)) {
					double w = tf(term, doc) * idf(term, docs);
					norm += w;
				}
			} 

			for (String term : vocab) {
				if (doc.getToken().contains(term)) {
					double w = tf(term, doc) * idf(term, docs);
					doc.setWeight(term, w / norm);
				}
				else
					doc.setWeight(term, 0.0);
			}
			out.add(doc);
		}
		return out;
	}

	public static Set<String> getVocabulary(List<Document> sentences, List<Document> testCodeSnippets) {
		Set<String> vocab = new HashSet<String>();

		for (Document d : sentences) {
			vocab.addAll(d.getToken());
		}

		for (Document d : testCodeSnippets) {
			vocab.addAll(d.getToken());
		}

		return vocab;
	}

    /**
	 * 1. Remove special symbols such as HTML tags
	 * 2. Tokenize with a whitespace
	 * 3. Split tokens based on camel case while keeping the original tokens.
	 * 4. Remove English stop words and 
	 * 5. Convert all capital letters to lowercase
	 */
	public static List<Document> preProcessing(List<Document> docs, List<String> removeWords) {
		List<Document> out = new ArrayList<Document>();
		for (Document d : docs) {
			String text = RemoveSpecialSymbols(d.getText());
			List<String> rawTokens = Tokenize(text);
			List<String> tokens = SplitCamelCase(rawTokens);
	
			for (String token : tokens) {
				if (!removeWords.contains(token.toLowerCase()))
					d.addToken(token.toLowerCase());
			}
			out.add(d);
		}
		return out;
	}

	static String RemoveSpecialSymbols(String rawText) {
		String regex = "-?\\d+(\\.\\d+)*|\\w+";
		String txt = rawText.replaceAll("(</?p>)|(</?div>)|(</?code>)|(</?u?l?i?>)|(</?P>)|(\\{@link)|(\\{@code)|(</?b>)|(_)", " ");
		String out = "";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(txt);
		while (matcher.find()) {
			for (int i = 0; i < matcher.groupCount(); i++) {
				out += matcher.group(i) + " ";
			}
		}
	
		return out;
	}

	/**
	 * E.g.,) CamelCaseABCEx = [Camel, Case, ABC, Ex]
	 */
	static List<String> SplitCamelCase(List<String> cleanTokens) {
		List<String> out = new ArrayList<String>();
		for (String token : cleanTokens) {
			String[] t = token.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");

			// keep the original token
			if (t.length > 1)
				out.add(token); 

			for (String _t : t)
				out.add(_t);
		}
		return out;
	}

	static List<String> Tokenize(String text) {
		List<String> words = new ArrayList<String>();

		StringTokenizer strTok = new StringTokenizer(text);
		while (strTok.hasMoreTokens()) {
			String word = strTok.nextToken();
			words.add(word);			
		}
		return words;
	}
}
