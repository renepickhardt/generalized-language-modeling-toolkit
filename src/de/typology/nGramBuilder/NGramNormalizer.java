package de.typology.nGramBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class NGramNormalizer {
	private BufferedReader reader;
	private BufferedWriter writer;
	private String outputPathWithRelType;
	private ArrayList<File> files;
	private HashMap<String, Integer> outgoingEdges;

	private String line;
	private String[] lineSplit;
	private int edgeCount;

	/**
	 * @param args
	 * @throws IOException
	 * @throws NumberFormatException
	 * @author Martin Koerner
	 */
	public static void main(String[] args) throws NumberFormatException,
			IOException {
		NGramNormalizer ngn = new NGramNormalizer();
		IOHelper.strongLog("normalizing edges from " + Config.get().edgeInput
				+ " and storing updated edges at "
				+ Config.get().normalizedEdges);
		double time = ngn.normalize(Config.get().edgeInput,
				Config.get().normalizedEdges);
		IOHelper.strongLog("time for normalizing edges from "
				+ Config.get().edgeInput + " : " + time);
	}

	/**
	 * given a directory containing typology edges (type 1 to 4) this function
	 * copies the edges into new files and replaces the edge counts by its
	 * probabilities.
	 * 
	 * @param inputPath
	 * @param outputPath
	 * @return
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public double normalize(String inputPath, String outputPath)
			throws NumberFormatException, IOException {
		long startTime = System.currentTimeMillis();
		new File(outputPath).mkdir();
		for (int relType = 1; relType < 5; relType++) {
			this.files = IOHelper.getDirectory(new File(inputPath + relType));
			this.outputPathWithRelType = outputPath + relType + "/";
			new File(this.outputPathWithRelType).mkdir();
			for (File file : this.files) {
				if (file.getName().contains("distribution")) {
					IOHelper.log("skipping " + file.getAbsolutePath());
					continue;
				}
				// aggregate outgoing edge counts for each node
				this.reader = IOHelper.openReadFile(file.getAbsolutePath());
				this.outgoingEdges = new HashMap<String, Integer>();
				while ((this.line = this.reader.readLine()) != null) {
					// extract information from line
					// line format: word\tword\t#edgeCount\n
					this.lineSplit = this.line.split("\t");
					if (this.lineSplit.length < 3) {
						continue;
					}
					this.edgeCount = Integer
							.parseInt(this.lineSplit[this.lineSplit.length - 1]
									.substring(1));
					if (!this.outgoingEdges.containsKey(this.lineSplit[0])) {
						this.outgoingEdges.put(this.lineSplit[0],
								this.edgeCount);
					} else {
						this.outgoingEdges.put(this.lineSplit[0],
								this.outgoingEdges.get(this.lineSplit[0])
										+ this.edgeCount);
					}
				}
				this.reader.close();

				// normalize edge counts
				this.reader = IOHelper.openReadFile(file.getAbsolutePath());
				this.writer = IOHelper.openWriteFile(this.outputPathWithRelType
						+ file.getName(), 32 * 1024 * 1024);
				while ((this.line = this.reader.readLine()) != null) {
					// extract information from line
					// line format: word\tword\t#edgeCount\n
					this.lineSplit = this.line.split("\t");
					if (this.lineSplit.length < 3) {
						continue;
					}
					this.edgeCount = Integer
							.parseInt(this.lineSplit[this.lineSplit.length - 1]
									.substring(1));
					if (this.outgoingEdges.containsKey(this.lineSplit[0])) {
						// write updated edge to new file
						this.writer.write(this.lineSplit[0] + "\t"
								+ this.lineSplit[1] + "\t#"
								+ (double) this.edgeCount
								/ this.outgoingEdges.get(this.lineSplit[0])
								+ "\n");
					} else {
						IOHelper.strongLog("no edge count for:"
								+ this.lineSplit[0] + " in file: "
								+ file.getName());
					}
				}
				this.reader.close();
				this.writer.close();
			}
		}
		long endTime = System.currentTimeMillis();
		return (endTime - startTime) / 1000;
	}
}
