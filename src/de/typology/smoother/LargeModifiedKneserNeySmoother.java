package de.typology.smoother;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.typology.utils.Counter;

public class LargeModifiedKneserNeySmoother {

	private double d1;
	private double d2;
	private double d3plus;

	Logger logger = LogManager.getLogger(this.getClass().getName());

	/**
	 * @param args
	 */

	// @Override
	protected void calculateDs(File directory) {
		long n1 = Counter.countCountsInDirectory(1, directory);
		long n2 = Counter.countCountsInDirectory(2, directory);
		long n3 = Counter.countCountsInDirectory(3, directory);
		long n4 = Counter.countCountsInDirectory(4, directory);
		this.logger.info("n1: " + n1);
		this.logger.info("n2: " + n2);
		this.logger.info("n3: " + n3);
		this.logger.info("n4: " + n4);
		double y = n1 / ((double) n1 + 2 * n2);
		this.d1 = 1 - 2 * y * ((double) n2 / (double) n1);
		this.d2 = 2 - 3 * y * ((double) n3 / (double) n2);
		this.d3plus = 3 - 4 * y * ((double) n4 / (double) n3);
		this.logger.info("D1: " + this.d1);
		this.logger.info("D2: " + this.d2);
		this.logger.info("D3+: " + this.d3plus);
	}

	// @Override
	protected double getD(int _absoluteCount) {
		if (_absoluteCount == 1) {
			return this.d1;
		}
		if (_absoluteCount == 2) {
			return this.d2;
		}
		if (_absoluteCount >= 3) {
			return this.d3plus;
		}
		// if _absoluteCount==0
		return 0;
	}

	// @Override
	// protected double getDNumerator(int _absoluteCount,
	// String _absoluteWordsWithoutLast) {
	// double one = this.getD(1)
	// * this.getAbsolute_Count(_absoluteWordsWithoutLast, 1);
	// double two = this.getD(2)
	// * this.getAbsolute_Count(_absoluteWordsWithoutLast, 2);
	// double threePlus = this.getD(3)
	// * this.getAbsolute_Count(_absoluteWordsWithoutLast, 3);
	// return one + two + threePlus;
	// }
}
