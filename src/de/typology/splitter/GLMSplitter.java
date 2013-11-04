package de.typology.splitter;

import java.io.File;
import java.util.ArrayList;

import de.typology.utils.Config;
import de.typology.utils.PatternTransformer;

public class GLMSplitter extends Splitter {

	public GLMSplitter(File inputFile, File outputDirectory,
			int maxCountDivider, char delimiter) {
		super(inputFile, outputDirectory, maxCountDivider, delimiter);
	}

	public static void main(String[] args) {
		// TODO: parameters as arguments
		File outputDirectory = new File(Config.get().outputDirectory
				+ Config.get().inputDataSet);
		File inputFile = new File(outputDirectory.getAbsolutePath()
				+ "/training.txt");
		GLMSplitter glmSplitter = new GLMSplitter(inputFile, outputDirectory,
				1000, '\t');
		// System.out.println("GLM");
		// glmSplitter.splitGLM(Config.get().modelLength);
		System.out.println("GLMForSmoothing");
		glmSplitter.splitGLMForSmoothing(Config.get().modelLength);
		// System.out.println("LM");
		// glmSplitter.splitLM(Config.get().modelLength);
		// System.out.println("Typology");
		// glmSplitter.splitTypology(Config.get().modelLength);

	}

	public void splitGLM(int maxModelLength) {
		ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
		for (int intPattern = 1; intPattern < Math.pow(2, maxModelLength); intPattern++) {
			// leave out even sequences since they don't contain a
			// target
			if (intPattern % 2 == 0) {
				continue;
			}
			patterns.add(PatternTransformer.getBooleanPattern(intPattern));
		}
		this.split(patterns);
	}

	public void splitGLMForSmoothing(int maxModelLength) {
		ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
		for (int intPattern = 1; intPattern < Math.pow(2, maxModelLength); intPattern++) {
			// // leave out even sequences since they don't contain a
			// // target
			// if (intPattern % 2 == 0) {
			// continue;
			// }
			patterns.add(PatternTransformer.getBooleanPattern(intPattern));
		}
		this.split(patterns);
	}

	public void splitLM(int maxModelLength) {
		ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
		for (int intPattern = 1; intPattern < Math.pow(2, maxModelLength); intPattern++) {
			String stringPattern = Integer.toBinaryString(intPattern);
			if (Integer.bitCount(intPattern) == stringPattern.length()) {
				patterns.add(PatternTransformer.getBooleanPattern(intPattern));
			}
		}
		this.split(patterns);
	}

	public void splitTypology(int maxModelLength) {
		ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
		for (int intPattern = 1; intPattern < Math.pow(2, maxModelLength); intPattern++) {
			String stringPattern = Integer.toBinaryString(intPattern);
			if (Integer.bitCount(intPattern) <= 2
					&& stringPattern.startsWith("1")
					&& stringPattern.endsWith("1")) {
				patterns.add(PatternTransformer.getBooleanPattern(intPattern));
			}
		}
		this.split(patterns);
	}
}
