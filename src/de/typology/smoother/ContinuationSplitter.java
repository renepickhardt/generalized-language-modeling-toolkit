package de.typology.smoother;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;

import de.typology.splitter.Splitter;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class ContinuationSplitter extends Splitter {
	/**
	 * This class provides a method for splitting and sorting ngrams by the
	 * second, third, fourth...(, first) word in order to calculate the novel
	 * continuation probability used in Kneser-Ney interpolation.
	 * 
	 * The index for file splitting is applied to the second word (instead of
	 * the first word as in the "normal" splitter).
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		String outputDirectory = Config.get().outputDirectory
				+ Config.get().inputDataSet;
		ContinuationSplitter cs = new ContinuationSplitter(outputDirectory,
				"absolute", "continuation", "index.txt", "stats.txt",
				"training.txt", false);
		cs.split(5);
	}

	protected String extension;
	private File[] inputFiles;
	private int filePointer;
	private String inputDirectoryName;
	private String outputDirectoryName;
	private boolean deleteInputFiles;

	private ContinuationSorter continuationSorter;

	public ContinuationSplitter(String directory, String inputDirectoryName,
			String outputDirectoryName, String indexName, String statsName,
			String inputName, boolean deleteInputFiles) {
		super(directory, indexName, statsName, inputName, "");
		this.inputDirectoryName = inputDirectoryName;
		this.outputDirectoryName = outputDirectoryName;
		this.deleteInputFiles = deleteInputFiles;
		try {
			FileUtils
					.deleteDirectory(new File(directory + outputDirectoryName));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.continuationSorter = new ContinuationSorter();
	}

	@Override
	protected void initialize(String extension) {
		String inputGLMPath = this.directory + this.inputDirectoryName + "/"
				+ extension;
		File inputGLMDirectory = new File(inputGLMPath);
		this.inputFiles = inputGLMDirectory.listFiles();

		this.filePointer = 0;
		this.reader = IOHelper.openReadFile(this.inputFiles[this.filePointer]
				.getAbsolutePath());

		File currentOutputDirectory = new File(this.outputDirectory
				.getAbsolutePath().replace("normalized",
						this.outputDirectoryName)
				+ "/" + extension);

		// delete old files
		try {
			FileUtils.deleteDirectory(currentOutputDirectory);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		currentOutputDirectory.mkdirs();
		currentOutputDirectory.mkdir();
		this.writers = new HashMap<Integer, BufferedWriter>();
		for (int fileCount = 0; fileCount < this.wordIndex.length; fileCount++) {
			this.writers.put(
					fileCount,
					IOHelper.openWriteFile(
							currentOutputDirectory + "/" + fileCount + "."
									+ extension + "_split",
							Config.get().memoryLimitForWritingFiles
									/ Config.get().maxCountDivider));
		}
	}

	protected boolean getNextLine() {
		try {
			if ((this.line = this.reader.readLine()) == null) {
				this.filePointer++;

				this.reader.close();
				if (this.deleteInputFiles) {
					this.inputFiles[this.filePointer - 1].delete();
				}
				if (this.filePointer < this.inputFiles.length) {

					this.reader = IOHelper
							.openReadFile(this.inputFiles[this.filePointer]
									.getAbsolutePath());

					return this.getNextLine();
				} else {
					return false;
				}
			} else {
				this.lineSplit = this.line.split("\t");
				return true;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void split(int maxSequenceLength) {
		this.outputDirectory = new File(this.outputDirectory.getAbsolutePath()
				.replace("normalized", this.outputDirectoryName));
		// leave out unigrams since they get calculated from bigrams
		// leave out 10 since continuation(0)=|distinct words|
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

			// iterate over glm files
			while (this.getNextLine()) {
				BufferedWriter writer;
				if (Integer.bitCount(sequenceDecimal) == 1) {
					writer = this.getWriter(this.lineSplit[0]);
				} else {
					// get writer fitting the second(!) word
					writer = this.getWriter(this.lineSplit[1]);
				}
				try {
					// write actual sequence
					writer.write(this.line + "\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			this.reset();

			this.continuationSorter.sortSecondCloumnDirectory(
					this.outputDirectory.getAbsolutePath() + "/"
							+ this.extension, "_split", "");
			this.mergeSmallestType(this.outputDirectory.getAbsolutePath() + "/"
					+ this.extension);

		}
		if (this.deleteInputFiles) {
			try {
				FileUtils.deleteDirectory(new File(this.directory + "/"
						+ this.inputDirectoryName));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
