package de.typology.evaluation;

import java.io.BufferedReader;
import java.io.IOException;

import de.typology.predictors.LuceneTypologySearcher;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class TypologyEvaluator {
	public static void main(String[] args) {
		LuceneTypologySearcher lts = new LuceneTypologySearcher();

		// int joinLength = 12;
		int topK = 10;

		for (int joinLength = 5; joinLength < 50; joinLength = joinLength + 2) {
			for (int n = 2; n < 6; n++) {

				BufferedReader br = IOHelper
						.openReadFile(Config.get().testingPath);
				try {
					String line = "";
					long start = System.currentTimeMillis();
					int cnt = 0;
					int queries = 0;
					IOHelper.setResultFile("joinlog/typo-" + n + "-joinLengh-"
							+ joinLength + "-" + Config.get().sampleRate
							+ Config.get().splitDataRatio + ".log." + start);
					IOHelper.log("!!!!!!!!!!TYPOLOGY EVAL: N = " + n);
					while ((line = br.readLine()) != null) {
						String sentence = line;
						String[] words = sentence.split("\\ ");
						if (words.length < n) {
							continue;
						}
						boolean flag = false;
						for (int l = 0; l < n; l++) {
							if (words[l].length() < 1) {
								flag = true;
							}
						}
						if (flag) {
							continue;
						}

						String query = "";
						if (n == 5) {
							query = words[0] + " " + words[1] + " " + words[2]
									+ " " + words[3];
						} else if (n == 4) {
							query = words[0] + " " + words[1] + " " + words[2];
						} else if (n == 3) {
							query = words[0] + " " + words[1];
						} else if (n == 2) {
							query = words[0];
						}

						// for (int j = 0; j < Math.min(words[n - 1].length(),
						// 4);
						// j++) {
						IOHelper.logResult(query + "  \tMATCH: " + words[n - 1]);
						for (int j = 0; j < words[n - 1].length() - 1; j++) {
							// include more logic
							int res = lts.query(query,
									words[n - 1].substring(0, j), words[n - 1],
									joinLength, topK);
							cnt++;
							if (cnt % 2000 == 0) {
								long time = System.currentTimeMillis() - start;
								IOHelper.strongLog(cnt + " predictions in "
										+ time + " ms \t" + cnt * 1000 / time
										+ " predictions / sec");
							}
							if (res == 1) {
								break;
							}
						}
						queries++;
						if (queries % 250 == 0) {
							long time = System.currentTimeMillis() - start;
							IOHelper.strongLog(queries + " queries in " + time
									+ " ms \t" + queries * 1000 / time
									+ " queries / sec");
						}
					}
					// }
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
