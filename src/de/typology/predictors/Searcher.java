package de.typology.predictors;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.FieldValueFilter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.Version;

import de.typology.interfaces.Searchable;
import de.typology.utils.Algo;
import de.typology.utils.Config;
import de.typology.utils.EvalHelper;
import de.typology.utils.IOHelper;

public abstract class Searcher implements Searchable {

	// @Override
	// abstract public HashMap<String, Float> search(String q, String prefix,
	// String match);

	protected ArrayList<IndexSearcher> index;
	protected Sort sort;
	protected QueryParser queryParser;
	protected FieldValueFilter fieldValueFilter;
	protected float[][] learningWeights;
	protected float[][] usedWeights;
	protected int joinLength;
	protected int k;
	protected int N;

	public Searcher(int n, int k, int joinLength) {
		this.k = k;
		this.N = n;
		this.joinLength = joinLength;
		SortField sortField = new SortField("cnt", SortField.Type.FLOAT, true);
		this.sort = new Sort(sortField);
		Analyzer analyzer = new KeywordAnalyzer();
		this.queryParser = new QueryParser(Version.LUCENE_40, "src", analyzer);
		this.queryParser.setLowercaseExpandedTerms(false);
		this.fieldValueFilter = new FieldValueFilter("cnt");
		this.learningWeights = new float[100][Config.get().nGramLength];
		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < Config.get().nGramLength; j++) {
				this.learningWeights[i][j] = 0;
			}
		}
	}

	@Override
	public int query(String q, String prefix, String match) {

		// IOHelper.logLearn("TYPOLOGY - QUERY: " + q + " PREFIXLENGTH: "
		// + prefix.length() + " MATCH: " + match);

		HashMap<String, Float> result = this.search(q, prefix, match);
		Algo<String, Float> a = new Algo<String, Float>();
		TreeMap<Float, Set<String>> topkSuggestions = a.getTopkElements(result,
				this.k);
		;
		int topkCnt = 0;

		for (Float score : topkSuggestions.descendingKeySet()) {
			for (String suggestion : topkSuggestions.get(score)) {
				topkCnt++;
				if (suggestion.equals(match)) {
					IOHelper.logResult("HIT\tRANK: " + topkCnt
							+ " \tPREFIXLENGTH: " + prefix.length() + " ");
					if (topkCnt == 1) {
						IOHelper.logResult("KSS: "
								+ (match.length() - prefix.length())+" ");
					}
					return topkCnt;
				}
			}
		}
		IOHelper.logResult("NOTHING\tPREFIXLENGTH: " + prefix.length() + " ");
		return -1;
	}

	@Override
	public void saveWeights(int numQueries) {
		BufferedWriter bw = IOHelper.openWriteFile("learntWeights" + this.getFileName());
		try {
			for (int i = 0; i < 10; i++) {
				bw.write("PREFIXLENGTH: " + i);
				float max = 0;
				for (int j = 1; j < Config.get().nGramLength; j++) {
					if (this.learningWeights[i][j] > max) {
						max = this.learningWeights[i][j];
					}
				}
				// TODO: can be divided by numQueries
				for (int j = 1; j < Config.get().nGramLength; j++) {
					bw.write(" w" + j + ": " + (int) this.learningWeights[i][j]
							* 1000 / max);
				}
				bw.write("\n");
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void openWeights() {
		this.usedWeights = new float[100][Config.get().nGramLength];

		for (int i = 0; i < 100; i++) {
			for (int j = 0; j < Config.get().nGramLength; j++) {
				this.usedWeights[i][j] = 1;
			}
		}

		this.usedWeights[0][1] = (float) 999.9863;
		this.usedWeights[0][2] = (float) 582.0288;
		this.usedWeights[0][3] = (float) 475.387;
		this.usedWeights[0][4] = (float) 442.76;
		this.usedWeights[1][1] = (float) 999.9864;
		this.usedWeights[1][2] = (float) 740.9225;
		this.usedWeights[1][3] = (float) 617.5018;
		this.usedWeights[1][4] = (float) 565.6135;
		this.usedWeights[2][1] = (float) 999.9889;
		this.usedWeights[2][2] = (float) 666.84924;
		this.usedWeights[2][3] = (float) 518.6447;
		this.usedWeights[2][4] = (float) 465.16992;
		this.usedWeights[3][1] = (float) 999.9662;
		this.usedWeights[3][2] = (float) 659.863;
		this.usedWeights[3][3] = (float) 514.01953;
		this.usedWeights[3][4] = (float) 459.6574;
		this.usedWeights[4][1] = (float) 999.95917;
		this.usedWeights[4][2] = (float) 674.7117;
		this.usedWeights[4][3] = (float) 518.66986;
		this.usedWeights[4][4] = (float) 457.94208;
		this.usedWeights[5][1] = (float) 999.91406;
		this.usedWeights[5][2] = (float) 675.6378;
		this.usedWeights[5][3] = (float) 513.2195;
		this.usedWeights[5][4] = (float) 443.8251;
		this.usedWeights[6][1] = (float) 999.95074;
		this.usedWeights[6][2] = (float) 692.6324;
		this.usedWeights[6][3] = (float) 512.2836;
		this.usedWeights[6][4] = (float) 435.3245;
		this.usedWeights[7][1] = (float) 999.85535;
		this.usedWeights[7][2] = (float) 703.2166;
		this.usedWeights[7][3] = (float) 510.41943;
		this.usedWeights[7][4] = (float) 431.86575;
		this.usedWeights[8][1] = (float) 999.9569;
		this.usedWeights[8][2] = (float) 698.27765;
		this.usedWeights[8][3] = (float) 504.47324;
		this.usedWeights[8][4] = (float) 435.7295;
		this.usedWeights[9][1] = (float) 999.7089;
		this.usedWeights[9][2] = (float) 682.14557;
		this.usedWeights[9][3] = (float) 474.29266;
		this.usedWeights[9][4] = (float) 406.00348;
	}

	@Override
	public void setTestParameter(int n, int topK, int joinLength) {
		this.N = n;
		this.k = topK;
		this.joinLength = joinLength;
	};

	@Override
	public void run() {
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
			IOHelper.log("!!!!!!!!!! EVAL " + this.getClass().getName()
					+ " : N = " + this.N);
			while ((line = br.readLine()) != null) {
				String[] words = line.split("\\ ");

				if (EvalHelper.badLine(words, this.N)) {
					continue;
				}

				String query = this.prepareQuery(words, this.N);
				String match = QueryParser.escape(words[words.length - 1]);

				IOHelper.logResult(query + "  \tMATCH: " + match);
				for (int j = 0; j < match.length() - 1; j++) {
					// int res = lts.query(query, match.substring(0, j), match,
					// joinLength, topK);
					// TODO: implement lts.update(n,topK,joinLength)
					int res = this.query(query, match.substring(0, j), match);
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
					this.saveWeights(queries);
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

	@Override
	public String prepareQuery(String[] words, int n) {
		String query = "";
		int l = words.length;
		for (int i = l - n; i < l - 1; i++) {
			query = query + QueryParser.escape(words[i]) + " ";
		}
		query = query.substring(0, query.length() - 1);
		return query;
	}

}
