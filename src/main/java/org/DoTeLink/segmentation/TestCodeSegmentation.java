package org.DoTeLink.segmentation;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.*;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.Node.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.*;

import java.util.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

public class TestCodeSegmentation {
	public static JsonArray segregate(CompilationUnit cu) {
		JsonArray jClazz = new JsonArray();
		List<MethodDeclaration> testMethods = GetTestMethods(cu);
		for (MethodDeclaration testMethod : testMethods) {
			String methodName = testMethod.resolve().getQualifiedSignature();

			JsonObject jMethod = new JsonObject();
			jMethod.addProperty("unitTestMethod", methodName);
			jMethod.addProperty("annotation", GetAnnotation(testMethod));
			jMethod.addProperty("code", GetStatementAsString(testMethod.getBody().get().getStatements()));
			jMethod.add("offset", GetMethodInvocations(testMethod));

			JsonArray jSnippets = new JsonArray();
			HashMap<Integer, List<Statement>> sections = SeparateWithAssertions(testMethod);
			Map<Integer, Integer> icd = GetAllImmediateControlDependency(testMethod);
			Map<Integer, List<Integer>> idd = GetAllImmediateDataDependency(testMethod);

			for (Integer firstLineNum : sections.keySet()) {
				List<Statement> snippet = new ArrayList<Statement>();
				List<Statement> section = sections.get(firstLineNum);
				snippet.addAll(GetTransitiveClosure(testMethod, section, idd, icd));

				JsonObject jLocation = new JsonObject();
				jLocation.addProperty("line", firstLineNum);
				jLocation.addProperty("column", section.get(0).getBegin().get().column);

				JsonObject jSnippet = new JsonObject();
				jSnippet.add("testCodeLocation", jLocation);
				jSnippet.addProperty("assert", GetStatementAsString(section));
				jSnippet.addProperty("slice", GetStatementAsString(snippet));

				jSnippets.add(jSnippet);
			}
			jMethod.add("testCodeRegion", jSnippets);
			jClazz.add(jMethod);
		}
		return jClazz;
	}

	private static List<Statement> GetTransitiveClosure(MethodDeclaration testMethod, List<Statement> section, Map<Integer, List<Integer>> idd, Map<Integer, Integer> icd) {
		Set<Integer> dd = new HashSet<Integer>();
		for (Statement stmt : section) {
			List<ExpressionStmt> exprStmts = stmt.findAll(ExpressionStmt.class);
			for (ExpressionStmt expr : exprStmts) {
				int from = expr.getRange().get().begin.line;
				dd.addAll(GetTransitiveDataDependency(idd, from));
			}
		}

		Set<Integer> closureLines = new HashSet<Integer>(dd);
		for (Integer from : dd) {
			if (icd.containsKey(from) && !closureLines.contains(icd.get(from))) {
				// closureLine.remove(from);
				closureLines.add(icd.get(from));
				closureLines.addAll(GetTransitiveDataDependency(idd, icd.get(from)));
			}
		}

		List<Statement> snippet = new ArrayList<Statement>();
		List<Statement> orgStmts = testMethod.getBody().get().getStatements();
		for (Statement stmt : orgStmts) {
			int lineNum = stmt.getRange().get().begin.line;
			if (closureLines.contains(lineNum)) {
				Statement sec = GetStatement(section, lineNum);
				if (sec != null) {
					snippet.add(sec);
				}
				else {
					snippet.add(stmt);
				}
			}
		}
		return snippet;
	}

	private static Statement GetStatement(List<Statement> section, int lineNum) {
		for (Statement stmt : section) {
			if (stmt.getRange().get().begin.line == lineNum)
				return stmt;
		}
		return null;
	}

	private static Map<Integer, Integer> GetAllImmediateControlDependency(MethodDeclaration testMethod) {
		Map<Integer, Integer> icd = new HashMap<Integer, Integer>();

		List<IfStmt> ifStmts = testMethod.findAll(IfStmt.class);
		List<ForStmt> forStmts = testMethod.findAll(ForStmt.class);
		List<ForEachStmt> forEachStmts = testMethod.findAll(ForEachStmt.class);
		List<TryStmt> tryStmts = testMethod.findAll(TryStmt.class);
		List<SwitchStmt> switchStmts = testMethod.findAll(SwitchStmt.class);

		if (ifStmts.size() + forStmts.size() + forEachStmts.size() + tryStmts.size() + switchStmts.size() < 1) {
			return icd;
		}

		if (ifStmts.size() > 0) 
			icd.putAll(GetRelation(ifStmts));
		if (forStmts.size() > 0) 
			icd.putAll(GetRelation(forStmts));
		if (forEachStmts.size() > 0) 
			icd.putAll(GetRelation(forEachStmts));
		if (tryStmts.size() > 0) 
			icd.putAll(GetRelation(tryStmts));
		if (switchStmts.size() > 0) 
			icd.putAll(GetRelation(switchStmts));
		
		return icd;
	}

