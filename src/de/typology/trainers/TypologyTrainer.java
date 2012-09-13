package de.typology.trainers;

import static de.typology.trainers.RelTypes.FOUR;
import static de.typology.trainers.RelTypes.ONE;
import static de.typology.trainers.RelTypes.THREE;
import static de.typology.trainers.RelTypes.TWO;

import java.util.HashMap;
import java.util.List;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import de.typology.interfaces.Trainable;

public class TypologyTrainer implements Trainable {
	// TODO implement corpusId system
	private int corpusId;
	private String storagePath;
	private NGramReader nr;
	private NGram currentNGram;
	private boolean realationshipFound;
	private List<Pair> currentListOfPairs;
	public GraphDatabaseService graphDb;
	public HashMap<String, Node> nodeMap = new HashMap<String, Node>();
	public static HashMap<Integer, RelTypes> relTypesMap = new HashMap<Integer, RelTypes>();
	static {
		relTypesMap.put(1, ONE);
		relTypesMap.put(2, TWO);
		relTypesMap.put(3, THREE);
		relTypesMap.put(4, FOUR);
	}

	// typeOne, typeTwo, typeThree, typeFour

	public TypologyTrainer(int corpusId, String storagePath) {
		this.corpusId = corpusId;
		this.storagePath = storagePath;
	}

	@Override
	public void train(NGramReader nr) {
		// neo4j database initialization
		this.graphDb = new GraphDatabaseFactory()
				.newEmbeddedDatabase(this.storagePath);
		this.registerShutdownHook(this.graphDb);

		this.nr = nr;
		while ((this.currentNGram = this.nr.readNGram()) != null) {
			for (int edgeType = 1; edgeType < 5; edgeType++) {
				this.currentListOfPairs = this.currentNGram
						.getPairsWithEdgeType(edgeType);
				for (Pair p : this.currentListOfPairs) {
					// add new words to graphDb and nodeMap
					if (!this.nodeMap.containsKey(p.getFirst())) {
						this.newNode(p.getFirst());
					}
					if (!this.nodeMap.containsKey(p.getSecond())) {
						this.newNode(p.getSecond());
					}

					Node start = this.nodeMap.get(p.getFirst());
					Node end = this.nodeMap.get(p.getSecond());
					this.realationshipFound = false;
					for (Relationship r : start.getRelationships(
							relTypesMap.get(edgeType), Direction.OUTGOING)) {
						if (r.getEndNode().equals(end)) {
							// increase count
							Transaction tx = this.graphDb.beginTx();
							try {
								r.setProperty(
										"cnt",
										(Integer) r.getProperty("cnt")
												+ this.currentNGram
														.getOccurrences());
								tx.success();
							} finally {
								tx.finish();
							}
							this.realationshipFound = true;
							break;
						}
					}
					if (this.realationshipFound == false) {
						// create new relationship
						this.newRelationship(start, end,
								relTypesMap.get(edgeType),
								this.currentNGram.getOccurrences());
					}

				}
			}
		}
		this.graphDb.shutdown();
	}

	@Override
	public int getCorpusId() {
		return this.corpusId;
	}

	@Override
	public void setCorpusId(int corpusId) {
		this.corpusId = corpusId;
	}

	public void newNode(String s) {
		Transaction tx = this.graphDb.beginTx();
		try {
			Node n = this.graphDb.createNode();
			n.setProperty("word", s);
			this.nodeMap.put(s, n);
			tx.success();
		} finally {
			tx.finish();
		}
	}

	public void newRelationship(Node source, Node target, RelationshipType rt,
			int i) {
		Transaction tx = this.graphDb.beginTx();
		try {
			Relationship r = source.createRelationshipTo(target, rt);
			r.setProperty("cnt", i);
			tx.success();
		} finally {
			tx.finish();
		}
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

	public Iterable<Relationship> getRelationships(Node source) {
		return source.getRelationships(Direction.OUTGOING);
	}
}
