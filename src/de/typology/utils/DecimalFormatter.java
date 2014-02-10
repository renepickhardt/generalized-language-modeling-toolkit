package de.typology.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class DecimalFormatter {

	DecimalFormat decimalFormat;

	public DecimalFormatter(int decimalPlaces) {
		String format = "###.";
		for (int i = 0; i < decimalPlaces; i++) {
			format += "#";
		}
		// set decimalFormat to override LOCALE values
		this.decimalFormat = new DecimalFormat(format);
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
		this.decimalFormat.setDecimalFormatSymbols(symbols);
	}

	public String getRoundedResult(double input) {
		return this.decimalFormat.format(input);
	}
}
