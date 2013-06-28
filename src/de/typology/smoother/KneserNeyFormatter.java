package de.typology.smoother;

import java.text.DecimalFormat;

public class KneserNeyFormatter {
	static DecimalFormat decimalFormat = new DecimalFormat("###.######");

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public static String getRoundedResult(double input) {
		return KneserNeyFormatter.decimalFormat.format(input);
	}
}
