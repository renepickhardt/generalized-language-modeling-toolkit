package de.typology.statsOld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import de.typology.utilsOld.Config;
import de.typology.utilsOld.IOHelper;

public class LineCounter {
	private BufferedReader reader;
	private BufferedWriter writer;
	private static ArrayList<File> files;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		files = IOHelper.getDirectory(new File(Config.get().wordCountInput));
		for (File file : files) {
			LineCounter lC = new LineCounter(file.getAbsolutePath(),
					Config.get().lineCountStats);
			System.out.println(file.getAbsolutePath() + ": ");
			System.out.println("start counting");
			lC.writeResult(file.getAbsolutePath(), lC.countLines());
			System.out.println("done");
		}

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
