package de.typology.smootherOld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.typology.utils.Config;
import de.typology.utilsOld.IOHelper;

public class Absolute_Aggregator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputDirectory = Config.get().outputDirectory
				+ Config.get().inputDataSet;
		Absolute_Aggregator naa = new Absolute_Aggregator(outputDirectory,
				"absolute", "absolute_");
		Absolute_Aggregator nac = new Absolute_Aggregator(outputDirectory,
				"_absolute", "_absolute_");
		naa.aggregate(5);
		nac.aggregate(5);
	}

	private String directory;
	private BufferedReader reader;
	private File inputDirectory;
	private File outputDirectory;
	protected BufferedWriter writer;

	public Absolute_Aggregator(String directory, String inputDirectoryName,
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
			if (inputFile.getName().endsWith("1")) {
				this.aggregateFiles(this.inputDirectory, inputFile.getName()
						.split("\\.")[0]);
			}
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
								this.writeSequence(currentSequence, ns);
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
						this.writeSequence(currentSequence, ns);
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

	protected void writeSequence(String currentSequence, long[] ns) {
		try {
			this.writer.write(currentSequence);
			for (int i = 0; i < ns.length - 1; i++) {
				this.writer.write(ns[i] + "\t");
			}
			this.writer.write(ns[ns.length - 1] + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void putIntoNs(int currentCount, long[] ns) {
		ns[0] = ns[0] + 1;
		switch (currentCount) {
		case 1:
			ns[1] = ns[1] + 1;
			break;
		case 2:
			ns[2] = ns[2] + 1;
			break;
		default:
			ns[3] = ns[3] + 1;
			break;
		}
	}
}
