package de.typology.splitter;

import java.io.BufferedWriter;
import java.io.IOException;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class NGramSplitter extends Splitter {
	private String extension;

	public NGramSplitter(String directory, String indexName, String statsName,
			String inputName) {
		super(directory, indexName, statsName, inputName, "ngrams");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputDirectory = Config.get().outputDirectory
				+ Config.get().inputDataSet;
		NGramSplitter ngs = new NGramSplitter(outputDirectory, "index.txt",
				"stats.txt", "normalized.txt");
		ngs.split(5);
	}

	@Override
	public void split(int maxSequenceLength) {
		BufferedWriter writer;
		for (int sequenceLength = 1; sequenceLength <= maxSequenceLength; sequenceLength++) {
			IOHelper.strongLog("splitting into " + sequenceLength + "grams");
			this.extension = sequenceLength + "gs";
			this.initialize(this.extension, sequenceLength);
			while (this.getNextSequence(sequenceLength)) {
				writer = this.getWriter(this.sequence[0]);
				try {
					for (String word : this.sequence) {
						writer.write(word + "\t");
					}
					writer.write(this.sequenceCount + "\n");
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