	private static HashMap<Integer, Integer> GetRelation(List<? extends Statement> controlStmts) {
		HashMap<Integer, Integer> icd = new HashMap<Integer, Integer>();

		for (Statement controlStmt : controlStmts) {
			int to = controlStmt.getRange().get().begin.line;
			List<ExpressionStmt> exprStmts = controlStmt.findAll(ExpressionStmt.class);
			for (ExpressionStmt exprStmt : exprStmts) {
				int from = exprStmt.getRange().get().begin.line;
				if (icd.containsKey(from)) { // nested control dependency 
					if (to < icd.get(from))
						icd.put(from, to);
				}
				else
					icd.put(from, to);
			}
		}
		return icd;
	}

	private static Map<Integer, List<Integer>> GetAllImmediateDataDependency(MethodDeclaration testMethod) {
		List<Statement> orgStmts = testMethod.getBody().get().getStatements();
		Map<Integer, List<Integer>> idd = new HashMap<Integer, List<Integer>>();
		for (Statement orgStmt1 : orgStmts) {
			int from = orgStmt1.getRange().get().begin.line;
			Set<String> useVars = Use(orgStmt1);
			for (Statement orgStmt2 : orgStmts) {
				int to = orgStmt2.getRange().get().begin.line;
				if (HasAssertion(orgStmt2))
					continue;
				if (to >= from)
					break;

				Set<String> writeVars = Write(orgStmt2);
				for (String var : writeVars) {
					if (useVars.contains(var)) {
						if (!idd.containsKey(from))
							idd.put(from, new ArrayList<Integer>());
						idd.get(from).add(to);
					}
				}
			}
		}

		return idd;
	}

	// resolve dd in try-catch block
	private static Map<Integer, List<Integer>> GetAllImmediateDataDependency_(MethodDeclaration testMethod) {
		List<Statement> orgStmts = testMethod.getBody().get().getStatements();
		Map<Integer, List<Integer>> idd = new HashMap<Integer, List<Integer>>();
		for (Statement orgStmt1 : orgStmts) {
			List<ExpressionStmt> exprStmts = orgStmt1.findAll(ExpressionStmt.class);

			for (ExpressionStmt expr : exprStmts) {
				int from = expr.getRange().get().begin.line;
				Set<String> useVars = Use(expr);
				for (Statement orgStmt2 : orgStmts) {
					int to = orgStmt2.getRange().get().begin.line;
					if (HasAssertion(orgStmt2))
						continue;
					if (to >= from)
						break;

					Set<String> writeVars = Write(orgStmt2);
					for (String var : writeVars) {
						if (useVars.contains(var)) {
							if (!idd.containsKey(from))
								idd.put(from, new ArrayList<Integer>());
							idd.get(from).add(to);
						}
					}
				}
			}

		}

		return idd;
	}

	private static Map<Integer, List<Integer>> GetAllImmediateDataDependency2(MethodDeclaration testMethod) {
		List<ExpressionStmt> orgExprStmts = testMethod.getBody().get().findAll(ExpressionStmt.class);
		Map<Integer, List<Integer>> idd = new HashMap<Integer, List<Integer>>();
		for (ExpressionStmt orgStmt1 : orgExprStmts) {
			int from = orgStmt1.getRange().get().begin.line;
			Set<String> useVars = Use(orgStmt1);

			for (ExpressionStmt orgStmt2 : orgExprStmts) {
				int to = orgStmt2.getRange().get().begin.line;
				if (HasAssertion(orgStmt2))
					continue;
				if (to >= from)
					break;

				Set<String> writeVars = Write(orgStmt2);
				for (String var : writeVars) {
					if (useVars.contains(var)) {
						if (!idd.containsKey(from))
							idd.put(from, new ArrayList<Integer>());
						idd.get(from).add(to);
					}
				}
			}
		}

		List<Statement> orgStmts = testMethod.getBody().get().getStatements();
		for (Statement s : orgStmts) {
			int from = s.getRange().get().begin.line;
			boolean hasBlock = false;
			Set<String> all = Use(s);
			Set<String> exclude = new HashSet<String>();
			List<BlockStmt> blocks = s.findAll(BlockStmt.class);
			for (BlockStmt block : blocks) {
				exclude.addAll(Use(block));
				hasBlock = true;
			}
			all.removeAll(exclude);
			if (hasBlock && all.size() > 0) {
				for (ExpressionStmt orgStmt2 : orgExprStmts) {
					int to = orgStmt2.getRange().get().begin.line;
					if (HasAssertion(orgStmt2))
						continue;
					if (to >= from)
						break;

					Set<String> writeVars = Write(orgStmt2);
					for (String var : writeVars) {
						if (all.contains(var)) {
							if (!idd.containsKey(from))
								idd.put(from, new ArrayList<Integer>());
							idd.get(from).add(to);
						}
					}
				}
			}
		}
		return idd;
	}

