package de.typology.predictors;

import de.typology.splitter.BinarySearch;
import de.typology.splitter.IndexBuilder;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class KneserNeyMySQLSearcher extends NewMySQLSearcher {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IndexBuilder ib = new IndexBuilder();
		String databaseName = Config.get().trainedOnDataSet + "_"
				+ Config.get().trainedOnLang + "_" + Config.get().dataBaseType;
		String indexPath = Config.get().outputDirectory + "/"
				+ Config.get().trainedOnDataSet + "/"
				+ Config.get().trainedOnLang + "/index.txt";
		String[] wordIndex = ib.deserializeIndex(indexPath);

		KneserNeyMySQLSearcher glmmss = new KneserNeyMySQLSearcher(databaseName);
		// Config.get().weight = "no";
		// for (int i = 5; i > 1; i--) {
		// IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
		// + i);
		// tmss.run(i, 100000, Config.get().weight);
		// }
		// Config.get().weight = "pic";

		for (int n = 5; n > 1; n--) {
			IOHelper.strongLog("model parameter: " + n);

			for (int k = n; k > 1; k--) {
				glmmss.run(n, k, Config.get().numberOfQueries,
						Config.get().weight, wordIndex);
			}
		}
	}

	public KneserNeyMySQLSearcher(String databaseName) {
		super(databaseName);
	}

	@Override
	protected String prepareQuery(String[] words, int sequence, int pfl,
			String[] wordIndex) {
		int l = words.length;
		String target = words[l - 1];
		String source;
		int leadingZeros = 0;
		if (sequence == 1) {
			source = "true";
			// TODO:remove this:
			return null;
		} else {
			if (sequence % 2 == 0) {
				// no target in sequence (e.g. 110)
				return null;
			}
			if (Integer.bitCount(sequence) == this.k
					|| Integer.bitCount(sequence) == Integer.toBinaryString(
							sequence).length()
					&& Integer.bitCount(sequence) <= this.k) {
				source = "";
				String sequenceBinary = Integer.toBinaryString(sequence);
				while (sequenceBinary.length() < Config.get().modelLength) {
					sequenceBinary = "0" + sequenceBinary;
					leadingZeros++;
				}
				// convert binary sequence type into char[] for iteration
				char[] sequenceChars = sequenceBinary.toCharArray();

				// sequencePointer points at sequenceCut
				// length - 1 to leave out target
				for (int i = 0; i < sequenceChars.length - 1; i++) {
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
				return null;
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
		String tableName;
		if (sequence == 1) {
			tableName = "1_all";
		} else {
			tableName = Integer.toBinaryString(sequence) + "_"
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
