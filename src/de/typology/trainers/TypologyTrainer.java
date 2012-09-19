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
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;

import de.typology.interfaces.Trainable;

/**
 * Trainer using 5grams to build a neo4j database using the heuristics of
 * typology.
 * <p>
 * 5gram format:
 * <p>
 * NGram(5gram, occurrences)
 * 
 * @author Martin Koerner
 * 
 */
public class TypologyTrainer implements Trainable {
	// TODO implement corpusId system
	private int corpusId;
	private String storagePath;
	private NGramReader nGramReader;
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

	public TypologyTrainer(int corpusId, String storagePath) {
		this.corpusId = corpusId;
		this.storagePath = storagePath;
	}

	@Override
	public double train(NGramReader nGramReader) {
		long start_time = System.nanoTime();

		// neo4j database initialization
		this.graphDb = new GraphDatabaseFactory()
				.newEmbeddedDatabaseBuilder(this.storagePath)
				.setConfig(GraphDatabaseSettings.keep_logical_logs, "false")
				.newGraphDatabase();
		this.registerShutdownHook(this.graphDb);

		this.nGramReader = nGramReader;

		int nGramCount = 0;

		Transaction tx = this.graphDb.beginTx();
		try {
			while ((this.currentNGram = this.nGramReader.readNGram()) != null) {

				nGramCount++;
				if (nGramCount % 10000 == 0) {
					System.out
							.println(nGramCount
									+ " "
									+ Math.round((double) (System.nanoTime() - start_time) / 1000)
									/ 1000 + " ms");

					// commit transaction
					tx.success();
					tx.finish();
					tx = this.graphDb.beginTx();
				}

				for (int edgeType = 1; edgeType < 5; edgeType++) {
					// generate pairs of words with distance=edgeType
					this.currentListOfPairs = this.currentNGram
							.getPairsWithEdgeType(edgeType);
					for (Pair p : this.currentListOfPairs) {
						// add new words to graphDb
						if (!this.nodeMap.containsKey(p.getFirst())) {
							Node n = this.graphDb.createNode();
							n.setProperty("word", p.getFirst());
							this.nodeMap.put(p.getFirst(), n);
						}
						if (!this.nodeMap.containsKey(p.getSecond())) {
							Node n = this.graphDb.createNode();
							n.setProperty("word", p.getSecond());
							this.nodeMap.put(p.getSecond(), n);
						}

						Node start = this.nodeMap.get(p.getFirst());
						Node end = this.nodeMap.get(p.getSecond());
						this.realationshipFound = false;
						// iterate over all outgoing relationships of start with
						// current edgeType
						for (Relationship r : start.getRelationships(
								relTypesMap.get(edgeType), Direction.OUTGOING)) {
							if (r.getEndNode().equals(end)) {
								// if relationship already exists:increase count
								r.setProperty(
										"cnt",
										(Integer) r.getProperty("cnt")
												+ this.currentNGram
														.getOccurrences());
								this.realationshipFound = true;
								break;
							}
						}
						if (this.realationshipFound == false) {
							// else:create new relationship
							Relationship r = start.createRelationshipTo(end,
									relTypesMap.get(edgeType));
							r.setProperty("cnt",
									this.currentNGram.getOccurrences());
						}
					}
				}
			}
			tx.success();
		} finally {
			tx.finish();

		}
		this.graphDb.shutdown();
		long end_time = System.nanoTime();
		return Math.round((double) (end_time - start_time) / 1000) / 1000;
	}

	@Override
	public int getCorpusId() {
		return this.corpusId;
	}

	@Override
	public void setCorpusId(int corpusId) {
		this.corpusId = corpusId;
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
