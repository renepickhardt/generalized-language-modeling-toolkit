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
								+ (match.length() - prefix.length()) + " ");
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
		BufferedWriter bw = IOHelper.openWriteFile("learntWeights"
				+ this.getFileName());
		try {
			for (int i = 0; i < 100; i++) {
				float max = 0;
				for (int j = 1; j < Config.get().nGramLength; j++) {
					if (this.learningWeights[i][j] > max) {
						max = this.learningWeights[i][j];
					}
				}
				// TODO: can be divided by numQueries
				for (int j = 1; j < Config.get().nGramLength; j++) {
					bw.write(this.learningWeights[i][j] * 1000
							/ (max * numQueries + 1) + "\t");
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
		try {

			for (int i = 0; i < 100; i++) {
				for (int j = 0; j < Config.get().nGramLength; j++) {
					this.usedWeights[i][j] = 1;
				}
			}
			BufferedReader br = IOHelper.openReadFile("learntWeights"
					+ this.getFileName());
			String line = "";
			int i = 0;
			while ((line = br.readLine()) != null) {
				String[] values = line.split("\t");
				if (values.length < Config.get().nGramLength) {
					break;
				}
				int j = 0;
				for (String value : values) {
					this.usedWeights[i][j++] = Float.parseFloat(value);
				}
				i++;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		System.out.println(Config.get().testingPath);
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
					if (Config.get().useWeights == false) {
						this.saveWeights(queries);
					}
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
