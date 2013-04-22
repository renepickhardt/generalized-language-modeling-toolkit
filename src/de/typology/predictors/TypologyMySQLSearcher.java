package de.typology.predictors;

import de.typology.splitter.BinarySearch;
import de.typology.splitter.IndexBuilder;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class TypologyMySQLSearcher extends MySQLSearcher {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IndexBuilder ib = new IndexBuilder();
		String databaseName = Config.get().trainedOnDataSet + "_"
				+ Config.get().trainedOnLang + "_typo";
		String indexPath = Config.get().outputDirectory + "/"
				+ Config.get().trainedOnDataSet + "/"
				+ Config.get().trainedOnLang + "/index.txt";
		String[] wordIndex = ib.deserializeIndex(indexPath);
		TypologyMySQLSearcher tmss = new TypologyMySQLSearcher(databaseName);
		// Config.get().weight = "no";
		// for (int i = 5; i > 1; i--) {
		// IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
		// + i);
		// tmss.run(i, 100000, Config.get().weight);
		// }
		// Config.get().weight = "pic";
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("model parameter: " + i);
			tmss.run(i, Config.get().numberOfQueries, Config.get().weight,
					wordIndex);
		}
	}

	public TypologyMySQLSearcher(String databaseName) {
		// general:
		super(databaseName);
	}

	// very specific to typology! needs to be exchanged for Language models!
	@Override
	protected String prepareQuery(String[] words, int i, int pfl,
			String[] wordIndex) {
		int l = words.length;
		String target = words[l - 1];
		String source;
		if (i == 0) {
			source = "true";
		} else {
			source = "source =\"" + words[l - 1 - i] + "\"";
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
		String tablePrefix = i + "es";
		String tableName;
		if (i == 0) {
			tableName = tablePrefix + "_all";
		} else {
			tableName = tablePrefix + "_"
					+ BinarySearch.rank(words[l - 1 - i], wordIndex);

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
