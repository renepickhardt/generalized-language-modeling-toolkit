package de.typology.smoother;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;

import de.typology.splitter.Splitter;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class AbsoluteRevertSortSplitter extends Splitter {
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
		AbsoluteRevertSortSplitter ars = new AbsoluteRevertSortSplitter(
				outputDirectory, "absolute", "absolute-rev-sort", "index.txt",
				"stats.txt", "training.txt");
		ars.split(5);
	}

	protected String extension;
	private File[] inputFiles;
	private int filePointer;
	private String inputDirectory;

	public AbsoluteRevertSortSplitter(String directory,
			String inputDirectoryName, String outputDirectoryName,
			String indexName, String statsName, String inputName) {
		super(directory, indexName, statsName, inputName, "");
		this.inputDirectory = this.directory + inputDirectoryName;

		// TODO: remove this line when Splitter is fixed (normalized
		// outputDirectory naming)
		this.outputDirectory = new File(this.directory + outputDirectoryName);

		if (this.outputDirectory.exists()) {
			try {
				FileUtils.deleteDirectory(this.outputDirectory);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.outputDirectory.mkdir();

	}

	@Override
	protected void initialize(String extension) {
		String inputGLMPath = this.inputDirectory + "/" + extension;
		File inputGLMDirectory = new File(inputGLMPath);
		this.inputFiles = inputGLMDirectory.listFiles();

		this.filePointer = 0;
		this.reader = IOHelper.openReadFile(this.inputFiles[this.filePointer]
				.getAbsolutePath());

		File currentOutputDirectory = new File(
				this.outputDirectory.getAbsolutePath() + "/" + extension);

		// delete old files
		try {
			FileUtils.deleteDirectory(currentOutputDirectory);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		currentOutputDirectory.mkdirs();
		this.writers = new HashMap<Integer, BufferedWriter>();
		for (int fileCount = 0; fileCount < this.wordIndex.length; fileCount++) {
			this.writers.put(
					fileCount,
					IOHelper.openWriteFile(
							currentOutputDirectory + "/" + fileCount + "."
									+ extension + "-split",
							Config.get().memoryLimitForWritingFiles
									/ Config.get().maxCountDivider));
		}
	}

	@Override
	public void split(int maxSequenceLength) {
		for (int sequenceDecimal = 1; sequenceDecimal < Math.pow(2,
				maxSequenceLength); sequenceDecimal++) {

			// leave out even sequences since they don't contain a target
			if (sequenceDecimal % 2 == 0) {
				continue;
			}

			// convert sequence type into binary representation
			String sequenceBinary = Integer.toBinaryString(sequenceDecimal);

			// naming and initialization
			this.extension = sequenceBinary;
			IOHelper.log("splitting into " + this.extension);
			this.initializeWriters(this.extension);

			String currentInputDirectory = this.inputDirectory + "/"
					+ this.extension;
			// iterate over glm files
			for (File inputFile : new File(currentInputDirectory).listFiles()) {
				BufferedReader inputReader = IOHelper.openReadFile(
						inputFile.getAbsolutePath(),
						Config.get().memoryLimitForReadingFiles);
				String line;
				String[] lineSplit;
				try {
					try {
						while ((line = inputReader.readLine()) != null) {
							lineSplit = line.split("\t");
							// get writer matching the last word
							BufferedWriter writer = this
									.getWriter(lineSplit[lineSplit.length - 2]);
							// write sequence
							for (int i = 0; i < lineSplit.length - 1; i++) {
								writer.write(lineSplit[i] + "\t");
							}
							// writer count
							writer.write(lineSplit[lineSplit.length - 1] + "\n");
						}
					} finally {
						inputReader.close();
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			this.reset();

			this.sorter.sortRevertCountDirectory(
					this.outputDirectory.getAbsolutePath() + "/"
							+ this.extension, "-split", "");
			this.mergeSmallestType(this.outputDirectory.getAbsolutePath() + "/"
					+ this.extension);

		}
		// this.sorter.sortCountDirectory(this.directory+this.outputDirectoryName,
		// inputExtension, outputExtension)
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
