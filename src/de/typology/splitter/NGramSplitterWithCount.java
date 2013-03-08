package de.typology.splitter;

import de.typology.utils.Config;

public class NGramSplitterWithCount extends NGramSplitter {

	public NGramSplitterWithCount(String directory, String indexName,
			String inputName) {
		super(directory, indexName, inputName);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String dataSet = "wiki/testwiki/";
		String outputDirectory = Config.get().outputDirectory + dataSet;
		NGramSplitterWithCount ngswc = new NGramSplitterWithCount(
				outputDirectory, "index.txt", "normalized.txt");
		ngswc.split(5);
	}

	@Override
	protected boolean getNextSequence(int sequenceLength) {
		return this.getNextSequenceWithCount(sequenceLength);
	}

	@Override
	protected void initialize(String extension, int sequenceLength) {
		this.initializeWithLength(extension, sequenceLength);
	}
}
