package de.typology.smootherOld;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class KneserNeyFormatter {
	public KneserNeyFormatter() {
		// set decimalFormat to override LOCALE values
		this.decimalFormat = new DecimalFormat("###.######");
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
		this.decimalFormat.setDecimalFormatSymbols(symbols);

	}

	DecimalFormat decimalFormat;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public String getRoundedResult(double input) {
		return this.decimalFormat.format(input);
	}
}
