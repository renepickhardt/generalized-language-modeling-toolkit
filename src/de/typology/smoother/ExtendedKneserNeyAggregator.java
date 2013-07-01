package de.typology.smoother;

public class ExtendedKneserNeyAggregator extends KneserNeyAggregator {
	private double d1;
	private double d2;
	private double d3plus;

	public ExtendedKneserNeyAggregator(String directory,
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
		// TODO Auto-generated method stub

	}

	private void calculateDs() {
		// TODO calculation
		this.d1 = 0.5;
		this.d2 = this.d1;
		this.d3plus = this.d2;
	}

	/**
	 * 
	 * @param maxSequenceLength
	 *            needs to be greater than 1
	 */
	private void calculate(int maxSequenceLength) {

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
}
