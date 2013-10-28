package de.typology.splitterOld;

import java.io.BufferedWriter;
import java.io.IOException;

import de.typology.utilsOld.Config;
import de.typology.utilsOld.IOHelper;

public class NGramSplitterWithCount extends NGramSplitter {

	public NGramSplitterWithCount(String directory, String indexName,
			String statsName, String inputName) {
		super(directory, indexName, statsName, inputName);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputDirectory = Config.get().outputDirectory
				+ Config.get().inputDataSet;
		NGramSplitterWithCount ngswc = new NGramSplitterWithCount(
				outputDirectory, "index.txt", "stats", "normalized.txt");
		ngswc.split(5);
	}

	@Override
	public void split(int sequenceLength) {
		BufferedWriter writer;
		IOHelper.strongLog("splitting into " + sequenceLength + "grams");
		this.extension = sequenceLength + "gs";
		this.initialize(this.extension);
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

	@Override
	protected boolean getNextSequence(int sequenceLength) {
		return this.getNextSequenceWithCount(sequenceLength);
	}

	@Override
	protected void initialize(String extension) {
		this.initializeWithLength(extension);
	}
}
