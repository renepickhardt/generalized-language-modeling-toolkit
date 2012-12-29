package de.typology.predictors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.FieldValueFilter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;

import de.typology.utils.Algo;
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

/**
 * typology retrieval implmented on lucene
 * 
 * @author rpickhardt, Martin Koerner
 * 
 */
public class LuceneTypologySearcher {

	private ArrayList<IndexSearcher> index;
	private Sort sort;
	private QueryParser queryParser;
	private FieldValueFilter fieldValueFilter;
	private float[][] learningWeights;

	private float[][] usedWeights;
	private boolean useWeights;

	public LuceneTypologySearcher() {
		this.useWeights = true;
		this.openWeights();
		this.index = new ArrayList<IndexSearcher>();
		try {
			for (int i = 1; i < 5; i++) {
				Directory directory;
				DirectoryReader directoryReader;
				if (Config.get().loadIndexToRAM) {
					directory = MMapDirectory.open(new File(
							Config.get().indexPath + i + "/"));
				} else {
					directory = FSDirectory.open(new File(
							Config.get().indexPath + i + "/"));
				}
				directoryReader = DirectoryReader.open(directory);
				this.index.add(new IndexSearcher(directoryReader));
			}

			SortField sortField = new SortField("cnt", SortField.Type.FLOAT,
					true);
			this.sort = new Sort(sortField);
			Analyzer analyzer = new KeywordAnalyzer();
			this.queryParser = new QueryParser(Version.LUCENE_40, "src",
					analyzer);
			this.queryParser.setLowercaseExpandedTerms(false);
			this.fieldValueFilter = new FieldValueFilter("cnt");
			this.learningWeights = new float[100][Config.get().nGramLength];
			for (int i = 0; i < 100; i++) {
				for (int j = 0; j < Config.get().nGramLength; j++) {
					this.learningWeights[i][j] = 0;
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 * @throws ParseException
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException, ParseException {
	}

	public int query(String q, String prefix, String match,
			int intermediateListLength, int k) {

		// IOHelper.logLearn("TYPOLOGY - QUERY: " + q + " PREFIXLENGTH: "
		// + prefix.length() + " MATCH: " + match);

		HashMap<String, Float> result = this.search(q, prefix,
				intermediateListLength, match);
		Algo<String, Float> a = new Algo<String, Float>();
		TreeMap<Float, Set<String>> topkSuggestions = a.getTopkElements(result,
				k);
		;
		int topkCnt = 0;

		for (Float score : topkSuggestions.descendingKeySet()) {
			for (String suggestion : topkSuggestions.get(score)) {
				topkCnt++;
				if (suggestion.equals(match)) {
					IOHelper.logResult("HIT\tRANK: " + topkCnt
							+ " \tPREFIXLENGTH: " + prefix.length());
					if (topkCnt == 1) {
						IOHelper.logResult("KSS: "
								+ (match.length() - prefix.length()));
					}
					return topkCnt;
				}
			}
		}
		IOHelper.logResult("NOTHING\tPREFIXLENGTH: " + prefix.length());
		return -1;
	}

	// docs at:
	// http://www.ibm.com/developerworks/library/os-apache-lucenesearch/
	// http://stackoverflow.com/questions/468405/how-to-incorporate-multiple-fields-in-queryparser
	// http://oak.cs.ucla.edu/cs144/projects/lucene/index.html in chapter 2
	public HashMap<String, Float> search(String q, String prefix,
			int numIntermediateLists, String match) {
		HashMap<String, Float> result = new HashMap<String, Float>();
		try {

			String[] terms = q.split(" ");
			int edge = 0;

			for (int i = terms.length - 1; i >= Math.max(0, terms.length - 4); i--) {
				String special = "src:" + terms[i];
				if (prefix.length() > 0) {
					special = special.concat(" AND tgt:" + prefix + "*");
				}

				TopDocs results = this.index.get(edge).search(
						this.queryParser.parse(special), this.fieldValueFilter,
						numIntermediateLists, this.sort);

				int rank = 1;
				for (ScoreDoc scoreDoc : results.scoreDocs) {
					Document doc = this.index.get(edge).doc(scoreDoc.doc);

					String key = doc.get("tgt");
					Float value = Float.parseFloat(doc.get("cnt"));

					if (key.equals(match)) {
						this.learningWeights[prefix.length()][edge + 1] += 1 / (float) rank;
					}
					rank++;
					// String res = "";
					// if (key.equals(match)) {
					// res = " \tHIT";
					// } else {
					// res = " \tNOMATCH";
					// }
					// IOHelper.logLearn("FROM: " + terms[i] + " \tEDGETYPE: "
					// + (edge + 1) + " \t RANK: " + rank++
					// + " \tPREDICTS: " + key + "\t SCORE: " + value
					// + res);

					float weight = 1;
					if (this.useWeights) {
						weight = this.usedWeights[prefix.length()][edge + 1];
					}
					if (result.containsKey(key)) {
						result.put(key, weight * value + result.get(key));
					} else {
						result.put(key, weight * value);
					}
				}
				edge++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		return result;
	}

	public void saveWeights(int numQueries) {
		BufferedWriter bw = IOHelper.openWriteFile("weights");
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
}
