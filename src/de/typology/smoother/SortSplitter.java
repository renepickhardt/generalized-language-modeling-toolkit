package de.typology.smoother;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.typology.splitter.Splitter;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class SortSplitter extends Splitter {
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
		SortSplitter ars = new SortSplitter(outputDirectory, "absolute",
				"absolute-rev-sort", "index.txt", "stats.txt", "training.txt",
				true);
		ars.split(5);
	}

	protected String extension;
	// private int filePointer;
	// private File[] inputFiles;
	private File inputDirectory;
	private boolean reverse;

	public SortSplitter(String directory, String inputDirectoryName,
			String outputDirectoryName, String indexName, String statsName,
			String inputName, boolean reverse) {
		super(directory, indexName, statsName, inputName, "");
		this.reverse = reverse;

		this.inputDirectory = new File(this.directory + inputDirectoryName);
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
	public void split(int maxSequenceLength) {
		IOHelper.strongLog("revert sorting " + this.inputDirectory);
		for (File inputDirectory : this.inputDirectory.listFiles()) {

			// naming and initialization
			this.extension = inputDirectory.getName();
			IOHelper.log("splitting into " + this.extension);
			this.initializeWriters(this.extension);

			String currentInputDirectory = this.inputDirectory + "/"
					+ this.extension;
			int columnNumber = Integer.bitCount(Integer.parseInt(
					this.extension.replaceAll("_", "0"), 2));
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
							BufferedWriter writer;
							if (this.reverse) {
								// get writer matching the last word
								writer = this
										.getWriter(lineSplit[columnNumber > 0 ? columnNumber - 1
												: 0]);
							} else {
								// get writer matching the first word
								writer = this.getWriter(lineSplit[0]);
							}
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

			for (File file : this.outputDirectory.listFiles()) {
				if (this.reverse) {
					this.sorter.sortRevertCountDirectory(
							file.getAbsolutePath(), "-split", "");
				} else {
					this.sorter.sortCountDirectory(file.getAbsolutePath(),
							"-split", "");
				}
			}
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
