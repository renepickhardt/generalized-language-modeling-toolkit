package de.typology.smoother;

import java.io.IOException;

public class _absolute_Aggregator extends Absolute_Aggregator {

	public _absolute_Aggregator(String directory, String inputDirectoryName,
			String outputDirectoryName) {
		super(directory, inputDirectoryName, outputDirectoryName);
	}

	@Override
	protected void writeSequence(String currentSequence, long[] ns) {
		try {
			this.writer.write(currentSequence + ns[0] + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void putIntoNs(int currentCount, long[] ns) {
		ns[0] = ns[0] + currentCount;
	}

}
