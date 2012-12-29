package de.typology.predictors;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;

import org.apache.lucene.queryparser.classic.QueryParser;

import de.typology.interfaces.Searchable;
import de.typology.utils.Config;
import de.typology.utils.EvalHelper;
import de.typology.utils.IOHelper;

public abstract class Searcher implements Searchable {

	@Override
	public int query(String q, String prefix, String match) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public HashMap<String, Float> search(String q, String prefix, String match) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveWeights(int numQueries) {
		// TODO Auto-generated method stub

	}

	@Override
	public void run(Searchable lts, int n, int topK, int joinLength) {
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
			IOHelper.setResultFile(this.getFileName());
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
					// int res = lts.query(query, match.substring(0, j), match,
					// joinLength, topK);
					// TODO: implement lts.update(n,topK,joinLength)
					int res = lts.query(query, match.substring(0, j), match);
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