	private static List<Integer> GetTransitiveDataDependency(Map<Integer, List<Integer>> idd, Integer from) {
		SortedSet<Integer> out = new TreeSet<Integer>();
		if (idd.containsKey(from)) {
			for (Integer to : idd.get(from)) {
				out.addAll(GetTransitiveDataDependency(idd, to));
			}
			out.add(from);
		}
		else
			out.add(from);
		return new ArrayList<Integer>(out);
	}

	private static List<ExpressionStmt> GetExprStmtWise(List<Statement> section) {
		List<ExpressionStmt> exprStmts = new ArrayList<ExpressionStmt>();
		for (Statement s : section) {
			exprStmts.addAll(s.findAll(ExpressionStmt.class));
		}
		return exprStmts;
	}

	private static List<ExpressionStmt> GetExprStmtWise(MethodDeclaration testMethod) {
		List<ExpressionStmt> stmts = testMethod.getBody().get().findAll(ExpressionStmt.class);

		List<ExpressionStmt> exprStmts = new ArrayList<ExpressionStmt>();
		for (Statement s : stmts) {
			exprStmts.addAll(s.findAll(ExpressionStmt.class));
		}
		return exprStmts;
	}

	private static HashMap<Integer, List<Statement>> SeparateWithAssertions(MethodDeclaration testMethod) {
		HashMap<Integer, List<Statement>> sections = new HashMap<>();

		int firstLineNum = -1;
		List<Statement> section = new ArrayList<Statement>();
		List<Statement> stmts = testMethod.clone().getBody().get().getStatements();
		for (Statement stmt : stmts) {
			section.add(stmt);

			if (firstLineNum == -1)
				firstLineNum = stmt.getRange().get().begin.line;

			if (HasAssertion(stmt)) {
				sections.put(firstLineNum, section);
				section = new ArrayList<Statement>();
				firstLineNum = -1;
			}
		}

		if (section.size() > 0)
			sections.put(firstLineNum, section);
		return sections;
	}

	private static boolean HasAssertion(Statement stmt) {
		if (stmt.toString().contains("assert") || stmt.toString().contains("fail"))
			return true;
		return false;
	}

	private static JsonArray GetMethodInvocations(MethodDeclaration testMethod) {
		JsonArray jOffsets = new JsonArray();
		testMethod.findAll(Expression.class).forEach(s -> {
			if (s instanceof MethodCallExpr) {
				JsonObject jObj = new JsonObject();
				jObj.addProperty("line", s.getRange().get().begin.line);
				jObj.addProperty("column", s.getRange().get().begin.column);
				jOffsets.add(jObj); 
			}
			else if (s instanceof ObjectCreationExpr) {
				JsonObject jObj = new JsonObject();
				jObj.addProperty("line", s.getRange().get().begin.line);
				jObj.addProperty("column", s.getRange().get().begin.column);
				jOffsets.add(jObj); 
			}
		});

		return jOffsets;
	}

	private static List<MethodDeclaration> GetTestMethods(CompilationUnit cu) {
		List<MethodDeclaration> testMethods = new ArrayList<MethodDeclaration>();
		cu.findAll(MethodDeclaration.class).forEach(md -> {
			if (!isDeprecated(md)) {
				if (isTestMethod(md)) {
					testMethods.add(md);		
				}
			}
		});

		return testMethods;
	}

	private static boolean isDeprecated(MethodDeclaration m) {
		List<Node> nodes = m.getChildNodes();
		for (Node node : nodes) {
			if (node instanceof MarkerAnnotationExpr || node instanceof NormalAnnotationExpr) {
				if (node.toString().contains("@Deprecated") || node.toString().contains("@deprecated"))
					return true;
			}
		}
		return false;
	}

	private static boolean isTestMethod(MethodDeclaration m) {
		// JUnit 3
		if (m.getName().toString().startsWith("test"))
			return true;

		// JUnit 4, 5
		List<Node> nodes = m.getChildNodes();
		for (Node node : nodes) {
			if (node instanceof MarkerAnnotationExpr || node instanceof NormalAnnotationExpr) {
				if (node.toString().contains("@Test")) {
					return true;
				}
			}
		}
		return false;

	}

