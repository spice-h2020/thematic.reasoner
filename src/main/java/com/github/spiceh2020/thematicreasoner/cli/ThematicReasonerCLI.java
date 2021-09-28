package com.github.spiceh2020.thematicreasoner.cli;

import java.io.FileNotFoundException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.github.spiceh2020.thematicreasoner.ThematicReasoner;
import com.github.spiceh2020.thematicreasoner.ThematicReasoner.OutputStrategy;

public class ThematicReasonerCLI {

	private static String INPUT = "i";
	private static String OUTPUT_STRATEGY = "s";
	private static String OUTPUT_FILE = "o";
	private static String BASE_URI = "b";

	public static void main(String[] args) {
		Options options = new Options();

		options.addOption(Option.builder(INPUT).argName("path").hasArg().required(true)
				.desc("The path to the file storing the input data about the collection of artworks to process.")
				.longOpt("input").build());

		options.addOption(Option.builder(OUTPUT_STRATEGY).argName("[console|rdf]").hasArg().required(false)
				.desc("The output strategy [Default: console].").longOpt("output-strategy").build());

		options.addOption(Option.builder(OUTPUT_FILE).argName("filepath").hasArg().required(false)
				.desc("The path to the output file [Default: out.ttl].").longOpt("output-file").build());

		options.addOption(Option.builder(BASE_URI).argName("uri").hasArg().required(false)
				.desc("The namespace of the new URIs that will be created.").longOpt("base-uri").build());

		CommandLine commandLine = null;

		CommandLineParser cmdLineParser = new DefaultParser();
		try {
			commandLine = cmdLineParser.parse(options, args);

			String input = commandLine.getOptionValue(INPUT);

			ThematicReasoner tr = new ThematicReasoner(input);

			OutputStrategy os = OutputStrategy.PRINT;

			if (commandLine.hasOption(OUTPUT_STRATEGY)) {
				if (commandLine.getOptionValue(OUTPUT_STRATEGY).equals("rdf")) {
					os = OutputStrategy.RDF;
				}
			}

			String out = "out.ttl";
			String baseURI = "";

			if (commandLine.hasOption(OUTPUT_FILE)) {
				os = OutputStrategy.RDF;

				if (commandLine.hasOption(BASE_URI)) {
					baseURI = commandLine.getOptionValue(BASE_URI);
				}

				out = commandLine.getOptionValue(OUTPUT_FILE);

			}

			tr.setOutputStrategy(os);
			tr.setOutputFile(out);
			tr.setBaseURI(baseURI);

			tr.run();

		} catch (ParseException e) {
			printHelp(options);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

	private static void printHelp(Options options) {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("java -jar thematic.reasoner-ex-<version>.jar  -i path [-b uri] [-o filepath] [-s strategy] ", options); // TODO
	}

}
