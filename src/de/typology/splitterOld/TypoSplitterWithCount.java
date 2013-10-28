package de.typology.splitterOld;

import java.io.BufferedWriter;
import java.io.IOException;

import de.typology.utilsOld.Config;
import de.typology.utilsOld.IOHelper;

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
	public void split(int sequenceLength) {
		int edgeType;
		BufferedWriter writer;
		edgeType = sequenceLength - 1;
		this.extension = edgeType + "es";
		IOHelper.strongLog("splitting into " + this.extension);
		this.initialize(this.extension);
		while (this.getNextSequence(sequenceLength)) {
			writer = this.getWriter(this.sequence[0]);
			try {
				writer.write(this.sequence[0] + "\t");
				writer.write(this.sequence[this.sequence.length - 1] + "\t");
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