package de.typology.evaluation;

import java.io.BufferedReader;
import java.io.IOException;

import de.typology.predictors.LuceneNGramSearcher;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class NGramEvaluator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LuceneNGramSearcher lns = new LuceneNGramSearcher();

		BufferedReader br = IOHelper.openReadFile(Config.get().germanWikiText);
		try {
			String line = "";
			long start = System.currentTimeMillis();
			int cnt = 0;
			while ((line = br.readLine()) != null) {
				String[] sentences = line.split("\\.");
				for (String sentence : sentences) {
					String[] words = sentence.split("\\ ");
					if (words.length < 5) {
						continue;
					}
					boolean flag = false;
					for (int l = 0; l < 5; l++) {
						if (words[l].length() < 1) {
							flag = true;
						}
					}
					if (flag) {
						continue;
					}
					String query = words[0] + " " + words[1] + " " + words[2]
							+ " " + words[3];
					for (int j = 0; j < Math.min(words[4].length(), 4); j++) {
						cnt++;
						lns.query(query, words[4].substring(0, j), words[4]);
						if (cnt % 50 == 0) {
							long time = System.currentTimeMillis() - start;
							IOHelper.strongLog(cnt + " predictions in " + time
									+ " ms \t" + cnt * 1000 / time
									+ " predictions / sec");
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
