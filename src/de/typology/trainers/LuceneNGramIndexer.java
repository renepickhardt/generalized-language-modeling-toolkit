package de.typology.trainers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import de.typology.utils.IOHelper;

/**
 * 
 * @author rpickhardt
 * 
 */
public class LuceneNGramIndexer {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
	}

	public static void run(String normalizedNGrams, String indexNGrams)
			throws NumberFormatException, IOException {
		long startTime = System.currentTimeMillis();
		for (int nGramType = 2; nGramType < 6; nGramType++) {
			LuceneNGramIndexer indexer = new LuceneNGramIndexer(indexNGrams
					+ nGramType);
			indexer.index(normalizedNGrams + nGramType + "/");
			indexer.close();
		}
		long endTime = System.currentTimeMillis();
		IOHelper.strongLog((endTime - startTime) / 1000
				+ " seconds for indexing " + normalizedNGrams);
	}

	private BufferedReader reader;
	private IndexWriter writer;

	private String line;
	private String[] lineSplit;
	private Float edgeCount;

	public LuceneNGramIndexer(String nGramDir) throws IOException {
		Directory dir = FSDirectory.open(new File(nGramDir));
		// TODO: change Analyzer since this one makes stuff case insensitive...
		// also change this in quering: see important note:
		// http://oak.cs.ucla.edu/cs144/projects/lucene/index.html in chapter
		// 2.0
		// http://stackoverflow.com/questions/2487736/lucene-case-sensitive-insensitive-search
		// Analyzer analyzer = new TypologyAnalyzer(Version.LUCENE_40);
		Analyzer analyzer = new KeywordAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40,
				analyzer);
		config.setOpenMode(OpenMode.CREATE);
		this.writer = new IndexWriter(dir, config);
	}

	public void close() throws IOException {
		this.writer.close();
	}

	/**
	 * Given a directory containing typology edge files, index() builds a Lucene
	 * index. See getDocument() for document structure.
	 * 
	 * @param dataDir
	 * @param filter
	 * @return
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public int index(String dataDir) throws NumberFormatException, IOException {
		ArrayList<File> files = IOHelper.getDirectory(new File(dataDir));
		for (File file : files) {
			if (file.getName().contains("distribution")) {
				IOHelper.log("skipping " + file.getAbsolutePath());
				continue;
			}
			IOHelper.log("indexing " + file.getAbsolutePath());
			this.indexFile(file);
		}
		return files.size();
	}

	private int indexFile(File file) throws NumberFormatException, IOException {
		this.reader = IOHelper.openReadFile(file.getAbsolutePath());
		int docCount = 0;
		while ((this.line = this.reader.readLine()) != null) {
			this.lineSplit = this.line.split("\t#");
			if (this.lineSplit.length != 2) {
				IOHelper.strongLog("can;t index line split is incorrectly"
						+ this.lineSplit.length);
				continue;
			}
			this.edgeCount = Float
					.parseFloat(this.lineSplit[this.lineSplit.length - 1]);
			String tmp = this.lineSplit[0].replace('\t', ' ');
			this.lineSplit = tmp.split(" ");
			String lastword = this.lineSplit[this.lineSplit.length - 1];
			tmp = tmp.substring(0, tmp.length() - (lastword.length() + 1));
			// System.out.println(tmp + " " + lastword);
			Document document = this.getDocument(tmp, lastword, this.edgeCount);
			this.writer.addDocument(document);
			docCount++;
		}
		return docCount;
	}

	private Document getDocument(String source, String target, Float count) {
		Document document = new Document();
		document.add(new StringField("src", source, Field.Store.NO));
		document.add(new StringField("tgt", target, Field.Store.YES));
		document.add(new FloatField("cnt", count, Field.Store.YES));
		return document;
	}

}
