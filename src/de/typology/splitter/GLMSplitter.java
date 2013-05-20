package de.typology.splitter;

import java.io.BufferedWriter;
import java.io.IOException;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

/**
 * 
 * @author Martin Koerner
 * 
 */
public class GLMSplitter extends Splitter {
	protected String extension;

	public GLMSplitter(String directory, String indexName, String statsName,
			String inputName) {
		super(directory, indexName, statsName, inputName, "glm");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputDirectory = Config.get().outputDirectory
				+ Config.get().inputDataSet;
		GLMSplitter ts = new GLMSplitter(outputDirectory, "index.txt",
				"stats.txt", "training.txt");
		ts.split(5);
	}

	@Override
	public void split(int maxSequenceLength) {
		for (int sequenceDecimal = 1; sequenceDecimal < Math.pow(2,
				maxSequenceLength); sequenceDecimal++) {

			// optional: leave out even sequences since they don't contain a
			// target
			// if (sequenceDecimal % 2 == 0) {
			// continue;
			// }

			// convert sequence type into binary representation
			String sequenceBinary = Integer.toBinaryString(sequenceDecimal);

			// naming and initialization
			this.extension = sequenceBinary;
			IOHelper.log("splitting into " + this.extension);
			this.initialize(this.extension);

			// iterate over corpus
			while (this.getNextSequence(sequenceBinary.length())) {
				// get actual sequence length (e.g.: 11011=4)
				String[] sequenceCut = new String[Integer
						.bitCount(sequenceDecimal)];

				// convert binary sequence type into char[] for iteration
				char[] sequenceChars = sequenceBinary.toCharArray();

				// sequencePointer points at sequenceCut
				int sequencePointer = 0;
				for (int i = 0; i < sequenceChars.length; i++) {
					if (Character.getNumericValue(sequenceChars[i]) == 1) {
						sequenceCut[sequencePointer] = this.sequence[i];
						sequencePointer++;
					}
				}
				// get accurate writer
				BufferedWriter writer = this.getWriter(sequenceCut[0]);
				try {
					// write actual sequence
					for (String sequenceCutWord : sequenceCut) {
						writer.write(sequenceCutWord + "\t");
					}
					writer.write(this.sequenceCount + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			// reset writers
			this.reset();
			this.sortAndAggregate(this.outputDirectory.getAbsolutePath() + "/"
					+ this.extension);
		}
	}

	@Override
	protected void mergeSmallestType(String inputPath) {
		// File inputFile = new File(inputPath);
		// if (Integer.bitCount(Integer.parseInt(inputFile.getName(), 2)) == 1)
		// {
		// File[] files = inputFile.listFiles();
		//
		// String fileExtension = inputFile.getName();
		// IOHelper.log("merge all " + fileExtension);
		// SystemHelper.runUnixCommand("cat " + inputPath + "/* > "
		// + inputPath + "/all." + fileExtension);
		// for (File file : files) {
		// if (!file.getName().equals("all." + fileExtension)) {
		// file.delete();
		// }
		// }
		// }
	}
}
