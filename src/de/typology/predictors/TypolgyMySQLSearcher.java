package de.typology.predictors;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class TypolgyMySQLSearcher extends MySQLSearcher {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TypolgyMySQLSearcher tmss = new TypolgyMySQLSearcher();
		Config.get().weight = "no";
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
					+ i);
			tmss.run(i, 100000, Config.get().weight);
		}
		Config.get().weight = "pic";
		for (int i = 5; i > 1; i--) {
			IOHelper.strongLog("google ngrams tested on wiki typology model parameter: "
					+ i);
			tmss.run(i, 100000, Config.get().weight);
		}
	}

	public TypolgyMySQLSearcher() {
		// general:
		super();
	}

	// veryspecific to typology! needs to be exchanged for Language models!
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
		String tableName = i + "es" + source.charAt(0);
		if (!this.tabelNames.contains(tableName)) {
			tableName = i + "n" + source.charAt(0);
			// really dirty quickfix for bug that table names break op on
			// import.
			if (!this.tabelNames.contains(tableName)) {
				tableName = i + "esother";
				if (!this.tabelNames.contains(tableName)) {
					tableName = i + "nother";
				}
			}
		}
		String query = "";
		if (pfl > 0) {
			query = "select * from " + tableName + " where source =\"" + source
					+ "\" and target like \"" + prefix
					+ "\" order by score desc limit " + this.joinLength;
		} else {
			query = "select * from " + tableName + " where source =\"" + source
					+ "\" order by score desc limit " + this.joinLength;
		}

		return query;
	}
}
