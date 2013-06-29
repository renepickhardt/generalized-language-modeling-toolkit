package de.typology.smoother;

public class _absolute_Aggregator extends Absolute_Aggregator {

	public _absolute_Aggregator(String directory, String inputDirectoryName,
			String outputDirectoryName) {
		super(directory, inputDirectoryName, outputDirectoryName);
	}

	@Override
	protected void putIntoNs(int currentCount, long[] ns) {
		ns[0] = ns[0] + currentCount;
		switch (currentCount) {
		case 1:
			ns[1] = ns[1] + currentCount;
			break;
		case 2:
			ns[2] = ns[2] + currentCount;
			break;
		default:
			ns[3] = ns[3] + currentCount;
			break;
		}
	}
}
