package de.typology.stats;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class LineCounter {
	private BufferedReader reader;
	private BufferedWriter writer;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		LineCounter lC = new LineCounter(Config.get().lineCountInput,
				Config.get().lineCountStats);
		System.out.println("start counting");
		lC.writeResult(Config.get().lineCountInput, lC.countLines());
		System.out.println("done");
	}

	public LineCounter(String input, String output) throws IOException {
		this.reader = IOHelper.openReadFile(input);
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
		this.writer.write(info + ":" + "\n");

		this.writer.write("\t" + "lines: " + result + "\n");
		Date date = new Date();
		this.writer.write("\t" + "date: " + date + "\n");
		this.writer.flush();
		this.writer.close();
	}

}
