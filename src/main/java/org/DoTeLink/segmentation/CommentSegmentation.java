package org.DoTeLink.segmentation;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.Node.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

/**
 * Some code that uses JavaParser.
 */
public class CommentSegmentation {
	private static Pattern DecimalPattern = Pattern.compile("-?\\d+(\\.\\d+)");
	private static Pattern AlphaNumeric = Pattern.compile("[A-Za-z0-9]");
	private static CompilationUnit cu;

	public static JsonArray segregate(CompilationUnit cu) {
		JsonArray jClazz = new JsonArray();
		HashMap<String, Comment> commentPerMethod = GetCommentPerMethod(cu);
		for (String methodName : commentPerMethod.keySet()) {
			Comment comment = commentPerMethod.get(methodName);

			JsonArray jSentences = Segregate(comment);
			JsonObject jMethod = new JsonObject();
			jMethod.addProperty("productionMethod", methodName);
			if (comment == null)
				jMethod.addProperty("comment", "");
			else
				jMethod.addProperty("comment", comment.getContent());
			jMethod.add("sentences", jSentences);

			jClazz.add(jMethod);
		}
		return jClazz;
	}

	private static HashMap<String, Comment> GetCommentPerMethod(CompilationUnit cu) {
		// `targetMethods`: (method, comments) 
		HashMap<String, Comment> targetMethods = new HashMap<String, Comment>();

		// Get a text from comment in each product method 
		cu.findAll(MethodDeclaration.class).forEach(md -> {
			if (!isDeprecated(md)) {
				if (md.getComment().isPresent()) {
					targetMethods.put(md.resolve().getQualifiedSignature(), md.getComment().get());
				}
				else {
					targetMethods.put(md.resolve().getQualifiedSignature(), null);
				}
			}
		});
		cu.findAll(ConstructorDeclaration.class).forEach(md -> {
			if (!isDeprecated(md)) {
				if (md.getComment().isPresent()) {
					targetMethods.put(md.resolve().getQualifiedSignature(), md.getComment().get());
				}
				else {
					targetMethods.put(md.resolve().getQualifiedSignature(), null);
				}
			}
		});
		return targetMethods;
	}

	private static JsonArray Segregate(Comment comment) {
		if (comment == null)
			return new JsonArray();

		JsonArray jSentences = new JsonArray();

		String commentString = comment.getContent();
		String text = ExtractCommentDescription(commentString);
		text += ExtractCommentOf(commentString, "@return");
		text += ExtractCommentOf(commentString, "@throws");
		text += ExtractCommentOf(commentString, "@exception");

		String sentence = "";
		String tagType = "";
		int delimiterPos = GetSentenceDelimiterPosition(text);
		HashMap<Integer, Integer> location = GetDescriptionLocation(comment);
		while (delimiterPos != -1) {
			sentence = text.substring(0, delimiterPos+1);
			text = text.substring(delimiterPos+1);
			int lineNum = (int)location.keySet().toArray()[0];
			int columnNum = (int)location.values().toArray()[0];

			if (sentence.startsWith("@")) {
//				sentence = sentence.replace("@" + tagType, "").trim();
			}

			JsonObject sentenceObj = new JsonObject();
			sentenceObj.addProperty("sentenceText", sentence.replaceAll("\n", " ").trim());
			sentenceObj.addProperty("tagType", tagType);
			
			JsonObject jLoc = new JsonObject();
			jLoc.addProperty("line", lineNum);
			jLoc.addProperty("column", columnNum);
			sentenceObj.add("sentenceLocation", jLoc);

			jSentences.add(sentenceObj);

			if (tagType == GetTagTypeWithPreviousTag(text, tagType)) {
				location = GetNextLocation(comment, sentence, location);
			}
			else {
				tagType = GetTagTypeWithPreviousTag(text, tagType);
				location = GetLocationOf(comment, tagType);
			}
			delimiterPos = GetSentenceDelimiterPosition(text);
		}

		return jSentences;
	}

