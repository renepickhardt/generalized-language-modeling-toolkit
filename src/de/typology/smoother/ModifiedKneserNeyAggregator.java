package de.typology.smoother;

import de.typology.utils.Config;
import de.typology.utils.Counter;

public class ModifiedKneserNeyAggregator extends KneserNeyAggregator {
	private double d1;
	private double d2;
	private double d3plus;

	public ModifiedKneserNeyAggregator(String directory,
			String absoluteDirectoryName, String _absoluteDirectoryName,
			String absolute_DirectoryName, String _absolute_DirectoryName,
			String outputDirectoryName, String indexName, String statsName) {
		super(directory, absoluteDirectoryName, _absoluteDirectoryName,
				absolute_DirectoryName, _absolute_DirectoryName,
				outputDirectoryName, indexName, statsName);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputDirectory = Config.get().outputDirectory
				+ Config.get().inputDataSet;
		ModifiedKneserNeyAggregator mkna = new ModifiedKneserNeyAggregator(
				outputDirectory, "absolute", "_absolute", "absolute_",
				"_absolute_", "mod-kneser-ney", "index.txt", "stats.txt");
		mkna.calculate(5);

	}

	@Override
	protected void calculateDs(String directoryPath) {
		long n1 = Counter.countCountsInDirectory(1, directoryPath);
		long n2 = Counter.countCountsInDirectory(2, directoryPath);
		long n3 = Counter.countCountsInDirectory(3, directoryPath);
		long n4 = Counter.countCountsInDirectory(4, directoryPath);
		System.out.println("n1: " + n1);
		System.out.println("n2: " + n2);
		System.out.println("n3: " + n3);
		System.out.println("n4: " + n4);
		double y = n1 / ((double) n1 + 2 * n2);
		this.d1 = 1 - 2 * y * ((double) n2 / (double) n1);
		this.d2 = 2 - 3 * y * ((double) n3 / (double) n2);
		this.d3plus = 3 - 4 * y * ((double) n4 / (double) n3);
		System.out.println("D1: " + this.d1);
		System.out.println("D2: " + this.d2);
		System.out.println("D3+: " + this.d3plus);
	}

	@Override
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

	@Override
	protected double getDNumerator(int _absoluteCount,
			String _absoluteWordsWithoutLast) {
		double one = this.getD(1)
				* this.getAbsolute_Count(_absoluteWordsWithoutLast, 1);
		double two = this.getD(2)
				* this.getAbsolute_Count(_absoluteWordsWithoutLast, 2);
		double threePlus = this.getD(3)
				* this.getAbsolute_Count(_absoluteWordsWithoutLast, 3);
		return one + two + threePlus;
	}
}
