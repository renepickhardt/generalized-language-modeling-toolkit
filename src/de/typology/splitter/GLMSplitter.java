package de.typology.splitter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

/**
 * 
 * @author Martin Koerner
 * 
 */
public class GLMSplitter extends Splitter {
	protected String extension;

	public GLMSplitter(String directory) {
		super(directory);
		try {
			// TODO:This should be placed somewhere else (inside Splitter is
			// currently not possible)
			IOHelper.strongLog("deleting old normalized directory");
			FileUtils.deleteDirectory(this.outputDirectory);
			IOHelper.strongLog("deleting old absolute directory");
			File absoluteDirectory = new File(this.outputDirectory
					.getAbsolutePath().replace("normalized", "absolute"));
			FileUtils.deleteDirectory(absoluteDirectory);
			this.outputDirectory.mkdir();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputDirectory = Config.get().outputDirectory
				+ Config.get().inputDataSet;
		GLMSplitter ts = new GLMSplitter(outputDirectory);
		// System.out.println("typo:");
		// ts.splitTypo(5);
		// System.out.println("lm:");
		// ts.splitLM(5);
		System.out.println("glm:");
		ts.splitGLM(5);
	}

	public void splitTypo(int maxSequenceLength) {
		for (int sequenceDecimal = 1; sequenceDecimal < Math.pow(2,
				maxSequenceLength); sequenceDecimal++) {

			String sequenceBinary = Integer.toBinaryString(sequenceDecimal);
			if (Integer.bitCount(sequenceDecimal) <= 2
					&& sequenceBinary.startsWith("1")
					&& sequenceBinary.endsWith("1")) {
				this.split(sequenceDecimal);
			}
		}
	}

	public void splitLM(int maxSequenceLength) {
		for (int sequenceDecimal = 1; sequenceDecimal < Math.pow(2,
				maxSequenceLength); sequenceDecimal++) {

			String sequenceBinary = Integer.toBinaryString(sequenceDecimal);
			if (Integer.bitCount(sequenceDecimal) == sequenceBinary.length()) {
				this.split(sequenceDecimal);
			}
		}
	}

	public void splitGLM(int maxSequenceLength) {
		for (int sequenceDecimal = 1; sequenceDecimal < Math.pow(2,
				maxSequenceLength); sequenceDecimal++) {
			// leave out even sequences since they don't contain a
			// target
			if (sequenceDecimal % 2 == 0) {
				continue;
			}
			this.split(sequenceDecimal);
		}
	}

	public void splitGLMForKneserNey(int maxSequenceLength) {
		for (int sequenceDecimal = 1; sequenceDecimal < Math.pow(2,
				maxSequenceLength); sequenceDecimal++) {
			this.split(sequenceDecimal);
		}
	}

	public void split(int sequenceDecimal) {

		// convert sequence type into binary representation
		String sequenceBinary = Integer.toBinaryString(sequenceDecimal);

		// naming and initialization
		this.extension = sequenceBinary;
		IOHelper.log("splitting into " + this.extension);
		this.initialize(this.extension);

		// iterate over corpus
		while (this.getNextSequence(sequenceBinary.length())) {
			// get actual sequence length (e.g.: 11011=4)
			String[] sequenceCut = new String[Integer.bitCount(sequenceDecimal)];

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
			String lineToPrint = "";
			try {
				// write actual sequence
				for (String sequenceCutWord : sequenceCut) {
					lineToPrint += sequenceCutWord + "\t";
				}
				lineToPrint += this.sequenceCount + "\n";
				if (lineToPrint.startsWith("\t")) {
					IOHelper.log("too short at:" + this.linePointer
							+ " text:\"" + this.line + "\"");
				} else {
					writer.write(lineToPrint);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// close reader
		try {
			this.reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// reset writers
		this.reset();
		this.sortAndAggregate(this.outputDirectory.getAbsolutePath() + "/"
				+ this.extension);
	}

}