	private static HashMap<Integer, Integer> GetNextLocation(Comment comment, String prevSentence, HashMap<Integer, Integer> location) {
		int cmtLineNum = comment.getBegin().get().line;
		int prevColumnNum = (int)location.values().toArray()[0];
		int prevLineNum = (int)location.keySet().toArray()[0];
		if (prevSentence.startsWith("\n"))
			prevSentence = prevSentence.substring(1);
		int nextLineNum = prevLineNum + prevSentence.split("\n").length-1;
		int nextColumnNum = prevColumnNum;

		String[] commentLines = comment.getContent().split("\n");
		for (String commentLine : commentLines) {
			if (cmtLineNum < nextLineNum || commentLine.trim().isEmpty()) {
				cmtLineNum += 1;
				continue;
			}

			if (prevSentence.contains("\n")) {
				int delPos = GetSentenceDelimiterPosition(commentLine) + 1;
				if (delPos == commentLine.length()) {
					nextLineNum += 1;
				}
				else if (commentLine.contains(".")) {
					while (commentLine.charAt(delPos) == ' ')
						delPos += 1;
					nextColumnNum = delPos + 1;
				}
			}
			else {
				String prevSen = prevSentence.trim();
				String cmtLine = commentLine.trim().substring(1).trim(); // rm beginning symbol (i.e., *)

				if (prevSen.equals(cmtLine)) {
					nextLineNum += 1;
				}
				else if (prevSen.length() < cmtLine.length()) {
					if (cmtLine.charAt(cmtLine.length()-1) == '.') {
						nextLineNum += 1;
						nextColumnNum = (int)GetDescriptionLocation(comment).values().toArray()[0];
						break;
					}

					int delPos = GetSentenceDelimiterPosition(commentLine) + 1;
					while (delPos < commentLine.length() && commentLine.charAt(delPos) == ' ')
						delPos += 1;
					nextColumnNum = delPos + 1;
				}
			}

			break;
		}

		HashMap<Integer, Integer> nextLoc = new HashMap<Integer, Integer>();
		nextLoc.put(nextLineNum, nextColumnNum);
		return nextLoc;
	}

	private static HashMap<Integer, Integer> GetLocationOf(Comment comment, String tagType) {
		int nextCol = comment.getBegin().get().column;
		int nextLineNum = comment.getBegin().get().line;
		String[] commentLines = comment.getContent().split("\n");
		for (String line : commentLines) {
			if (line.trim().startsWith("*")) {
				String tmp = line.replace("*", " ");
				if (tmp.trim().startsWith("@" + tagType)) {
					while (line.charAt(nextCol) == ' ' || line.charAt(nextCol) == '*') {
						nextCol += 1;
					}
					nextCol += 1;
					break;
				}
			}
			nextLineNum += 1;
		}
		HashMap<Integer, Integer> nextLoc = new HashMap<Integer, Integer>();
		nextLoc.put(nextLineNum, nextCol);
		return nextLoc;
	}

	private static HashMap<Integer, Integer> GetDescriptionLocation(Comment comment) {
		HashMap<Integer, Integer> location = new HashMap<Integer, Integer>();
		int lineNum = comment.getBegin().get().line;
		int colNum = comment.getBegin().get().column;
		String[] commentLines = comment.getContent().split("\n");
		for (String line : commentLines) {
			if (!AlphaNumeric.matcher(line).find()) {
				lineNum += 1;
				continue;
			}

			while (line.charAt(colNum) == ' ' || line.charAt(colNum) == '*' || line.charAt(colNum) == '/') {
				colNum += 1;
			}
			location.put(lineNum, colNum + 1);	
			break;
		}
		return location;
	}

	private static String GetTagTypeWithPreviousTag(String text, String prevTag) {
		if (text.trim().startsWith("@return")) {
			return "return";
		}
		else if (text.trim().startsWith("@throws")) {
			return "throws";
		}
		else if (text.trim().startsWith("@exception")) {
			return "exception";
		}
		else
			return prevTag;
	}

