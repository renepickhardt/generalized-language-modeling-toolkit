package de.typology.trainers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

/**
 * reads file with the following format
 * <p>
 * ngram TAB count NEWLINE
 * <p>
 * readNGram returns new NGram(ngram, count)
 * 
 * @author Martin Koerner
 * 
 */
public class NGramReader extends Reader {
	BufferedReader br;

	public NGramReader(Reader in) {
		this.br = new BufferedReader(in);
	}

	@Override
	public void close() throws IOException {
		this.br.close();
	}

	@Override
	public int read(char[] cbuf, int off, int len) throws IOException {
		throw new UnsupportedOperationException();
	}

	/**
	 * reads a line from file
	 * 
	 * @return new NGram
	 */
	public NGram readNGram() {
		String line;
		try {
			line = this.br.readLine();
			if (line != null) {
				String[] splitLine = line.split("\\s");
				if (splitLine.length > 1) {
					return new NGram(Arrays.copyOfRange(splitLine, 0,
							splitLine.length - 1),
							Integer.parseInt(splitLine[splitLine.length - 1]));
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
