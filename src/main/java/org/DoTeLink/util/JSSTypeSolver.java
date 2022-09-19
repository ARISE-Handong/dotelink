package org.DoTeLink.util;

import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.resolution.UnsolvedSymbolException;

import com.github.javaparser.JavaToken;
import static com.github.javaparser.GeneratedJavaParserConstants.MULTI_LINE_COMMENT;

import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;

import com.github.javaparser.JavaParser;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.ParserConfiguration;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.Node.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.*;
import com.github.javaparser.ast.nodeTypes.NodeWithIdentifier;
import com.github.javaparser.ParserConfiguration;

import java.nio.file.Paths;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.FileNotFoundException; 
import java.io.PrintWriter;
import java.io.IOException;
import java.util.*;
import org.apache.commons.io.FileUtils;

/**
 * Some code that uses JavaParser.
 */
public class JSSTypeSolver {

	public JSSTypeSolver() {}

	public static TypeSolver getTypeSolver(String projectDir, String productionClazzName, String testClazzName) {
		try {
			File proj = new File(projectDir);
			CombinedTypeSolver typeSolver = new CombinedTypeSolver(new ReflectionTypeSolver());

			Collection<File> jars = FileUtils.listFiles(proj, new String[]{"jar"}, true);
			for (File jar : jars) {
				System.out.println(jar.getAbsolutePath());
				typeSolver.add(JarTypeSolver.getJarTypeSolver(jar.getAbsolutePath()));
			}

			String pPackage = GetPackageDir(productionClazzName);
			String tPackage = GetPackageDir(testClazzName);

			List<File> subdirectories = GetSubdirectories(proj);
			HashSet<String> rootPackages = new HashSet<String>();
			for (File subdir : subdirectories) {
				String absDir = subdir.getAbsolutePath();
				if (absDir.contains(pPackage) || absDir.contains(tPackage)) {
					String rootPackage = absDir.substring(0, absDir.indexOf(pPackage));
					if (!rootPackages.contains(rootPackage)) {
						System.out.println(rootPackage);
						rootPackages.add(rootPackage);
					}
				}
			}

			for (String rootPackage : rootPackages) {
				typeSolver.add(new JavaParserTypeSolver(rootPackage)); 
			}

			return typeSolver;
		}
		catch (IOException e) { }
		return null;
	}

	static List<File> GetSubdirectories(File dir) {
		List<File> subdirs = Arrays.asList(dir.listFiles(new FileFilter() {
			public boolean accept(File f) {
				return f.isDirectory();
			}
		}));
		subdirs = new ArrayList<File>(subdirs);

		List<File> deepSubdirs = new ArrayList<File>();
		for(File subdir : subdirs) {
			deepSubdirs.addAll(GetSubdirectories(subdir)); 
		}
		subdirs.addAll(deepSubdirs);
		return subdirs;
	}

	static String GetPackageDir(String clazzName) {
		String dir = "";
		try {
			File f = new File(clazzName);
			BufferedReader br = new BufferedReader(new FileReader(f));
			dir = br.readLine();
			while (dir != null) {
				if (dir.trim().startsWith("package")) {
					return dir.replaceAll("package ", "").replaceAll("\\.", "/").replace(";", "");
				}
				dir = br.readLine();
			}
		}
		catch (IOException e) { }
		return null;
	}

	public static CompilationUnit getCu(TypeSolver typeSolver, String targetClazz) {
		ParserConfiguration pConf = new ParserConfiguration();
		JavaParser jp = new JavaParser(pConf.setSymbolResolver(new JavaSymbolSolver(typeSolver)));
//		StaticJavaParser.getConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver));
		try {
			return jp.parse(new File(targetClazz)).getResult().get();
//			return StaticJavaParser.parse(new File(targetClazz));
		}
		catch (IOException e) { }
		return null;
	}

