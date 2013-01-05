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
	private String outputPathWithNGramType;
	private ArrayList<File> files;
	private HashMap<String, Integer> nMinusOneGrams;

	private String line;
	private String[] lineSplit;
	String nMinusOneGram;
	private int nGramCount;

	/**
	 * @param args
	 * @throws IOException
	 * @throws NumberFormatException
	 * @author Martin Koerner
	 */
	public static void main(String[] args) throws NumberFormatException,
	IOException {
		NGramNormalizer ngn = new NGramNormalizer();
		IOHelper.strongLog("normalizing ngrams from "
				+ Config.get().nGramsInput + " and storing updated ngrams at "
				+ Config.get().normalizedNGrams);
		double time = ngn.normalize(Config.get().nGramsInput,
				Config.get().normalizedNGrams);
		IOHelper.strongLog("time for normalizing ngrams from "
				+ Config.get().nGramsInput + " : " + time);
	}

	/**
	 * given a directory containing ngrams (2grams to 5grams) this function
	 * copies the ngrams into new files and replaces the ngram counts by its
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
		for (int nGramType = 2; nGramType < 6; nGramType++) {
			this.files = IOHelper.getDirectory(new File(inputPath + nGramType));
			this.outputPathWithNGramType = outputPath + nGramType + "/";
			new File(this.outputPathWithNGramType).mkdir();
			for (File file : this.files) {
				if (file.getName().contains("distribution")) {
					IOHelper.log("skipping " + file.getAbsolutePath());
					continue;
				}
				// aggregate outgoing ngram counts for each node
				this.reader = IOHelper.openReadFile(file.getAbsolutePath());
				this.nMinusOneGrams = new HashMap<String, Integer>();
				while ((this.line = this.reader.readLine()) != null) {
					// extract information from line
					// line format: ngram\t#nGramCount\n
					this.lineSplit = this.line.split("\t");
					if (this.lineSplit.length != nGramType + 1) {
						continue;
					}

					this.nMinusOneGram = "";
					for (int i = 0; i < nGramType - 2; i++) {
						this.nMinusOneGram += this.lineSplit[i] + "\t";
					}
					this.nMinusOneGram += this.lineSplit[nGramType - 2];

					this.nGramCount = Integer
							.parseInt(this.lineSplit[this.lineSplit.length - 1]
									.substring(1));
					if (!this.nMinusOneGrams.containsKey(this.nMinusOneGram)) {
						this.nMinusOneGrams.put(this.nMinusOneGram,
								this.nGramCount);
					} else {
						this.nMinusOneGrams.put(this.nMinusOneGram,
								this.nMinusOneGrams.get(this.nMinusOneGram)
								+ this.nGramCount);
					}
				}
				this.reader.close();

				// normalize ngram counts
				this.reader = IOHelper.openReadFile(file.getAbsolutePath());

				String fileName = file.getName();
				if (fileName.endsWith("gs")) {
					fileName = fileName.substring(0, fileName.length() - 2)
							+ "n";
				}

				this.writer = IOHelper.openWriteFile(
						this.outputPathWithNGramType + fileName,
						32 * 1024 * 1024);
				while ((this.line = this.reader.readLine()) != null) {
					// extract information from line
					// line format: ngram\t#nGramCount\n
					this.lineSplit = this.line.split("\t");
					if (this.lineSplit.length != nGramType + 1) {
						continue;
					}

					this.nMinusOneGram = "";
					for (int i = 0; i < nGramType - 2; i++) {
						this.nMinusOneGram += this.lineSplit[i] + "\t";
					}
					this.nMinusOneGram += this.lineSplit[nGramType - 2];

					this.nGramCount = Integer
							.parseInt(this.lineSplit[this.lineSplit.length - 1]
									.substring(1));

					if (this.nMinusOneGrams.containsKey(this.nMinusOneGram)) {
						// write updated edge to new file
						this.writer.write(this.nMinusOneGram + "\t"
								+ this.lineSplit[nGramType - 1] + "\t#"
								+ (double) this.nGramCount
								/ this.nMinusOneGrams.get(this.nMinusOneGram)
								+ "\n");
					} else {
						IOHelper.strongLog("no ngram count for:"
								+ this.lineSplit[0] + " in file: "
								+ file.getName());
					}
				}
				this.reader.close();
				this.writer.close();
				//file.delete();
			}
		}
		long endTime = System.currentTimeMillis();
		return (endTime - startTime) / 1000;
	}
	public double removeNGrams(String inputPath, String outputPath) throws IOException{
		long startTime = System.currentTimeMillis();
		new File(outputPath).mkdir();
		for (int nGramType = 2; nGramType < 6; nGramType++) {
			this.files = IOHelper.getDirectory(new File(inputPath + nGramType));
			this.outputPathWithNGramType = outputPath + nGramType + "/";
			new File(this.outputPathWithNGramType).mkdir();
			for (File file : this.files) {
				this.reader = IOHelper.openReadFile(file.getAbsolutePath());
				String fileName = file.getName();
				this.writer = IOHelper.openWriteFile(
						this.outputPathWithNGramType + fileName,
						32 * 1024 * 1024);
				while ((this.line = this.reader.readLine()) != null) {
					this.lineSplit = this.line.split("\t");
					if (this.lineSplit.length != nGramType + 1) {
						continue;
					}
					this.writer.write(this.line+"\n");
				}
				this.reader.close();
				this.writer.close();
			}
		}
		long endTime = System.currentTimeMillis();
		return (endTime - startTime) / 1000;
	}

}
