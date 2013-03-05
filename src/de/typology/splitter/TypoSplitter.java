package de.typology.splitter;

import java.io.BufferedWriter;
import java.io.IOException;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class TypoSplitter extends Splitter {
	private String extension;

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
		int edgeType;
		String[] sequence;
		BufferedWriter writer;
		for (int sequenceLength = 1; sequenceLength <= maxSequenceLength; sequenceLength++) {
			edgeType = sequenceLength - 1;
			this.extension = edgeType + "es";
			IOHelper.log("splitting into " + this.extension);
			this.initialize(this.extension);
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
			this.sortAndAggregate(this.outputDirectory.getAbsolutePath() + "/"
					+ this.extension);
		}
	}
}