	public static CompilationUnit getCu(String targetClass, String projectDir) {
		System.out.println("Parsing " + targetClass);
		try {
			TypeSolver typeSolver = new CombinedTypeSolver(
/*
					JarTypeSolver.getJarTypeSolver(projectDir + "/lib/jfreesvg-2.0.jar"),
					JarTypeSolver.getJarTypeSolver(projectDir + "/lib/orsonpdf-1.6-eval.jar"),
*/
					JarTypeSolver.getJarTypeSolver(projectDir + "/lib/jfreechart-1.0.19.jar"),
					JarTypeSolver.getJarTypeSolver(projectDir + "/lib/orsoncharts-1.4-eval-nofx.jar"),
					JarTypeSolver.getJarTypeSolver(projectDir + "/lib/jfreechart-1.0.19-swt.jar"),
					JarTypeSolver.getJarTypeSolver(projectDir + "/lib/hamcrest-core-1.3.jar"),
					JarTypeSolver.getJarTypeSolver(projectDir + "/lib/servlet.jar"),
					JarTypeSolver.getJarTypeSolver(projectDir + "/lib/jfreechart-1.0.19-experimental.jar"),
					JarTypeSolver.getJarTypeSolver(projectDir + "/lib/swtgraphics2d.jar"),
					new ReflectionTypeSolver(),
					JarTypeSolver.getJarTypeSolver(projectDir + "/lib/jcommon-1.0.23.jar"),
					JarTypeSolver.getJarTypeSolver(projectDir + "/lib/junit-4.11.jar"),
					new JavaParserTypeSolver(new File(projectDir + "/source")),
					new JavaParserTypeSolver(new File(projectDir + "/tests"))
			);

			StaticJavaParser.getConfiguration().setSymbolResolver(new JavaSymbolSolver(typeSolver));
			return StaticJavaParser.parse(new File(targetClass));
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	static TypeSolver getTypeSolver(String rootPath, String project) {
		try {
			if (project.equals("collections"))
				return collections_tc(rootPath);
			else if (project.equals("jfreechart"))
				return jfreechart_ts(rootPath);
			else if (project.equals("ant"))
				return ant_ts(rootPath);
			else if (project.equals("drjava"))
				return drjava_ts(rootPath);
			else if (project.equals("log4j"))
				return log4j_ts(rootPath);
			else if (project.equals("jenetics"))
				return jenetics_ts(rootPath);
			else if (project.equals("lucene"))
				return lucene_ts(rootPath);
			else if (project.equals("supercsv"))
				return supercsv_ts(rootPath);
			else if (project.equals("weka"))
				return weka_ts(rootPath);
		}
		catch (IOException e) { 
			System.err.println("Failed to parse " + project + " in " + rootPath);
		}
		return null;
	}

	static TypeSolver ant_ts(String rootPath) throws IOException {
		String antDir = rootPath + "/apache-ant-1.10.7/";
		TypeSolver typeSolver = new CombinedTypeSolver(
				new ReflectionTypeSolver(),
				JarTypeSolver.getJarTypeSolver(antDir + "lib/optional/junit-4.12.jar"),
				JarTypeSolver.getJarTypeSolver(antDir + "lib/optional/hamcrest-core-1.3.jar"),
				JarTypeSolver.getJarTypeSolver(antDir + "lib/optional/hamcrest-library-1.3.jar"),

				new JavaParserTypeSolver(new File(antDir + "src/main")),
				new JavaParserTypeSolver(new File(antDir + "src/tests/junit"))
		);
		return typeSolver;
	}

	// Edited `CollectionUtils, ClosureUtils, IteratorUtils` due to JSS bug
	static TypeSolver collections_tc(String rootPath) throws IOException {
		String collectionDir = rootPath + "/commons-collections4-4.4/";
		TypeSolver typeSolver = new CombinedTypeSolver(
				new ReflectionTypeSolver(),
				JarTypeSolver.getJarTypeSolver("./collections_jar/junit-4.12.jar"),
				JarTypeSolver.getJarTypeSolver("./collections_jar/dom-2.3.0-jaxb-1.0.6.jar"),
				JarTypeSolver.getJarTypeSolver("./collections_jar/easymock-3.6.jar"),
				JarTypeSolver.getJarTypeSolver(collectionDir + "target/commons-collections4-4.4.jar"),
				JarTypeSolver.getJarTypeSolver(collectionDir + "target/commons-collections4-4.4-sources.jar"),
				JarTypeSolver.getJarTypeSolver(collectionDir + "target/commons-collections4-4.4-tests.jar"),
				JarTypeSolver.getJarTypeSolver(collectionDir + "target/commons-collections4-4.4-test-sources.jar"),
				new JavaParserTypeSolver(new File(collectionDir + "src/main/java/org/apache/commons/collections4"))
//				new JavaParserTypeSolver(new File(collectionDir + "src/test/java/org/apache/commons/collections4"))
		);
		return typeSolver;
	}

	static TypeSolver jfreechart_ts(String rootPath) throws IOException {
		String dir = rootPath + "/jfreechart-1.0.19/";
		TypeSolver typeSolver = new CombinedTypeSolver(
				new ReflectionTypeSolver(),
				JarTypeSolver.getJarTypeSolver(dir + "lib/jcommon-1.0.23.jar"),
				JarTypeSolver.getJarTypeSolver(dir + "lib/junit-4.11.jar"),
				new JavaParserTypeSolver(new File(dir + "source")),
				new JavaParserTypeSolver(new File(dir + "tests"))
		);
		return typeSolver;
	}


	static TypeSolver drjava_ts(String rootPath) throws IOException {
		String drjavaDir = rootPath + "/dr/";
		TypeSolver typeSolver = new CombinedTypeSolver(
				new ReflectionTypeSolver(),
				JarTypeSolver.getJarTypeSolver("./drjava_jar/sax-2.0.1.jar"),
				JarTypeSolver.getJarTypeSolver("./drjava_jar/dom-2.3.0-jaxb-1.0.6.jar"),
				JarTypeSolver.getJarTypeSolver(drjavaDir + "drjava-code/drjava/drjava.jar"),
				new JavaParserTypeSolver(new File(drjavaDir + "drjava-code/drjava/src"))
		);
		return typeSolver;
	}

	static TypeSolver lucene_ts(String rootPath) throws IOException {
		String luceneDir = rootPath + "/lucene/";
		TypeSolver typeSolver = new CombinedTypeSolver(
				new ReflectionTypeSolver(),
				JarTypeSolver.getJarTypeSolver(luceneDir + "build/core/lucene-core-8.4.1-SNAPSHOT.jar"),
				new JavaParserTypeSolver(new File(luceneDir + "queries/src/java")),
				new JavaParserTypeSolver(new File(luceneDir + "core/src/java"))
		);
		return typeSolver;
	}

	static TypeSolver weka_ts(String rootPath) throws IOException {
		String dir = rootPath + "/weka/";
		TypeSolver typeSolver = new CombinedTypeSolver(
				new ReflectionTypeSolver(),
//				JarTypeSolver.getJarTypeSolver(dir + "lib/junit-4.13-rc-1.jar"),
				new JavaParserTypeSolver(new File(dir + "src/main/java"))
//				new JavaParserTypeSolver(new File(dir + "src/test/java"))
		);
		return typeSolver;
	}

	static TypeSolver supercsv_ts(String rootPath) throws IOException {
		String dir = rootPath + "/super-csv-2.4.0/";
		TypeSolver typeSolver = new CombinedTypeSolver(
				new ReflectionTypeSolver(),
				JarTypeSolver.getJarTypeSolver("./junit-4.12.jar"),
				new JavaParserTypeSolver(new File(dir + "super-csv/src/main/java")),
				new JavaParserTypeSolver(new File(dir + "super-csv/src/test/java"))
		);
		return typeSolver;
	}

	static TypeSolver log4j_ts(String rootPath) throws IOException {
		String log4jDir = rootPath + "/apache-log4j-2.13.0-src/";
		TypeSolver typeSolver = new CombinedTypeSolver(
				new ReflectionTypeSolver(),
				JarTypeSolver.getJarTypeSolver("./junit-4.12.jar"),
				new JavaParserTypeSolver(new File(log4jDir + "mainsrc/main/java")),
				new JavaParserTypeSolver(new File(log4jDir + "log4j-core/src/main/java"))
//				new JavaParserTypeSolver(new File(log4jDir + "log4j-core/src/test/java"))
		);
		return typeSolver;
	}

	// Edited `BitsTest.java` due to JSS bug
	static TypeSolver jenetics_ts(String rootPath) throws IOException {
		String jeneticsDir = rootPath + "/jenetics-5.2.0/jenetics/";
		TypeSolver typeSolver = new CombinedTypeSolver(
				new ReflectionTypeSolver(),
//				JarTypeSolver.getJarTypeSolver("./jenetics_jar/testng-7.1.0.jar"),
//				JarTypeSolver.getJarTypeSolver("./jenetics_jar/prngine-1.0.1.jar"),
//				JarTypeSolver.getJarTypeSolver("./jenetics_jar/equalsverifier-3.1.12.jar"),
				JarTypeSolver.getJarTypeSolver(jeneticsDir + "build/libs/jenetics-5.2.0.jar"),
				new JavaParserTypeSolver(new File(jeneticsDir + "src/jmh/java")),
				new JavaParserTypeSolver(new File(jeneticsDir + "src/main/java"))
//				new JavaParserTypeSolver(new File(jeneticsDir + "src/test/java"))
		);
		return typeSolver;
	}
}
