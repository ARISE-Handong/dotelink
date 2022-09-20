package org.DoTeLink.util;

import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.SourceRoot;
import com.github.javaparser.resolution.UnsolvedSymbolException;

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
		try {
			return jp.parse(new File(targetClazz)).getResult().get();
		}
		catch (IOException e) { }
		return null;
	}
}
