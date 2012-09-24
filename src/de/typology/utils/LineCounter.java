package de.typology.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class LineCounter {
	private BufferedReader reader;
	private BufferedWriter writer;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		LineCounter lC = new LineCounter(Config.get().lineCountInput,
				Config.get().lineCountOutput);
		System.out.println("start counting");
		lC.writeResult(Config.get().lineCountInput, lC.countLines());
		System.out.println("done");
	}

	public LineCounter(String input, String output) throws IOException {
		this.reader = new BufferedReader(new FileReader(input));
		this.writer = new BufferedWriter(new FileWriter(output, true));
	}

	public int countLines() throws IOException {
		int count = 0;
		while (this.reader.readLine() != null) {
			count++;
		}
		this.reader.close();
		return count;
	}

	public void writeResult(String info, int result) throws IOException {
		this.writer.write(info + ": " + result + "\n");
		this.writer.flush();
		this.writer.close();
	}

}
