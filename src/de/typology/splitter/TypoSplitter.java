package de.typology.splitter;

import java.io.BufferedWriter;
import java.io.IOException;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class TypoSplitter extends Splitter {

	public TypoSplitter(String indexPath, String inputPath) {
		super(indexPath, inputPath, "typos");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String dataSet = "testwiki";
		TypoSplitter ngs = new TypoSplitter(Config.get().outputDirectory
				+ dataSet + "/index.txt", Config.get().outputDirectory
				+ dataSet + "/normalized.txt");
		ngs.split(5);
	}

	@Override
	public void split(int maxSequenceLength) {
		String[] sequence;
		BufferedWriter writer;
		for (int sequenceLength = 1; sequenceLength <= maxSequenceLength; sequenceLength++) {
			IOHelper.log("splitting into " + (sequenceLength - 1) + "es");
			this.initialize(sequenceLength - 1 + "es_split");
			while ((sequence = this.getSequence(sequenceLength)) != null) {
				writer = this.getWriter(sequence[0]);
				try {
					writer.write(sequence[0] + "\t");
					writer.write(sequence[sequence.length - 1] + "\t");
					writer.write("\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			this.reset();
		}
	}
}