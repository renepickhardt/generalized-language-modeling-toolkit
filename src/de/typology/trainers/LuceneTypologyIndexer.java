package de.typology.trainers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
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

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class LuceneTypologyIndexer {

	/**
	 * @param args
	 * @throws IOException
	 * @author Martin Koerner
	 */
	public static void main(String[] args) throws IOException {
		long startTime = System.currentTimeMillis();
		for (int edgeType = 1; edgeType < 5; edgeType++) {
			LuceneTypologyIndexer indexer = new LuceneTypologyIndexer(
					Config.get().indexPath + edgeType);
			indexer.index(Config.get().normalizedEdges + edgeType + "/");
			indexer.close();
		}
		long endTime = System.currentTimeMillis();
		IOHelper.strongLog((endTime - startTime) / 1000
				+ " seconds for indexing " + Config.get().normalizedEdges);
	}

	private BufferedReader reader;
	private IndexWriter writer;

	private String line;
	private String[] lineSplit;
	private Float edgeCount;

	public LuceneTypologyIndexer(String edgeDir) throws IOException {
		Directory dir = FSDirectory.open(new File(edgeDir));
		Analyzer analyzer = new SimpleAnalyzer(Version.LUCENE_40);
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
			IOHelper.log("indexing " + file.getAbsolutePath());
			this.indexFile(file);
		}
		return files.size();
	}

	private int indexFile(File file) throws NumberFormatException, IOException {
		this.reader = IOHelper.openReadFile(file.getAbsolutePath());
		int docCount = 0;
		while ((this.line = this.reader.readLine()) != null) {
			// extract information from line
			// line format: word\tword\t#edgeCount\n
			this.lineSplit = this.line.split("\t");
			if (this.lineSplit.length != 3) {
				IOHelper.strongLog("lineSplit length is "
						+ this.lineSplit.length);
			}
			this.edgeCount = Float
					.parseFloat(this.lineSplit[this.lineSplit.length - 1]
							.substring(1));
			Document document = this.getDocument(this.lineSplit[0],
					this.lineSplit[1], this.edgeCount);
			this.writer.addDocument(document);
			docCount++;
		}
		return docCount;
	}

	private Document getDocument(String source, String target, Float count) {
		Document document = new Document();
		document.add(new StringField("src", source, Field.Store.YES));
		document.add(new StringField("tgt", target, Field.Store.YES));
		document.add(new FloatField("cnt", count, Field.Store.YES));
		return document;
	}
}
