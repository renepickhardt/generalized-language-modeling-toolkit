package de.typology.splitter;

import java.io.File;

import de.typology.utils.Config;

public class GLMZeroBuilder {
	protected File directory;
	protected String currentGLMType;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		GLMZeroBuilder glmzb = new GLMZeroBuilder(Config.get().outputDirectory
				+ Config.get().inputDataSet);
		glmzb.build();
	}

	public GLMZeroBuilder(String directoryPath) {
		this.directory = new File(directoryPath);
	}

	public void build() {
		File[] gLMFolders = this.directory.listFiles();
		for (File gLMFolder : gLMFolders) {
			this.currentGLMType = gLMFolder.getName();
			File[] currentFiles = gLMFolder.listFiles();
			for (File currentFile : currentFiles) {
				this.initialize(currentFile);
			}
		}
	}

	public void initialize(File currentFile) {

		String outputFileName = "";
	}
}
