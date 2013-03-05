package de.typology.splitter;

import java.io.BufferedWriter;
import java.io.IOException;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class NGramSplitter extends Splitter {
	private String extension;

	public NGramSplitter(String indexPath, String inputPath) {
		super(indexPath, inputPath, "ngrams");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String dataSet = "testwiki";
		NGramSplitter ngs = new NGramSplitter(Config.get().outputDirectory
				+ dataSet + "/index.txt", Config.get().outputDirectory
				+ dataSet + "/normalized.txt");
		ngs.split(5);
	}

	@Override
	public void split(int maxSequenceLength) {
		String[] sequence;
		BufferedWriter writer;
		for (int sequenceLength = 1; sequenceLength <= maxSequenceLength; sequenceLength++) {
			IOHelper.log("splitting into " + sequenceLength + "grams");
			this.extension = sequenceLength + "gs";
			this.initialize(this.extension);
			while ((sequence = this.getSequence(sequenceLength)) != null) {
				writer = this.getWriter(sequence[0]);
				try {
					for (String word : sequence) {
						writer.write(word + "\t");
					}
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
