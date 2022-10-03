package org.DoTeLink.util;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.lang.reflect.Type;
import org.DoTeLink.output.MethodAssociationOutput;

public class MethodAssociation {

	public static List<MethodAssociationOutput> relateProductionMethodWise(JsonArray jSegProductionMethod, JsonArray jSegTestMethod, CompilationUnit testCodeCu) {
		Type listType = new TypeToken<List<JsonObject>>() {}.getType();
		List<JsonObject> productionMethods = new Gson().fromJson(jSegProductionMethod, listType);
		List<JsonObject> testMethods = new Gson().fromJson(jSegTestMethod, listType);

		Map<JsonObject, Set<JsonObject>> mc = new HashMap<JsonObject, Set<JsonObject>>();
		for (JsonObject testMethod : testMethods) {
			mc.put(testMethod, GetProductionMethodsCalledBy(testMethod, productionMethods, testCodeCu));
		}

		Map<JsonObject, Set<JsonObject>> nc = GetNamingConvention(mc);
		Map<JsonObject, Set<JsonObject>> ncc = GetNamingConventionContains(mc);
		Map<JsonObject, Set<JsonObject>> methodAssociation = new HashMap<>();;

		// MC(t,m) and NC(t,m)
		for (JsonObject testMethod : nc.keySet()) {
			for (JsonObject productionMethod : nc.get(testMethod)) {
				if (!methodAssociation.containsKey(productionMethod))
					methodAssociation.put(productionMethod, new HashSet<JsonObject>());
				methodAssociation.get(productionMethod).add(testMethod);
			}
		}

		// \neg\exists m'(MC(t,m') and NC(t,m')) and MC(t,m) and NCC(t,m)
		for (JsonObject testMethod : ncc.keySet()) {
			if (!nc.containsKey(testMethod))  {
				for (JsonObject productionMethod : ncc.get(testMethod)) {
					if (!methodAssociation.containsKey(productionMethod))
						methodAssociation.put(productionMethod, new HashSet<JsonObject>());
					methodAssociation.get(productionMethod).add(testMethod);
				}
			}
		}

		// \neg\exists m'(NC(t,m') and NCC(t,m')) and MC(t,m)
		for (JsonObject testMethod : mc.keySet()) {
			if (!nc.containsKey(testMethod) && !ncc.containsKey(testMethod)) {
				for (JsonObject productionMethod : mc.get(testMethod)) {
					if (!methodAssociation.containsKey(productionMethod))
						methodAssociation.put(productionMethod, new HashSet<JsonObject>());
					methodAssociation.get(productionMethod).add(testMethod);
				}
			}
		}

		List<MethodAssociationOutput> ma = new ArrayList<MethodAssociationOutput>();
		for (JsonObject productionMethod : methodAssociation.keySet()) {
			MethodAssociationOutput relation = new MethodAssociationOutput();
			relation.addRelation(productionMethod, methodAssociation.get(productionMethod));
			ma.add(relation);
		}

		return ma;
	}

	public static List<MethodAssociationOutput> relateTestMethodWise(JsonArray jSegProductionMethod, JsonArray jSegTestMethod, CompilationUnit testCodeCu) {
		Type listType = new TypeToken<List<JsonObject>>() {}.getType();
		List<JsonObject> productionMethods = new Gson().fromJson(jSegProductionMethod, listType);
		List<JsonObject> testMethods = new Gson().fromJson(jSegTestMethod, listType);

		Map<JsonObject, Set<JsonObject>> mc = new HashMap<JsonObject, Set<JsonObject>>();
		for (JsonObject testMethod : testMethods) {
			mc.put(testMethod, GetProductionMethodsCalledBy(testMethod, productionMethods, testCodeCu));
		}

		Map<JsonObject, Set<JsonObject>> nc = GetNamingConvention(mc);
		Map<JsonObject, Set<JsonObject>> ncc = GetNamingConventionContains(mc);
		Map<JsonObject, Set<JsonObject>> methodAssociation = new HashMap<>();;

		// MC(t,m) and NC(t,m)
		for (JsonObject testMethod : nc.keySet()) {
			if (!methodAssociation.containsKey(testMethod))
				methodAssociation.put(testMethod, new HashSet<JsonObject>());
			methodAssociation.put(testMethod, nc.get(testMethod));
		}

		// \neg\exists m'(MC(t,m') and NC(t,m')) and MC(t,m) and NCC(t,m)
		for (JsonObject testMethod : ncc.keySet()) {
			if (!nc.containsKey(testMethod))  {
				if (!methodAssociation.containsKey(testMethod))
					methodAssociation.put(testMethod, new HashSet<JsonObject>());
				methodAssociation.get(testMethod).addAll(ncc.get(testMethod));
			}
		}

		// \neg\exists m'(NC(t,m') and NCC(t,m')) and MC(t,m)
		for (JsonObject testMethod : mc.keySet()) {
			if (!nc.containsKey(testMethod) && !ncc.containsKey(testMethod)) {
				if (!methodAssociation.containsKey(testMethod))
					methodAssociation.put(testMethod, new HashSet<JsonObject>());
				methodAssociation.get(testMethod).addAll(mc.get(testMethod));
			}
		}

		List<MethodAssociationOutput> ma = new ArrayList<MethodAssociationOutput>();
		for (JsonObject testMethod : methodAssociation.keySet()) {
			MethodAssociationOutput relation = new MethodAssociationOutput();
			relation.addRelation(testMethod, methodAssociation.get(testMethod));
			ma.add(relation);
		}

		return ma;
	}

