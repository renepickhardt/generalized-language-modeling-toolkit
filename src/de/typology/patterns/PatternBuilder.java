package de.typology.patterns;

import java.util.ArrayList;

public class PatternBuilder {

	public static ArrayList<boolean[]> splitGLM(int maxModelLength) {
		ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
		for (int intPattern = 1; intPattern < Math.pow(2, maxModelLength); intPattern++) {
			// leave out even sequences since they don't contain a
			// target
			if (intPattern % 2 == 0) {
				continue;
			}
			patterns.add(PatternTransformer.getBooleanPattern(intPattern));
		}
		return patterns;
	}

	public static ArrayList<boolean[]> splitGLMForSmoothing(int maxModelLength) {
		ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
		for (int intPattern = 1; intPattern < Math.pow(2, maxModelLength); intPattern++) {
			// // leave out even sequences since they don't contain a
			// // target
			// if (intPattern % 2 == 0) {
			// continue;
			// }
			patterns.add(PatternTransformer.getBooleanPattern(intPattern));
		}
		return patterns;
	}

	public static ArrayList<boolean[]> splitLM(int maxModelLength) {
		ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
		for (int intPattern = 1; intPattern < Math.pow(2, maxModelLength); intPattern++) {
			String stringPattern = Integer.toBinaryString(intPattern);
			if (Integer.bitCount(intPattern) == stringPattern.length()) {
				patterns.add(PatternTransformer.getBooleanPattern(intPattern));
			}
		}
		return patterns;
	}

	public static ArrayList<boolean[]> splitTypology(int maxModelLength) {
		ArrayList<boolean[]> patterns = new ArrayList<boolean[]>();
		for (int intPattern = 1; intPattern < Math.pow(2, maxModelLength); intPattern++) {
			String stringPattern = Integer.toBinaryString(intPattern);
			if (Integer.bitCount(intPattern) <= 2
					&& stringPattern.startsWith("1")
					&& stringPattern.endsWith("1")) {
				patterns.add(PatternTransformer.getBooleanPattern(intPattern));
			}
		}
		return patterns;
	}
}
