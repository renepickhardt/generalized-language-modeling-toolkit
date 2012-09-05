package de.typology.googleNgrams;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import de.typology.utils.IOHelper;
import de.typology.utils.SystemHelper;

public class NgramMerger {

	/**
	 * reads: Tab separated file. Each line has the following format:
	 * <p>
	 * ngram TAB year TAB match_count TAB page_count TAB volume_count NEWLINE
	 * <p>
	 * see also: http://books.google.com/ngrams/datasets
	 * <p>
	 * <p>
	 * writes: Tab separated file. Each line has the following format:
	 * <p>
	 * ngram TAB totalized_match_count NEWLINE
	 * <p>
	 * only runs on a Unix system
	 * 
	 * @param input
	 *            path to a directory containing google ngram files
	 * @param output
	 *            file path where the output file is written
	 * @throws IOException
	 * 
	 * @author Martin Koerner
	 */
	public void merge(String input, String output) throws IOException {
		BufferedReader reader;
		BufferedWriter writer;
		String line;
		String[] lineSplit;
		String[] currentNgram;
		int count;

		ArrayList<File> fileList = IOHelper.getDirectory(new File(input));

		for (File file : fileList) {
			System.out.println("unzip " + file + " -d " + input);
			SystemHelper.runUnixCommand("unzip " + file + " -d " + input);
			System.out.println("merge file");
			reader = new BufferedReader(new FileReader(file.getAbsolutePath()
					.substring(0, file.getAbsolutePath().length() - 4)));
			writer = new BufferedWriter(new FileWriter(output,/* append */true));

			// read first line
			line = reader.readLine();
			if (line != null) {
				lineSplit = line.split("\\s");
				currentNgram = Arrays.copyOfRange(lineSplit, 0,
						lineSplit.length - 4);
				count = 1;
			} else {
				continue;
			}
			// read following lines
			while ((line = reader.readLine()) != null) {
				lineSplit = line.split("\\s");
				if (lineSplit.length > 4) {
					if (Arrays.equals(currentNgram, Arrays.copyOfRange(
							lineSplit, 0, lineSplit.length - 4))) {
						// same ngram
						count += Integer
								.parseInt(lineSplit[lineSplit.length - 2]);
					} else {
						// new ngram

						// write current ngram + count
						for (int i = 0; i < currentNgram.length - 1; i++) {
							writer.write(currentNgram[i] + " ");

						}
						writer.write(currentNgram[currentNgram.length - 1]);
						writer.write("\t");
						writer.write(String.valueOf(count));
						writer.write("\n");
						writer.flush();

						// set currentNgram and reset count
						currentNgram = Arrays.copyOfRange(lineSplit, 0,
								lineSplit.length - 4);
						count = 1;
					}
				}
			}
			System.out.println("rm "
					+ file.getAbsolutePath().substring(0,
							file.getAbsolutePath().length() - 4));

			SystemHelper.runUnixCommand("rm "
					+ file.getAbsolutePath().substring(0,
							file.getAbsolutePath().length() - 4));
			reader.close();
			writer.close();
		}
	}
}