	/**
	 * Return a position that exists a delimiter otherwise, return the text length.
	 * Use the following elements as a sentence delimiter position:
	 *  [a period mark followed by a space or line break, an annotation header (e.g., @return)]
	 *
	 * @param text an extracted text from the comment
	 * @return a position that exists a delimiter, or length of the text
	 *			-1 if the text is empty.
	 */
	private static int GetSentenceDelimiterPosition(String text) {
		if (text.trim().isEmpty())
			return -1;

		String tmpText = text; 
		if (!text.trim().startsWith("@return") && !text.trim().startsWith("@throws") && !text.trim().startsWith("@exception")) {
			int firstTagPos = GetFirstTagPosition(text, "@return", "@throws", "@exception");
			if (firstTagPos != -1) {
				tmpText = text.substring(0, firstTagPos);
			}

			int periodPos = GetFirstPeriodMarkPosition(tmpText);
			return periodPos != -1 ? periodPos : tmpText.length()-1;
		}
		else {
			int nextTagPos = GetSecondTagPosition(text, "@return", "@throws", "@exception");
			if (nextTagPos != -1) {
				tmpText = text.substring(0, nextTagPos);
			}

			int periodPos = GetFirstPeriodMarkPosition(tmpText);
			return periodPos != -1 ? periodPos : tmpText.length()-1;
		}
	}

	private static int GetFirstPeriodMarkPosition(String text) {
		// find an initial period mark as a delimiter
		int preTextLength = 0;
		int periodMark = text.indexOf(".");
		while (periodMark > - 1) {
			// period mark followed by a space
			if (text.length() - 1 > periodMark && (text.charAt(periodMark + 1) == ' ' || text.charAt(periodMark + 1) == '\n')) {
				return periodMark + preTextLength;
			}

			// period mark followed by a new line (e.g., end of line)
			if (text.length() - 1 == periodMark) {
				return periodMark + preTextLength;
			}

			text = text.substring(periodMark + 1);
			preTextLength += periodMark + 1;
			periodMark = text.indexOf(".");
		}
		return -1;
	}

	private static int GetFirstTagPosition(String text, String rt, String thrws, String excpt) {
		int returnPos = text.indexOf(rt);
		if (returnPos != -1)
			return returnPos;
		else {
			int throwsPos = text.indexOf(thrws);
			if (throwsPos != -1)
				return throwsPos;
			else {
				int exceptionPos = text.indexOf(excpt);
				if (exceptionPos!= -1)
					return exceptionPos;
			}
		}
		return -1;
	}

	private static int GetSecondTagPosition(String text, String rt, String thrws, String excpt) {
		int firstTagPos = GetFirstTagPosition(text, rt, thrws, excpt);
		if (firstTagPos == -1)
			return -1;

		String tmp = text.substring(firstTagPos+1);
		int secondTagPos = GetFirstTagPosition(tmp, rt, thrws, excpt);
		
		if (secondTagPos != -1)
			return secondTagPos + firstTagPos;

		return -1;
	}

	private static String ExtractCommentDescription(String comment) {
		String ret = "";
		String[] lines = comment.split("\n");

		for (String line : lines) {
			line = line.trim();
			if (line.startsWith("*"))
				line = line.substring(1).trim();

			if (line.isEmpty())
				continue;

			if (line.startsWith("@"))
				break;

			ret += line + "\n";
		}
		return ret;
	}

	private static String ExtractCommentOf(String comment, String tag) {
		String ret = "";
		String[] lines = comment.split("\n");

		boolean hasTag = false;
		for (String line : lines) {
			line = line.trim();
			if (line.startsWith("*"))
				line = line.substring(1).trim();

			if (line.isEmpty())
				continue;

			if (line.startsWith(tag))
				hasTag = true;

			if (hasTag) {
				if (line.startsWith("@") && !line.startsWith(tag)) 
					break;
				ret += line + "\n";
			}
		}
		return ret;
	}

	private static boolean isDeprecated(ConstructorDeclaration m) {
	    List<Node> child = m.getChildNodes();
        for (Node ch : child) {
            if (ch instanceof MarkerAnnotationExpr || ch instanceof NormalAnnotationExpr) {
                if (ch.toString().contains("@Deprecated") || ch.toString().contains("@deprecated")) 
					return true;
			}
		}
		return false;
	}

	private static boolean isDeprecated(MethodDeclaration m) {
	    List<Node> child = m.getChildNodes();
        for (Node ch : child) {
            if (ch instanceof MarkerAnnotationExpr || ch instanceof NormalAnnotationExpr) {
                if (ch.toString().contains("@Deprecated") || ch.toString().contains("@deprecated")) 
					return true;
			}
		}
		return false;
	}
}
