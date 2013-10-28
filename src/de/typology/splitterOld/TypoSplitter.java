package de.typology.splitterOld;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import de.typology.utils.Config;
import de.typology.utilsOld.IOHelper;
import de.typology.utilsOld.SystemHelper;

public class TypoSplitter extends Splitter {
	protected String extension;

	public TypoSplitter(String directory, String indexName, String statsName,
			String inputName) {
		super(directory, indexName, statsName, inputName, "typos");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputDirectory = Config.get().outputDirectory
				+ Config.get().inputDataSet;
		TypoSplitter ts = new TypoSplitter(outputDirectory, "index.txt",
				"stats.txt", "training.txt");
		ts.split(5);
	}

	@Override
	public void split(int maxSequenceLength) {
		int edgeType;
		BufferedWriter writer;
		for (int sequenceLength = 1; sequenceLength <= maxSequenceLength; sequenceLength++) {
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
	}

	@Override
	protected void mergeSmallestType(String inputPath) {
		File[] files = new File(inputPath).listFiles();
		if (files[0].getName().endsWith(".0es")) {
			IOHelper.log("merge all .0es");
			SystemHelper.runUnixCommand("cat " + files[0].getParent() + "/* > "
					+ inputPath + "/all.0es");
			for (File file : files) {
				if (!file.getName().equals("all.0es")) {
					file.delete();
				}
			}
		}
	}
}