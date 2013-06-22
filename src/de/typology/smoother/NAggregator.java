package de.typology.smoother;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class NAggregator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputDirectory = Config.get().outputDirectory
				+ Config.get().inputDataSet;
		NAggregator na = new NAggregator(outputDirectory, "absolute",
				"continuation", "ns");
		na.aggregate(5);
	}

	private String directory;
	private BufferedReader reader;
	private File absoluteInputDirectory;
	private File continuationInputDirectory;
	private File outputDirectory;
	private BufferedWriter writer;

	public NAggregator(String directory, String absoluteInputDirectoryName,
			String continuationInputDirectoryName, String outputDirectoryName) {
		this.directory = directory;
		this.absoluteInputDirectory = new File(directory
				+ absoluteInputDirectoryName);
		this.continuationInputDirectory = new File(directory
				+ continuationInputDirectoryName);
		this.outputDirectory = new File(directory + outputDirectoryName);
		// delete old output directory
		try {
			FileUtils.deleteDirectory(this.outputDirectory);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.outputDirectory.mkdir();
	}

	public void aggregate(int maxSequenceLength) {
		IOHelper.strongLog("calculate N values of " + this.directory + " into "
				+ this.outputDirectory);
		IOHelper.strongLog("DELETE TEMP FILES IS: "
				+ Config.get().deleteTempFiles);
		// leave out unigrams
		for (int sequenceDecimal = 2; sequenceDecimal < Math.pow(2,
				maxSequenceLength); sequenceDecimal++) {
			// optional: leave out even sequences since they don't contain a
			// target
			if (sequenceDecimal % 2 == 0) {
				continue;
			}
			String sequenceBinary = Integer.toBinaryString(sequenceDecimal);
			this.aggregateFiles(this.absoluteInputDirectory, sequenceBinary);
			this.aggregateFiles(this.continuationInputDirectory,
					sequenceBinary.replaceFirst("1", "_"));
			// merge and sort current directory
		}

		// delete unaggregated continuation directory
		if (Config.get().deleteTempFiles) {
			// TODO: delete some input directories (absolute?)
		}

	}

	private void aggregateFiles(File inputDirectory, String sequenceBinary) {
		String sequenceBinaryMinusOne = sequenceBinary.substring(0,
				sequenceBinary.length() - 1) + "_";
		System.out.println(sequenceBinary + " " + sequenceBinaryMinusOne);
		IOHelper.strongLog("calculate N for " + sequenceBinaryMinusOne);
		File currentInputDirectory = new File(inputDirectory.getAbsolutePath()
				+ "/" + sequenceBinary);
		File currentOutputDirectory = new File(this.outputDirectory + "/"
				+ sequenceBinaryMinusOne);
		currentOutputDirectory.mkdir();
		System.out.println(currentInputDirectory);
		for (File currentFile : currentInputDirectory.listFiles()) {
			this.reader = IOHelper.openReadFile(currentFile.getAbsolutePath(),
					Config.get().memoryLimitForReadingFiles);
			this.writer = IOHelper.openWriteFile(currentOutputDirectory + "/"
					+ currentFile.getName().split("\\.")[0] + "."
					+ sequenceBinaryMinusOne,
					Config.get().memoryLimitForWritingFiles);
			String line;
			String lineSplit[] = null;
			String currentSequence = null;
			int currentCount = 0;

			// format of ns: 0=N1+,1=N1,2=N2,3=N3+
			int[] ns = new int[4];
			try {

				while ((line = this.reader.readLine()) != null) {
					lineSplit = line.split("\t");
					String tempSequence = "";
					for (int i = 0; i < lineSplit.length - 2; i++) {
						tempSequence += lineSplit[i] + "\t";
					}
					currentCount = Integer
							.parseInt(lineSplit[lineSplit.length - 1]);
					if (currentSequence == null) {
						// initialize
						currentSequence = tempSequence;
						this.putIntoNs(currentCount, ns);

					} else {
						if (tempSequence.equals(currentSequence)) {
							this.putIntoNs(currentCount, ns);
						} else {
							this.writer.write(currentSequence);
							for (int i = 0; i < ns.length - 1; i++) {
								this.writer.write(ns[i] + "\t");
							}
							this.writer.write(ns[ns.length - 1] + "\n");
							ns = new int[4];
							currentSequence = tempSequence;
							this.putIntoNs(currentCount, ns);
						}
					}
				}
				if (currentSequence != null) {
					this.writer.write(currentSequence);
					for (int i = 0; i < ns.length - 1; i++) {
						this.writer.write(ns[i] + "\t");
					}
					this.writer.write(ns[ns.length - 1] + "\n");
				}
				this.reader.close();
				this.writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void putIntoNs(int currentCount, int[] ns) {
		ns[0] = ns[0] + currentCount;
		switch (currentCount) {
		case 1:
			ns[1] = ns[1] + currentCount;
			break;
		case 2:
			ns[2] = ns[2] + currentCount;
			break;
		default:
			ns[3] = ns[3] + currentCount;
			break;
		}
	}
}
