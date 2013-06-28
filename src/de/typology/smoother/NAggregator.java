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
		NAggregator naa = new NAggregator(outputDirectory, "absolute",
				"absolute_");
		NAggregator nac = new NAggregator(outputDirectory, "_absolute",
				"_absolute_");
		naa.aggregate(5);
		nac.aggregate(5);
	}

	private String directory;
	private BufferedReader reader;
	private File inputDirectory;
	private File outputDirectory;
	private BufferedWriter writer;

	public NAggregator(String directory, String inputDirectoryName,
			String outputDirectoryName) {
		this.directory = directory;
		this.inputDirectory = new File(directory + inputDirectoryName);
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
		for (File inputFile : this.inputDirectory.listFiles()) {
			this.aggregateFiles(this.inputDirectory,
					inputFile.getName().split("\\.")[0]);
		}

		// delete unaggregated continuation directory
		if (Config.get().deleteTempFiles) {
			// TODO: delete some input directories (absolute?)
		}

	}

	private void aggregateFiles(File inputDirectory, String sequenceBinary) {
		String sequenceBinaryMinusOne = sequenceBinary.substring(0,
				sequenceBinary.length() - 1) + "_";
		IOHelper.strongLog("calculate Ns for " + sequenceBinaryMinusOne);
		File currentInputDirectory = new File(inputDirectory.getAbsolutePath()
				+ "/" + sequenceBinary);
		File currentOutputDirectory = new File(this.outputDirectory + "/"
				+ sequenceBinaryMinusOne);
		currentOutputDirectory.mkdir();
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
			long[] ns = new long[4];
			try {

				while ((line = this.reader.readLine()) != null) {
					lineSplit = line.split("\t");
					if (lineSplit[1].equals("<s>")) {
						continue;
					}
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
							// skip <fs> counts
							if (!currentSequence.startsWith("<fs>")) {
								this.writer.write(currentSequence);
								for (int i = 0; i < ns.length - 1; i++) {
									this.writer.write(ns[i] + "\t");
								}
								this.writer.write(ns[ns.length - 1] + "\n");
							}
							ns = new long[4];
							currentSequence = tempSequence;
							this.putIntoNs(currentCount, ns);
						}
					}
				}
				if (currentSequence != null) {
					// skip <fs> counts
					if (!currentSequence.startsWith("<fs>")) {
						this.writer.write(currentSequence);
						for (int i = 0; i < ns.length - 1; i++) {
							this.writer.write(ns[i] + "\t");
						}
						this.writer.write(ns[ns.length - 1] + "\n");
					}
				}
				this.reader.close();
				this.writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void putIntoNs(int currentCount, long[] ns) {
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
