package de.typology.predictors;

import de.typology.splitter.BinarySearch;
import de.typology.splitter.IndexBuilder;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class GLMMySQLSearcher extends NewMySQLSearcher {
	int k;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IndexBuilder ib = new IndexBuilder();
		String databaseName = Config.get().trainedOnDataSet + "_"
				+ Config.get().trainedOnLang;
		String indexPath = Config.get().outputDirectory + "/"
				+ Config.get().trainedOnDataSet + "/"
				+ Config.get().trainedOnLang + "/index.txt";
		String[] wordIndex = ib.deserializeIndex(indexPath);

		// k is used in prepareQuery
		int k = 3;

		GLMMySQLSearcher glmmss = new GLMMySQLSearcher(databaseName, k);
		// Config.get().weight = "no";
		// for (int i = 5; i > 1; i--) {
		// IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
		// + i);
		// tmss.run(i, 100000, Config.get().weight);
		// }
		// Config.get().weight = "pic";

		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("model parameter: " + i);
			glmmss.run(i, Config.get().numberOfQueries, Config.get().weight,
					wordIndex);
		}
	}

	public GLMMySQLSearcher(String databaseName, int k) {
		// general:
		super(databaseName);
		this.k = k;
	}

	@Override
	protected String prepareQuery(String[] words, int sequence, int pfl,
			String[] wordIndex) {
		int l = words.length;
		String target = words[l - 1];
		String source;
		int leadingZeros = 0;
		if (sequence == 0) {
			source = "true";
		} else {
			if (Integer.bitCount(sequence) == this.k
					|| Integer.bitCount(sequence) == Integer.toBinaryString(
							sequence).length()
					&& Integer.bitCount(sequence) <= this.k) {
				source = "";
				String sequenceBinary = Integer.toBinaryString(sequence);
				while (sequenceBinary.length() < this.n - 1) {
					sequenceBinary = "0" + sequenceBinary;
					leadingZeros++;
				}

				// convert binary sequence type into char[] for iteration
				char[] sequenceChars = sequenceBinary.toCharArray();

				// sequencePointer points at sequenceCut
				for (int i = 0; i < sequenceChars.length; i++) {
					if (Character.getNumericValue(sequenceChars[i]) == 1) {
						if (source.length() == 0) {
							source += "source" + (i - leadingZeros) + " =\""
									+ words[i] + "\"";
						} else {
							source += " and source" + (i - leadingZeros)
									+ " =\"" + words[i] + "\"";
						}
					}
				}
			} else {
				// dummy sql query?
				source = "false";
			}
		}
		if (pfl > target.length()) {
			System.out.println("target: '" + target
					+ "' to short for prefixlength: " + pfl);
			return null;
		}
		String prefix = target.substring(0, pfl) + "%";
		if (target.equals("-%")) {
			System.out.println("deteced hyphen");
			return null;
		}
		// + "1" since sequence=(n-1)^2 to leave out target
		String tablePrefix = Integer.toBinaryString(sequence) + "1";
		String tableName;
		if (sequence == 0) {
			tableName = "1_all";
		} else {
			tableName = tablePrefix + "_"
					+ BinarySearch.rank(words[leadingZeros], wordIndex);

		}
		String query = "";
		if (pfl > 0) {
			query = "select * from " + tableName + " where " + source
					+ " and target like \"" + prefix
					+ "\" order by score desc limit " + this.joinLength;
		} else {
			query = "select * from " + tableName + " where " + source
					+ " order by score desc limit " + this.joinLength;
		}
		return query;
	}
}
