package de.typology.smoother;

public class _absolute_Aggregator extends Absolute_Aggregator {

	public _absolute_Aggregator(String directory, String inputDirectoryName,
			String outputDirectoryName) {
		super(directory, inputDirectoryName, outputDirectoryName);
	}

	@Override
	protected void setCurrentCount(int currentCount) {
		this.currentCount += currentCount;
	}

}