	private static Set<String> Use(ExpressionStmt stmt) {
		Set<String> use = new HashSet<String>(Read(stmt));
		use.addAll(Write(stmt));
		return use;
	}

	private static Set<String> Use(Statement stmt) {
		Set<String> use = new HashSet<String>(Read(stmt));
		use.addAll(Write(stmt));
		return use;
	}

	private static Set<String> Read(Statement stmt) {
		Set<String> readVars = new HashSet<String>();

		Set<String> vars = new HashSet<String>();
		Set<String> exclude = new HashSet<String>();
		if (stmt.toString().contains("assert")) {
			stmt.walk(node -> {
				if (node instanceof NameExpr)
					vars.add(node.toString());
			});
		}
		else {
			stmt.walk(node -> {
				if (node instanceof NameExpr) {
					vars.add(node.toString());
				}
				if (node instanceof AssignExpr || node instanceof VariableDeclarator) {
					exclude.add(node.toString().split("=")[0].trim());
				}
				if (node instanceof MethodCallExpr) {
					if (!node.findFirst(NameExpr.class).isEmpty())
						exclude.add(node.findFirst(NameExpr.class).get().toString()); // first NameExpr is an object name
				}
			});
		}
		vars.removeAll(exclude);
		readVars.addAll(vars);
		return readVars;
	}

	private static Set<String> Read(ExpressionStmt stmt) {
		Set<String> readVars = new HashSet<String>();

		Set<String> vars = new HashSet<String>();
		Set<String> exclude = new HashSet<String>();
		if (stmt.toString().contains("assert")) {
			stmt.walk(node -> {
				if (node instanceof NameExpr)
					vars.add(node.toString());
			});
		}
		else {
			stmt.walk(node -> {
				if (node instanceof NameExpr) {
					vars.add(node.toString());
				}
				if (node instanceof AssignExpr || node instanceof VariableDeclarator) {
					exclude.add(node.toString().split("=")[0].trim());
				}
				if (node instanceof MethodCallExpr) {
					if (!node.findFirst(NameExpr.class).isEmpty())
						exclude.add(node.findFirst(NameExpr.class).get().toString()); // first NameExpr is an object name
				}
			});
		}
		vars.removeAll(exclude);
		readVars.addAll(vars);
		return readVars;
	}

	private static Set<String> Write(Statement stmt) {
		Set<String> writeVars = new HashSet<String>();

		if (stmt.toString().contains("assert")) // Consider an assertion stmt as Read
			return writeVars;

		Set<String> vars = new HashSet<String>();
		stmt.walk(node -> {
			if (node instanceof AssignExpr || node instanceof VariableDeclarator) {
				vars.add(node.toString().split("=")[0].trim());
			}
			if (node instanceof MethodCallExpr) {
				if (!node.findFirst(NameExpr.class).isEmpty())
					vars.add(node.findFirst(NameExpr.class).get().toString());
			}
			if (node instanceof UnaryExpr) {
				if (node.toString().contains("++") || node.toString().contains("--"))
					vars.add(node.findFirst(NameExpr.class).get().toString());
			}
		});
		writeVars.addAll(vars);
		return writeVars;
	}

	private static Set<String> Write(ExpressionStmt stmt) {
		Set<String> writeVars = new HashSet<String>();

		if (stmt.toString().contains("assert")) // Consider an assertion stmt as Read
			return writeVars;

		Set<String> vars = new HashSet<String>();
		stmt.walk(node -> {
			if (node instanceof AssignExpr || node instanceof VariableDeclarator) {
				vars.add(node.toString().split("=")[0].trim());
			}
			if (node instanceof MethodCallExpr) {
				if (!node.findFirst(NameExpr.class).isEmpty())
					vars.add(node.findFirst(NameExpr.class).get().toString());
			}
			if (node instanceof UnaryExpr) {
				if (node.toString().contains("++") || node.toString().contains("--"))
					vars.add(node.findFirst(NameExpr.class).get().toString());
			}
		});
		writeVars.addAll(vars);
		return writeVars;
	}

	private static String GetStatementAsString(List<Statement> stmts) {
		String ret = "";
		for (Statement s : stmts)
			ret += s.toString();
		return ret;
	}

	private static String GetAnnotation(MethodDeclaration testMethod) {
		if (testMethod.findFirst(MarkerAnnotationExpr.class).isPresent())
			return testMethod.findFirst(MarkerAnnotationExpr.class).get().toString();
		else if (testMethod.findFirst(NormalAnnotationExpr.class).isPresent())
			return testMethod.findFirst(NormalAnnotationExpr.class).get().toString();
		return "";
	}
}
