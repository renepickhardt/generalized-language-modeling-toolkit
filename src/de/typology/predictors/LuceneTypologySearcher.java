package de.typology.predictors;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.FieldValueFilter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import de.typology.utils.Config;

public class LuceneTypologySearcher {

	/**
	 * @param args
	 * @throws ParseException
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException, ParseException {
		LuceneTypologySearcher lts = new LuceneTypologySearcher();
		lts.search(Config.get().indexPath + "1/", "A");
		System.out.println("exit");
	}

	public void search(String indexDir, String q) throws IOException,
			ParseException {
		Directory directory = FSDirectory.open(new File(indexDir));
		DirectoryReader directoryReader = DirectoryReader.open(directory);
		IndexSearcher indexSearcher = new IndexSearcher(directoryReader);

		Term term = new Term("src", q);
		Query query = new TermQuery(term);

		SortField sortField = new SortField("cnt", SortField.Type.FLOAT, true);
		// true at sortField: enable reverse sort
		Sort sort = new Sort(sortField);
		TopDocs hits = indexSearcher.search(query, new FieldValueFilter("cnt"),
				5, sort);
		// change 3rd parameter at hits to change the number of results
		for (ScoreDoc scoreDoc : hits.scoreDocs) {
			Document doc = indexSearcher.doc(scoreDoc.doc);
			System.out.println(doc.get("tgt") + " " + doc.get("cnt"));
		}
	}
}
