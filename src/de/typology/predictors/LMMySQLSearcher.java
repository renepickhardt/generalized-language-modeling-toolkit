package de.typology.predictors;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class LMMySQLSearcher extends MySQLSearcher {

	@Override
	protected String prepareQuery(String[] words, int i, int pfl) {
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
		String tableName = tablePrefix + "n" + source.charAt(0);
		if (!this.tabelNames.contains(tableName)) {
			tableName = tablePrefix + "nother";
		}
		String query = "";

		// create source statements
		String sourcepart = "";
		if (i == 1) {
			sourcepart = " where source1 = \"" + words[l - 2] + "\" ";
		}
		if (i == 2) {
			sourcepart = " where source1 = \"" + words[l - 3]
					+ "\" AND source2 = \"" + words[l - 2] + "\" ";
		}
		if (i == 3) {
			sourcepart = " where source1 = \"" + words[l - 4]
					+ "\" AND source2 = \"" + words[l - 3]
					+ "\" AND source3 = \"" + words[l - 2] + "\" ";
		}
		if (i == 4) {
			sourcepart = " where source1 = \"" + words[l - 5]
					+ "\" AND source2 = \"" + words[l - 4]
					+ "\" AND source3 = \"" + words[l - 3]
					+ "\" AND source4 = \"" + words[l - 2] + "\" ";
		}

		if (pfl > 0) {
			query = "select * from " + tableName + sourcepart
					+ " and target like \"" + prefix
					+ "\" order by score desc limit " + this.joinLength;
		} else {
			query = "select * from " + tableName + sourcepart
					+ " order by score desc limit " + this.joinLength;
		}
		return query;
	}

	LMMySQLSearcher() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// Config.get().dbName = "bigenwiki";
		LMMySQLSearcher lmss = new LMMySQLSearcher();
//		Config.get().weight = "no";
//		Config.get().useWeights = false;
//		for (int i = 5; i > 1; i--) {
//			IOHelper.strongLog("google ngrams tested on wiki ngramModel model parameter: "
//					+ i);
//			lmss.run(i, 100000, Config.get().weight);
//		}
//
//		Config.get().weight = "pic";
//		Config.get().useWeights = true;
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("google ngrams tested on wiki ngramModel model parameter: "
					+ i);
			lmss.run(i, 100000, Config.get().weight);
		}
	}

}
