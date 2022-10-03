package org.DoTeLink;

import org.DoTeLink.segmentation.*;
import org.DoTeLink.inference.*;
import org.DoTeLink.util.JSSTypeSolver;

import java.util.List;
import java.util.HashMap;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import org.apache.commons.cli.*;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class Main {
	public static void main(String[] args) {
		CommandLine cmd = GetCommandLineArguments(args);

		String productionClazzName = cmd.getOptionValue("production_class");
		String testClazzName = cmd.getOptionValue("test_class");
		String projectDir = cmd.getOptionValue("project_dir");

		TypeSolver typeSolver = JSSTypeSolver.getTypeSolver(projectDir, productionClazzName, testClazzName);

		/* comment segmentation module */
		CompilationUnit productionCu = JSSTypeSolver.getCu(typeSolver, productionClazzName);
		JsonArray jSegProductionMethods = CommentSegmentation.segregate(productionCu);
		
		/* test code segmentation module */
		CompilationUnit testCodeCu = JSSTypeSolver.getCu(typeSolver, testClazzName);
		JsonArray jSegTestMethods = TestCodeSegmentation.segregate(testCodeCu);

		/* inference module */
		CompilationUnit testCodeCu2 = JSSTypeSolver.getCu(typeSolver, testClazzName);
		JsonArray jMethodAllLinks = MethodAll.relate(jSegProductionMethods, jSegTestMethods, testCodeCu2);
		JsonArray jMethodOneLinks = MethodOne.relate(jSegProductionMethods, jSegTestMethods, testCodeCu2);
		JsonArray jMethodBestLinks = MethodBest.relate(jSegProductionMethods, jSegTestMethods, testCodeCu2);
		JsonArray jDoTeLinks = DoTeLink.relate(jSegProductionMethods, jSegTestMethods, testCodeCu2);

		/* write each output */
		WriteToFile(jSegProductionMethods, productionClazzName, "output/sentence");
		WriteToFile(jSegTestMethods, testClazzName, "output/testCodeSnippet");
		WriteToFile(jMethodAllLinks, productionClazzName, "output/link/methodAll");
		WriteToFile(jMethodOneLinks, productionClazzName, "output/link/methodOne");
		WriteToFile(jMethodBestLinks, productionClazzName, "output/link/methodBest");
		WriteToFile(jDoTeLinks, productionClazzName, "output/link/dotelink");
	}

	private static CommandLine GetCommandLineArguments(String[] args) {
		Options opts = new Options();

		Option productionOpt = new Option("production_class", true, "a production class");
		Option testOpt = new Option("test_class", true, "a test class");
		Option projectOpt = new Option("project_dir", true, "home directory of the project");

		productionOpt.setRequired(true);
		testOpt.setRequired(true);
		projectOpt.setRequired(true);

		opts.addOption(productionOpt);
		opts.addOption(testOpt);
		opts.addOption(projectOpt);

		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(opts, args);
		}
		catch (ParseException e) {
			e.printStackTrace();
		}
		return cmd;
	}

	private static void WriteToFile(JsonArray jOutput, String clazzName, String outputDir) {
		try {
			File folder = new File(outputDir);
			folder.mkdirs();

			String outputName = clazzName.substring(clazzName.lastIndexOf("/"));
			BufferedWriter fw = new BufferedWriter(new FileWriter(outputDir + "/" + outputName.replaceAll(".java", ".json")));
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			fw.write(gson.toJson(jOutput));
			fw.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
}
