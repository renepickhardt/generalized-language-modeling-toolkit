package de.typology.splitterOld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.typology.utilsOld.Config;
import de.typology.utilsOld.IOHelper;
import de.typology.utilsOld.SystemHelper;

@Deprecated
public class GLMZeroBuilder {
	protected File directory;
	protected File outputDirectory;
	protected String outputType;
	protected String currentGLMType;
	protected File currentOutputDirectory;
	protected BufferedReader reader;
	protected BufferedWriter writer;

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		GLMZeroBuilder glmzb = new GLMZeroBuilder(Config.get().outputDirectory
				+ Config.get().inputDataSet + "continuation/", "absolute");
		glmzb.build();
	}

	public GLMZeroBuilder(String directoryPath, String outputDirectoryName) {
		this.directory = new File(directoryPath);
		this.outputDirectory = new File(this.directory.getParent() + "/"
				+ outputDirectoryName);
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
								currentCut += lineSplit[i] + "\t";
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
			this.mergeSmallestType(this.currentOutputDirectory
					.getAbsolutePath());
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
		this.outputType = this.currentGLMType.replaceFirst("1", "0");

		String outputFileName = currentFileHead + "." + this.outputType;
		this.currentOutputDirectory = new File(this.outputDirectory + "/"
				+ this.outputType);
		this.currentOutputDirectory.mkdir();
		this.reader = IOHelper.openReadFile(currentFile.getAbsolutePath(),
				Config.get().memoryLimitForReadingFiles);
		this.writer = IOHelper.openWriteFile(this.currentOutputDirectory + "/"
				+ outputFileName, Config.get().memoryLimitForWritingFiles);
	}

	protected void mergeSmallestType(String inputPath) {
		File inputFile = new File(inputPath);
		if (Integer.bitCount(Integer.parseInt(inputFile.getName(), 2)) == 0) {
			File[] files = inputFile.listFiles();

			String fileExtension = inputFile.getName();
			IOHelper.log("merge all " + fileExtension);

			SystemHelper.runUnixCommand("cat " + inputPath
					+ "/*| awk '{a+=$1;}END{print a}' > " + inputPath + "/all."
					+ fileExtension);
			for (File file : files) {
				if (!file.getName().equals("all." + fileExtension)) {
					file.delete();
				}
			}
		}
	}
}
