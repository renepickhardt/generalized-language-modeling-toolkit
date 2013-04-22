package de.typology.predictors;

import de.typology.splitter.BinarySearch;
import de.typology.splitter.IndexBuilder;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class LMMySQLSearcher extends MySQLSearcher {
	public LMMySQLSearcher(String databaseName) {
		super(databaseName);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IndexBuilder ib = new IndexBuilder();
		String databaseName = Config.get().trainedOnDataSet + "_"
				+ Config.get().trainedOnLang + "_ngram";
		String indexPath = Config.get().outputDirectory + "/"
				+ Config.get().trainedOnDataSet + "/"
				+ Config.get().trainedOnLang + "/index.txt";
		String[] wordIndex = ib.deserializeIndex(indexPath);
		LMMySQLSearcher lmss = new LMMySQLSearcher(databaseName);
		// Config.get().weight = "no";
		// Config.get().useWeights = false;
		// for (int i = 5; i > 1; i--) {
		// IOHelper.strongLog("google ngrams tested on wiki ngramModel model parameter: "
		// + i);
		// lmss.run(i, 100000, Config.get().weight);
		// }
		//
		// Config.get().weight = "pic";
		// Config.get().useWeights = true;
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("model parameter: " + i);
			lmss.run(i, Config.get().numberOfQueries, Config.get().weight,
					wordIndex);
		}
	}

	@Override
	protected String prepareQuery(String[] words, int i, int pfl,
			String[] wordIndex) {
		int l = words.length;
		String target = words[l - 1];
		String source = words[l - 1 - i];
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
		int tablePrefix = i + 1;
		// String tableName = tablePrefix + "n" + source.charAt(0);
		// if (!this.tabelNames.contains(tableName)) {
		// tableName = tablePrefix + "nother";
		// }
		String tableName;
		if (i == 0) {
			tableName = tablePrefix + "gs_all";
		} else {
			tableName = tablePrefix + "gs_"
					+ BinarySearch.rank(source, wordIndex);
		}
		String query = "";

		// create source statements
		String sourcepart = " where ";
		if (i == 0) {
			sourcepart += "true";
		}
		if (i == 1) {
			sourcepart += "source1 = \"" + words[l - 2] + "\"";
		}
		if (i == 2) {
			sourcepart += "source1 = \"" + words[l - 3] + "\" AND source2 = \""
					+ words[l - 2] + "\"";
		}
		if (i == 3) {
			sourcepart += "source1 = \"" + words[l - 4] + "\" AND source2 = \""
					+ words[l - 3] + "\" AND source3 = \"" + words[l - 2]
					+ "\"";
		}
		if (i == 4) {
			sourcepart += "source1 = \"" + words[l - 5] + "\" AND source2 = \""
					+ words[l - 4] + "\" AND source3 = \"" + words[l - 3]
					+ "\" AND source4 = \"" + words[l - 2] + "\"";
		}

		if (pfl > 0) {
			query = "select * from " + tableName + sourcepart
					+ " and target like \"" + prefix
					+ "\" order by score desc limit " + this.joinLength;
		} else {
			query = "select * from " + tableName + sourcepart
					+ " order by score desc limit " + this.joinLength;
		}
		System.out.println(query);
		return query;
	}
}
