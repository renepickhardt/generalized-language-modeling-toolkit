package de.typology.splitter;

import de.typology.utils.Config;

public class TypoSplitterWithCount extends TypoSplitter {

	public TypoSplitterWithCount(String directory, String indexName,
			String statsName, String inputName) {
		super(directory, indexName, statsName, inputName);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputDirectory = Config.get().outputDirectory
				+ Config.get().inputDataSet;
		TypoSplitterWithCount tswc = new TypoSplitterWithCount(outputDirectory,
				"index.txt", "stats", "normalized.txt");
		tswc.split(5);
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