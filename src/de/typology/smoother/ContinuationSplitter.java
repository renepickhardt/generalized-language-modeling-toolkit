package de.typology.smoother;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

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
				"index.txt", "stats.txt", "training.txt");
		cs.brh = new HashMap<BufferedReader, String>();
		cs.bwh = new HashMap<BufferedWriter, String>();
		try {
			cs.split(5);
		} catch (Exception e) {
			for (Entry<BufferedReader, String> r : cs.brh.entrySet()) {
				System.out.println(r.getValue());
			}
			for (Entry<BufferedWriter, String> r : cs.bwh.entrySet()) {
				System.out.println(r.getValue());
			}
			System.out.println("brh size: " + cs.brh.size());
			System.out.println("bwh size: " + cs.bwh.size());
			int mb = 1024 * 1024;
			// Getting the runtime reference from system
			Runtime runtime = Runtime.getRuntime();

			IOHelper.log("##### Heap utilization statistics [MB] #####");

			// Print used memory
			IOHelper.log("Used Memory:\t"
					+ (runtime.totalMemory() - runtime.freeMemory()) / mb);

			// Print free memory
			IOHelper.log("Free Memory:\t" + runtime.freeMemory() / mb);

			// Print total available memory
			IOHelper.log("Total Memory:\t" + runtime.totalMemory() / mb);

			// Print Maximum available memory
			IOHelper.log("Max Memory:\t" + runtime.maxMemory() / mb);
		}
		System.out.println("reader");
		for (Entry<BufferedReader, String> r : cs.brh.entrySet()) {
			System.out.println(r.getValue());
		}
		System.out.println("writer");
		for (Entry<BufferedWriter, String> r : cs.bwh.entrySet()) {
			System.out.println(r.getValue());
		}

	}

	protected String extension;
	private File[] inputFiles;
	private int filePointer;

	private ContinuationSorter continuationSorter;

	public ContinuationSplitter(String directory, String indexName,
			String statsName, String inputName) {
		super(directory, indexName, statsName, inputName, "");
		try {
			FileUtils.deleteDirectory(new File(directory + "continuation"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.continuationSorter = new ContinuationSorter();
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void initialize(String extension) {
		String absoluteGLMPath = this.directory + "/absolute/" + extension;
		File absoluteGLMDirectory = new File(absoluteGLMPath);
		this.inputFiles = absoluteGLMDirectory.listFiles();

		this.filePointer = 0;
		this.reader = IOHelper.openReadFile(this.inputFiles[this.filePointer]
				.getAbsolutePath());
		this.brh.put(this.reader,
				this.inputFiles[this.filePointer].getAbsolutePath());

		File currentOutputDirectory = new File(this.outputDirectory
				.getAbsolutePath().replace("normalized", "continuation")
				+ "/"
				+ extension);

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
			if (!this.bwh.containsKey(this.writers.get(fileCount))) {
				this.bwh.put(this.writers.get(fileCount),
						currentOutputDirectory + "/" + fileCount + "."
								+ extension + "_split");
			}
		}
	}

	protected boolean getNextLine() {
		try {
			if ((this.line = this.reader.readLine()) == null) {
				this.filePointer++;

				if (this.brh.containsKey(this.reader)) {
					this.brh.remove(this.reader);
				}

				this.reader.close();
				if (this.filePointer < this.inputFiles.length) {

					this.reader = IOHelper
							.openReadFile(this.inputFiles[this.filePointer]
									.getAbsolutePath());

					if (!this.brh.containsKey(this.reader)) {
						this.brh.put(this.reader,
								this.inputFiles[this.filePointer]
										.getAbsolutePath());
					}

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
				.replace("normalized", "continuation"));
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
					if (!this.bwh.containsKey(writer)) {
						this.bwh.put(writer, "from ContSplitter: temp BWriter");
					}
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
