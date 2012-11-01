package de.typology.predictors;

import static de.typology.trainers.RelTypes.FOUR;
import static de.typology.trainers.RelTypes.ONE;
import static de.typology.trainers.RelTypes.THREE;
import static de.typology.trainers.RelTypes.TWO;

import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.kernel.EmbeddedReadOnlyGraphDatabase;

import de.typology.interfaces.Predictable;
import de.typology.trainers.RelTypes;

public class TypologyPredictor implements Predictable {
	private GraphDatabaseService graphDb;
	private String dbPath;
	private IndexManager index;
	private Index<Node> nodes;
	private TreeMap<String, Double> ends;
	private TreeMap<String, Double> sortedEnds;
	private Double[] weights = { 5.0, 2.0, 2.0, 0.0 };
	public static HashMap<Integer, RelTypes> relTypesMap = new HashMap<Integer, RelTypes>();
	static {
		relTypesMap.put(1, ONE);
		relTypesMap.put(2, TWO);
		relTypesMap.put(3, THREE);
		relTypesMap.put(4, FOUR);
	}

	private final Comparator<String> StringComparator = new Comparator<String>() {
		@Override
		public int compare(String a, String b) {
			if (TypologyPredictor.this.ends.get(a) >= TypologyPredictor.this.ends
					.get(b)) {
				return -1;
			} else {
				return 1;
			} // returning 0 would merge keys
		}
	};

	public TypologyPredictor(String dbPath) {
		// neo4j database initialization
		this.dbPath = dbPath;
		this.graphDb = new EmbeddedReadOnlyGraphDatabase(this.dbPath);
		this.registerShutdownHook(this.graphDb);
		this.index = this.graphDb.index();
		this.nodes = this.index.forNodes("word");
	}

	@Override
	public String[] predict(String[] fourGram) {
		String[] result = new String[5];
		this.ends = new TreeMap<String, Double>();
		for (int i = 0; i < 4; i++) {
			String s = fourGram[i];
			this.predict(s, 4 - i);
		}
		// sort TreeMap
		this.sortedEnds = new TreeMap<String, Double>(this.StringComparator);
		this.sortedEnds.putAll(this.ends);

		for (int i = 0; i < 5 && this.sortedEnds.size() > 0; i++) {
			result[i] = this.sortedEnds.pollFirstEntry().getKey();
		}
		return result;
	}

	public void predict(String word, int relType) {
		Node start = this.nodes.get("word", word).getSingle();
		if (start == null) {
			return;
		}
		for (Relationship relationship : start.getRelationships(
				relTypesMap.get(relType), Direction.OUTGOING)) {
			String end = (String) relationship.getEndNode().getProperty("word");
			Double endWeight = (Integer) relationship.getProperty("cnt")
					* this.weights[relType - 1];
			if (this.ends.containsKey(end)) {
				this.ends.put(end, this.ends.get(end) + endWeight);
			} else {
				this.ends.put(end, endWeight);
			}
		}
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

	public void shutdown() {
		this.graphDb.shutdown();
	}

}
