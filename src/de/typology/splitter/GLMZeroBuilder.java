package de.typology.splitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class GLMZeroBuilder {
	protected File directory;
	protected String currentGLMType;
	protected BufferedReader reader;
	protected BufferedWriter writer;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GLMZeroBuilder glmzb = new GLMZeroBuilder(Config.get().outputDirectory
				+ Config.get().inputDataSet + "glm-absolute/");
		glmzb.build();
	}

	public GLMZeroBuilder(String directoryPath) {
		this.directory = new File(directoryPath);
	}

	public void build() {
		this.deleteOldFiles();
		File[] gLMFolders = this.directory.listFiles();
		for (File gLMFolder : gLMFolders) {
			this.currentGLMType = gLMFolder.getName();
			System.out.println("reducing " + this.currentGLMType);
			File[] currentFiles = gLMFolder.listFiles();
			for (File currentFile : currentFiles) {
				this.initialize(currentFile);
				long count = 0;
				String line;
				String[] lineSplit;
				String cut = "";
				long currentCount;
				String currentCut = "";
				try {
					while ((line = this.reader.readLine()) != null) {
						lineSplit = line.split("\t");
						currentCount = Long
								.parseLong(lineSplit[lineSplit.length - 1]);
						if (Integer.bitCount(Integer.parseInt(
								this.currentGLMType, 2)) == 1) {
							// special case: 1,10,100...
							count += currentCount;
						} else {
							// non special case: 11,101,110...
							currentCut = "";
							for (int i = 1; i < lineSplit.length - 1; i++) {
								currentCut += lineSplit[1] + "\t";
							}

							if (cut.equals("")) {
								cut = currentCut;
								count += currentCount;
							} else {
								if (cut.equals(currentCut)) {
									count += currentCount;
								} else {
									this.writer.write(cut + count + "\n");
									cut = currentCut;
									count = currentCount;
								}
							}
						}
					}
					// write last currentCut
					this.writer.write(cut + count + "\n");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					this.reader.close();
					this.writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void deleteOldFiles() {
		IOHelper.strongLog("deleting old files");
		File[] files = this.directory.listFiles();
		for (File file : files) {
			if (file.getName().startsWith("0")) {
				try {
					FileUtils.deleteDirectory(file);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public void initialize(File currentFile) {
		String currentFileHead = currentFile.getName().split("\\.")[0];
		String outputType = this.currentGLMType.replaceFirst("1", "0");

		String outputFileName = currentFileHead + "." + outputType;
		File outputDirectory = new File(this.directory + "/" + outputType);
		outputDirectory.mkdir();
		this.reader = IOHelper.openReadFile(currentFile.getAbsolutePath(),
				Config.get().memoryLimitForReadingFiles);
		this.writer = IOHelper.openWriteFile(outputDirectory + "/"
				+ outputFileName, Config.get().memoryLimitForWritingFiles);
	}
}