	public static List<MethodAssociationOutput> relate_orginal(JsonArray jSegProductionMethod, JsonArray jSegTestMethod, CompilationUnit testCodeCu) {
		Type listType = new TypeToken<List<JsonObject>>() {}.getType();
		List<JsonObject> productionMethods = new Gson().fromJson(jSegProductionMethod, listType);
		List<JsonObject> testMethods = new Gson().fromJson(jSegTestMethod, listType);

		Map<JsonObject, Set<JsonObject>> mc = new HashMap<JsonObject, Set<JsonObject>>();
		for (JsonObject testMethod : testMethods) {
			mc.put(testMethod, GetProductionMethodsCalledBy(testMethod, productionMethods, testCodeCu));
		}

		Map<JsonObject, Set<JsonObject>> nc = GetNamingConvention(mc);
		Map<JsonObject, Set<JsonObject>> ncc = GetNamingConventionContains(mc);
		Map<JsonObject, Set<JsonObject>> methodAssociation = new HashMap<>();;

		// MC(t,m) and NC(t,m)
		for (JsonObject testMethod : nc.keySet()) {
			for (JsonObject productionMethod : nc.get(testMethod)) {
				if (!methodAssociation.containsKey(productionMethod))
					methodAssociation.put(productionMethod, new HashSet<JsonObject>());
				methodAssociation.get(productionMethod).add(testMethod);
			}
		}

		// \neg\exists m'(MC(t,m') and NC(t,m')) and MC(t,m) and NCC(t,m)
		for (JsonObject testMethod : ncc.keySet()) {
			if (!nc.containsKey(testMethod))  {
				for (JsonObject productionMethod : ncc.get(testMethod)) {
					if (!methodAssociation.containsKey(productionMethod))
						methodAssociation.put(productionMethod, new HashSet<JsonObject>());
					methodAssociation.get(productionMethod).add(testMethod);
				}
			}
		}

		// \neg\exists m'(NC(t,m') and NCC(t,m')) and MC(t,m)
		for (JsonObject testMethod : mc.keySet()) {
			if (!nc.containsKey(testMethod) && !ncc.containsKey(testMethod)) {
				for (JsonObject productionMethod : mc.get(testMethod)) {
					if (!methodAssociation.containsKey(productionMethod))
						methodAssociation.put(productionMethod, new HashSet<JsonObject>());
					methodAssociation.get(productionMethod).add(testMethod);
				}
			}
		}

		List<MethodAssociationOutput> ma = new ArrayList<MethodAssociationOutput>();
		for (JsonObject productionMethod : methodAssociation.keySet()) {
			MethodAssociationOutput relation = new MethodAssociationOutput();
			relation.addRelation(productionMethod, methodAssociation.get(productionMethod));
			ma.add(relation);
		}

		return ma;
	}

	static Map<JsonObject, Set<JsonObject>> GetNamingConventionContains(Map<JsonObject, Set<JsonObject>> mc) {
		Map<JsonObject, Set<JsonObject>> ncc = new HashMap<JsonObject, Set<JsonObject>>();
		for (JsonObject testMethod : mc.keySet()) {
			String testMethodQualifiedName = testMethod.get("unitTestMethod").getAsString();
			String testMethodName = GetSimpleMethodName(testMethodQualifiedName).toLowerCase();
			testMethodName = RemovePrefixSuffix(testMethodName, "test");

			for (JsonObject productionMethod : mc.get(testMethod)) {
				String productionQualifiedName = productionMethod.get("productionMethod").getAsString();
				String productionMethodName = GetSimpleMethodName(productionQualifiedName);

				if (testMethodName.contains(productionMethodName.toLowerCase())) {
					if (!ncc.containsKey(testMethod))
						ncc.put(testMethod, new HashSet<JsonObject>());

					ncc.get(testMethod).add(productionMethod);
				}
			}
		}
		return ncc;
	}

