package de.typology.splitter;

import java.io.File;
import java.util.ArrayList;

import de.typology.utils.PatternTransformer;

public class GLMSplitter extends Splitter {

	public GLMSplitter(File inputFile, File indexFile, File outputDirectory,
			int maxCountDivider, String delimiter, boolean deleteTempFiles) {
		super(inputFile, indexFile, outputDirectory, maxCountDivider,
				delimiter, deleteTempFiles);
		// TODO Auto-generated constructor stub
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
