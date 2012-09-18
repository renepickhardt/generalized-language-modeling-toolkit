package de.typology.predictors;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;

import de.typology.interfaces.Predictable;

public class TypologyPredictor implements Predictable {
	private GraphDatabaseService graphDb;
	private String dbPath;
	private Index<Node> index;

	public TypologyPredictor(String dbPath) {
		// neo4j database initialization
		this.dbPath = dbPath;
		this.graphDb = new GraphDatabaseFactory()
				.newEmbeddedDatabase(this.dbPath);
		this.registerShutdownHook(this.graphDb);
	}

	@Override
	public String[] predict(String[] fourGram) {
		String[] result = new String[5];
		String s = fourGram[0];
		System.out.println("sould be: " + s);
		IndexHits<Node> hits = this.index.query("word", s + "*");
		System.out.println(hits.hasNext());
		for (Node n : hits) {
			System.out.println("is:" + n.getProperty("word"));
		}

		return result;
	}

	public String[] predict(String word, RelationshipType relType) {
		String[] result = new String[5];

		return result;
	}

	@Override
	public int getCorpusId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setCorpusId(int corpusId) {
		// TODO Auto-generated method stub

	}

	public double buildIndex() {
		long start_time = System.nanoTime();

		IndexManager ix = this.graphDb.index();
		this.index = ix.forNodes("word");

		long end_time = System.nanoTime();
		return Math.round((double) (end_time - start_time) / 1000) / 1000;

	}

	/**
	 * copied from
	 * http://docs.neo4j.org/chunked/stable/tutorials-java-embedded-setup.html
	 * 
	 * @param graphDb
	 */
	private void registerShutdownHook(final GraphDatabaseService graphDb) {
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running example before it's completed)
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}
}
