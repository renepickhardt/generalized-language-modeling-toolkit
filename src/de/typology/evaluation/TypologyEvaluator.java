package de.typology.evaluation;

import java.io.BufferedReader;
import java.io.IOException;

import org.apache.lucene.queryparser.classic.QueryParser;

import de.typology.predictors.LuceneTypologySearcher;
import de.typology.utils.Config;
import de.typology.utils.EvalHelper;
import de.typology.utils.IOHelper;

public class TypologyEvaluator {
	public static void main(String[] args) {
		LuceneTypologySearcher lts = new LuceneTypologySearcher();
		int joinLength = 100;
		int topK = 10;
		// for (int joinLength = 5; joinLength < 50; joinLength = joinLength +
		// 2) {
		for (int n = 5; n < 6; n++) {
			run(lts, n, topK, joinLength);
			// }
		}
	}

	private static void run(LuceneTypologySearcher lts, int n, int topK,
			int joinLength) {
		BufferedReader br = IOHelper.openReadFile(Config.get().testingPath);
		try {
			String line = "";
			long start = System.currentTimeMillis();
			int cnt = 0;
			int queries = 0;
			// IOHelper.setResultFile("joinlog/typo-" + n +
			// "-joinLengh-"
			// + joinLength + "-" + Config.get().sampleRate
			// + Config.get().splitDataRatio + ".log." + start);
			IOHelper.setResultFile("typo-" + n + "-joinLengh-" + joinLength
					+ "-" + Config.get().sampleRate
					+ Config.get().splitDataRatio + ".log");
			IOHelper.log("!!!!!!!!!!TYPOLOGY EVAL: N = " + n);
			while ((line = br.readLine()) != null) {
				String[] words = line.split("\\ ");

				if (EvalHelper.badLine(words, n)) {
					continue;
				}

				String query = EvalHelper.prepareQuery(words, n);
				String match = QueryParser.escape(words[words.length - 1]);

				IOHelper.logResult(query + "  \tMATCH: " + match);
				for (int j = 0; j < match.length() - 1; j++) {
					int res = lts.query(query, match.substring(0, j), match,
							joinLength, topK);
					cnt++;
					if (cnt % 5000 == 0) {
						long time = System.currentTimeMillis() - start;
						IOHelper.strongLog(cnt + " predictions in " + time
								+ " ms \t" + cnt * 1000 / time
								+ " predictions / sec");
					}
					if (res == 1) {
						break;
					}
				}
				queries++;
				if (queries % 500 == 0) {
					lts.saveWeights(queries);
					long time = System.currentTimeMillis() - start;
					IOHelper.strongLog(queries + " queries in " + time
							+ " ms \t" + queries * 1000 / time
							+ " queries / sec");
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
