package de.typology.predictors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;

import de.typology.utils.Config;

/**
 * typology retrieval implmented on lucene
 * 
 * @author rpickhardt, Martin Koerner
 * 
 */
public class LuceneTypologySearcher extends Searcher {

	public LuceneTypologySearcher(int N, int k, int joinLength) {
		super(N, k, joinLength);

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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// docs at:
	// http://www.ibm.com/developerworks/library/os-apache-lucenesearch/
	// http://stackoverflow.com/questions/468405/how-to-incorporate-multiple-fields-in-queryparser
	// http://oak.cs.ucla.edu/cs144/projects/lucene/index.html in chapter 2
	@Override
	public HashMap<String, Float> search(String q, String prefix, String match) {
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
						this.joinLength, this.sort);

				int rank = 1;
				for (ScoreDoc scoreDoc : results.scoreDocs) {
					Document doc = this.index.get(edge).doc(scoreDoc.doc);
					String key = null;
					Float value = null;
					try {
						key = doc.get("tgt");
						value = Float.parseFloat(doc.get("cnt"));
					} catch (Exception e) {
						System.out
								.println("cant retrieve data from lucene index");
						continue;
					}
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
					if (Config.get().useWeights) {
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

	@Override
	public String getFileName() {
		String name = Config.get().dataSet + "-";
		if (Config.get().useWeights) {
			name = name.concat("weighted-");
		}
		name = name.concat("typo-" + this.N + "-joinLengh-" + this.joinLength
				+ "-" + Config.get().sampleRate + Config.get().splitDataRatio
				+ ".log");
		// TODO Auto-generated method stub
		return name;
	}

}
