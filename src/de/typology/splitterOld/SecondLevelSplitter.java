package de.typology.splitterOld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import de.typology.utils.Config;
import de.typology.utilsOld.IOHelper;
import de.typology.utilsOld.SystemHelper;

public class SecondLevelSplitter {
	private String[] wordIndex;

	protected BufferedReader reader;
	private HashMap<Integer, BufferedWriter> writers;

	private String line;
	private String[] lineSplit = new String[0];

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public void secondLevelSplitDirectory(String indexPath, String inputPath,
			String inputExtension, String outputExtension) {
		File[] files = new File(inputPath).listFiles();
		for (File file : files) {
			this.secondLevelSplitFile(indexPath, file.getAbsolutePath(),
					inputExtension, outputExtension);
		}
	}

	public void secondLevelSplitFile(String indexPath, String absolutePath,
			String inputExtension, String outputExtension) {
		// initialize wordIndex
		if (this.wordIndex == null) {
			IndexBuilder ib = new IndexBuilder();
			this.wordIndex = ib.deserializeIndex(indexPath);
		}

		File file = new File(absolutePath);
		if (file.isFile() && file.length() > Config.get().fileSizeThreashhold) {
			try {
				this.reader = IOHelper.openReadFile(absolutePath);

				// check if less than three columns
				this.line = this.reader.readLine();
				if (this.line == null) {
					this.reader.close();
					return;
				}
				this.lineSplit = this.line.split("\t");
				if (this.lineSplit.length < 3) {
					// return if less than three columns
					this.reader.close();
					return;
				}

				IOHelper.log("second level splitting of " + file.getName());

				this.initializeWriters(indexPath, absolutePath, inputExtension,
						outputExtension);

				do {
					this.lineSplit = this.line.split("\t");
					this.writers.get(
							BinarySearch
									.rank(this.lineSplit[1], this.wordIndex))
							.write(this.line + "\n");
				} while ((this.line = this.reader.readLine()) != null);
				this.reader.close();
				this.closeWriters();

				// delete old file
				file.delete();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Initializing the reader and writers
	 * 
	 * @param extension
	 * @param sequenceLength
	 */
	protected void initializeWriters(String indexPath, String inputPath,
			String inputExtension, String outputExtension) {
		File file = new File(inputPath);
		String fileName = file.getName().replace(inputExtension,
				outputExtension);
		String fileNameSplit[] = fileName.split("\\.");
		String fileNumber = fileNameSplit[0];
		String fileExtension = fileNameSplit[1];
		this.writers = new HashMap<Integer, BufferedWriter>();
		for (int fileCount = 0; fileCount < this.wordIndex.length; fileCount++) {
			// s stands for sub file
			this.writers.put(
					fileCount,
					IOHelper.openWriteFile(
							file.getParent() + "/" + fileNumber + "-"
									+ fileCount + "." + fileExtension,
							Config.get().memoryLimitForWritingFiles
									/ Config.get().maxCountDivider));
		}
	}

	protected void closeWriters() {
		try {
			for (Entry<Integer, BufferedWriter> writer : this.writers
					.entrySet()) {
				writer.getValue().close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void mergeDirectory(String inputPath) {
		File[] files = new File(inputPath).listFiles();
		HashSet<String> filesToMerge = new HashSet<String>();
		String fileNameCut;
		String fileExtension = "." + files[0].getName().split("\\.")[1];

		for (File file : files) {
			if (file.getName().contains("-")) {
				fileNameCut = file.getName().split("-")[0];
				if (!filesToMerge.contains(fileNameCut)) {
					filesToMerge.add(fileNameCut);
				}
			}
		}

		Arrays.sort(this.wordIndex);
		for (String fileToMerge : filesToMerge) {
			IOHelper.log("merge second level split of " + fileToMerge
					+ fileExtension);
			for (int secondLevelType = 0; secondLevelType < this.wordIndex.length; secondLevelType++) {
				for (File file : files) {
					if (file.getName()
							.contains(
									fileToMerge + "-" + secondLevelType
											+ fileExtension)) {
						// IOHelper.log("merge " + fileToMerge + "-"
						// + secondLevelType + fileExtension + " into "
						// + fileToMerge + fileExtension);
						SystemHelper.runUnixCommand("cat " + inputPath + "/"
								+ fileToMerge + "-" + secondLevelType
								+ fileExtension + " >> " + inputPath + "/"
								+ fileToMerge + fileExtension);

					}
				}
			}
		}
		for (File file : files) {
			if (file.getName().contains("-")) {
				file.delete();
			}
		}
	}
}