	static Map<JsonObject, Set<JsonObject>> GetNamingConvention(Map<JsonObject, Set<JsonObject>> mc) {
		Map<JsonObject, Set<JsonObject>> nc = new HashMap<JsonObject, Set<JsonObject>>();
		for (JsonObject tm : mc.keySet()) {
			String testMethodQualifiedName = tm.get("unitTestMethod").getAsString();
			String testMethodName = GetSimpleMethodName(testMethodQualifiedName).toLowerCase();
			testMethodName = RemovePrefixSuffix(testMethodName, "test");
	
			for (JsonObject pm : mc.get(tm)) {
				String productionQualifiedName = pm.get("productionMethod").getAsString();
				String productionMethodName = GetSimpleMethodName(productionQualifiedName).toLowerCase();
				if (testMethodName.equals(productionMethodName)) {
					if (!nc.containsKey(tm))
						nc.put(tm, new HashSet<JsonObject>());

					nc.get(tm).add(pm);
				}
			}
		}
		return nc;
	}

	static String RemovePrefixSuffix(String str, String pattern) {
		String out = str;
		if (str.startsWith(pattern)) {
			out = str.replaceFirst(pattern, "");
		}
		else if (str.endsWith(pattern)) {
			int pos = str.lastIndexOf(pattern);
			out = str.substring(0, pos);
		}
		return out;
	}

	static MethodDeclaration GetTestMethodByName(CompilationUnit cu, String testMethodName) {
		List<MethodDeclaration> methods = cu.getChildNodesByType(MethodDeclaration.class);
		for (MethodDeclaration m : methods) {
			if (!isDeprecated(m)) {
				if (isTestMethod(m)) {
					String name = m.resolve().getQualifiedSignature();
					if (testMethodName.equals(name)) {
						return m;
					}
				}
			}
		};
		return null;
	}

	static Set<String> GetCalleeIn(JsonObject testMethod, CompilationUnit cu, Set<String> productionMethodSimpleNames) {
		String testMethodName = testMethod.get("unitTestMethod").getAsString();
		MethodDeclaration tm = GetTestMethodByName(cu, testMethodName);
		Set<String> callee = new HashSet<String>();

		JsonArray offsets = testMethod.get("offset").getAsJsonArray();
		for (JsonElement e : offsets) {
			JsonObject offset = e.getAsJsonObject();
			int line = offset.get("line").getAsInt();
			int column = offset.get("column").getAsInt();

			tm.findAll(Expression.class).forEach(expr -> {
				if (expr instanceof MethodCallExpr) {
					if (productionMethodSimpleNames.contains(((MethodCallExpr)expr).getName().toString())) {
						if (line == expr.getRange().get().begin.line && column == expr.getRange().get().begin.column) {
							callee.add(((MethodCallExpr)expr).resolve().getQualifiedSignature());
						}
					}
				}
				else if (expr instanceof ObjectCreationExpr) {
					if (line == expr.getRange().get().begin.line && column == expr.getRange().get().begin.column) {
						callee.add(((ObjectCreationExpr)expr).resolve().getQualifiedSignature());
					}
				}
			});
		}
		return callee;
	}

	static Set<String> GetSimpleName(List<JsonObject> productionMethods) {
		Set<String> out = new HashSet<String>();
		for (JsonObject productionMethod : productionMethods) {
			String name = productionMethod.get("productionMethod").getAsString();
			String simpleName = name.substring(0, name.indexOf("("));
			simpleName = simpleName.substring(simpleName.lastIndexOf(".")+1).trim();
			out.add(simpleName);
		}
		return out;
	}

	static HashSet<JsonObject> GetProductionMethodsCalledBy(JsonObject testMethod, List<JsonObject> productionMethods, CompilationUnit cu) {
		HashSet<JsonObject> out = new HashSet<JsonObject>();

		Set<String> productionMethodSimpleNames = GetSimpleName(productionMethods);
		Set<String> callee = GetCalleeIn(testMethod, cu, productionMethodSimpleNames);

		for (JsonObject productionMethod : productionMethods) {
			String productionMethodName = productionMethod.get("productionMethod").getAsString();
			if (callee.contains(productionMethodName)) {
				out.add(productionMethod);
			}
		}

		return out;
	}

	static boolean isDeprecated(MethodDeclaration m) {
		List<Node> nodes = m.getChildNodes();
		for (Node node : nodes) {
			if (node instanceof MarkerAnnotationExpr || node instanceof NormalAnnotationExpr) {
				if (node.toString().contains("@Deprecated") || node.toString().contains("@deprecated")) {
					return true;
				}
			}
		}
		return false;
	}

	static boolean isTestMethod(MethodDeclaration m) {
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

	static String GetSimpleMethodName(String qualifiedName) {
		String simpleName =	qualifiedName.substring(0, qualifiedName.indexOf("("));
		return simpleName.substring(simpleName.lastIndexOf(".") + 1);
	}
}
