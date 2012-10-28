package de.typology.trainers;

import static de.typology.trainers.RelTypes.FOUR;
import static de.typology.trainers.RelTypes.ONE;
import static de.typology.trainers.RelTypes.THREE;
import static de.typology.trainers.RelTypes.TWO;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.tooling.GlobalGraphOperations;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import de.typology.utils.IOHelper;

/**
 * Trainer using list of edges to build a neo4j database using the heuristics of
 * typology.
 * <p>
 * edge format:
 * <p>
 * node_1\tnode_2\t#count\n
 * 
 * @author Martin Koerner
 * 
 */
public class Neo4JTypologyTrainer /* implements Trainable */{
	// TODO implement corpusId system
	// private int corpusId;
	private String storagePath;

	private BufferedReader reader;
	private ArrayList<File> files;
	private String line;
	private String[] lineSplit;
	private String temp;
	private Map<String, Object> properties;

	private int edgeCount;
	private int lineCount;

	private Node[] currentNodes = { null, null };
	private GraphDatabaseService graphDb;
	private BatchInserter inserter;
	private HashMap<String, Long> nodeMap = new HashMap<String, Long>();

	private static HashMap<Integer, RelTypes> relTypesMap = new HashMap<Integer, RelTypes>();
	static {
		relTypesMap.put(1, ONE);
		relTypesMap.put(2, TWO);
		relTypesMap.put(3, THREE);
		relTypesMap.put(4, FOUR);
	}

	public Neo4JTypologyTrainer(int corpusId, String storagePath) {
		// this.corpusId = corpusId;
		this.storagePath = storagePath;
	}

	public double train(String path) throws IOException {
		IOHelper.strongLog("building node map");
		double buildNodeMap = this.buildNodeMap(path);
		IOHelper.strongLog("training using batch inserter");
		double trainBatch = this.trainBatch(path);
		IOHelper.strongLog("training using lucene");
		double trainLucene = this.trainLucene(path);
		return buildNodeMap + trainBatch + trainLucene;
	}

	public double buildNodeMap(String path) throws IOException {
		this.lineCount = 0;
		long startTime = System.currentTimeMillis();

		this.inserter = BatchInserters.inserter(this.storagePath);
		HashMap<String, Integer> tempNodeMap = new HashMap<String, Integer>();
		for (int relType = 1; relType < 5; relType++) {
			this.files = IOHelper.getFileList(new File(path + relType));
			for (File file : this.files) {
				this.reader = IOHelper.openReadFile(file.getAbsolutePath());
				while ((this.line = this.reader.readLine()) != null) {
					this.lineCount++;
					if (this.lineCount % 1000000 == 0) {
						System.out.println(this.lineCount + " "
								+ (System.currentTimeMillis() - startTime)
								/ 1000 + " s");
					}

					// initialize edgeCount
					this.lineSplit = this.line.split("\t");
					if (this.lineSplit.length < 3) {
						continue;
					}

					this.temp = this.lineSplit[this.lineSplit.length - 1]
							.substring(1);
					this.edgeCount = Integer.parseInt(this.temp);

					if (!tempNodeMap.containsKey(this.lineSplit[0])) {
						tempNodeMap.put(this.lineSplit[0], 1);
					} else {
						tempNodeMap.put(this.lineSplit[0],
								tempNodeMap.get(this.lineSplit[0]) + 1);
					}
				}
			}
		}
		Iterator<Entry<String, Integer>> it = tempNodeMap.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, Integer> pair = it.next();
			if (pair.getValue() > 2) {
				this.properties = new HashMap<String, Object>();
				this.properties.put("word", pair.getKey());
				this.nodeMap.put(pair.getKey(),
						this.inserter.createNode(this.properties));
			}
		}
		System.out.println("HashMap size: " + this.nodeMap.size());
		this.inserter.shutdown();
		long endTime = System.currentTimeMillis();
		return (endTime - startTime) / 1000;
	}

	public double trainBatch(String path) throws IOException {
		long startTime = System.currentTimeMillis();
		this.lineCount = 0;

		Writer tempWriter = IOHelper.openWriteFile(path + "temp.txt",
				32 * 1024 * 1024);

		this.inserter = BatchInserters.inserter(this.storagePath);
		for (int relType = 1; relType < 5; relType++) {
			this.files = IOHelper.getFileList(new File(path + relType));
			for (File file : this.files) {
				this.reader = IOHelper.openReadFile(file.getAbsolutePath());
				while ((this.line = this.reader.readLine()) != null) {
					this.lineCount++;
					if (this.lineCount % 1000000 == 0) {
						System.out.println(this.lineCount + " "
								+ (System.currentTimeMillis() - startTime)
								/ 1000 + " s");
					}

					// initialize edgeCount
					this.lineSplit = this.line.split("\t");
					if (this.lineSplit.length < 3) {
						continue;
					}
					this.temp = this.lineSplit[this.lineSplit.length - 1]
							.substring(1);
					this.edgeCount = Integer.parseInt(this.temp);

					if (this.nodeMap.containsKey(this.lineSplit[0])
							&& this.nodeMap.containsKey(this.lineSplit[1])) {
						this.properties = new HashMap<String, Object>();
						this.properties.put("cnt", this.edgeCount);
						this.inserter.createRelationship(
								this.nodeMap.get(this.lineSplit[0]),
								this.nodeMap.get(this.lineSplit[1]),
								relTypesMap.get(relType), this.properties);
					} else {
						tempWriter.write(this.line + "\t" + relType + "\n");
						tempWriter.flush();
					}
				}
			}
		}
		tempWriter.close();
		this.inserter.shutdown();
		long endTime = System.currentTimeMillis();
		return (endTime - startTime) / 1000;

	}

	private double trainLucene(String path) throws IOException {
		long startTime = System.currentTimeMillis();

		// neo4j database initialization
		int relType;

		this.graphDb = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(
				this.storagePath).newGraphDatabase();
		IndexManager index = this.graphDb.index();
		Index<Node> nodeIndex = index.forNodes("word");
		this.lineCount = 0;
		Transaction tx = this.graphDb.beginTx();
		IOHelper.strongLog("	building index");
		try {
			for (Node n : GlobalGraphOperations.at(this.graphDb).getAllNodes()) {
				this.lineCount++;
				if (this.lineCount % 100000 == 0) {
					System.out.println(this.lineCount + " "
							+ (System.currentTimeMillis() - startTime) / 1000
							+ " s");
					// commit transaction
					tx.success();
					tx.finish();
					tx = this.graphDb.beginTx();
				}
				if (n.hasProperty("word")) {
					nodeIndex.add(n, "word", n.getProperty("word"));
				}
			}
			tx.success();
		} finally {
			tx.finish();
		}
		IOHelper.strongLog("	add edges from temp.txt");
		this.lineCount = 0;
		this.reader = IOHelper.openReadFile(path + "temp.txt");
		tx = this.graphDb.beginTx();
		try {
			while ((this.line = this.reader.readLine()) != null) {
				this.lineCount++;
				if (this.lineCount % 100000 == 0) {
					System.out.println(this.lineCount + " "
							+ (System.currentTimeMillis() - startTime) / 1000
							+ " s");
					// commit transaction
					tx.success();
					tx.finish();
					tx = this.graphDb.beginTx();

				}
				// initialize edgeCount
				this.lineSplit = this.line.split("\t");
				if (this.lineSplit.length != 4) {
					continue;
				}
				this.edgeCount = Integer
						.parseInt(this.lineSplit[this.lineSplit.length - 2]
								.substring(1));
				relType = Integer
						.parseInt(this.lineSplit[this.lineSplit.length - 1]);
				for (int i = 0; i < 2; i++) {
					Node currentNode = nodeIndex.get("word", this.lineSplit[i])
							.getSingle();
					if ((this.currentNodes[i] = currentNode) == null) {
						this.currentNodes[i] = this.graphDb.createNode();
						this.currentNodes[i].setProperty("word",
								this.lineSplit[i]);
					}
				}
				Relationship r = this.currentNodes[0].createRelationshipTo(
						this.currentNodes[1], relTypesMap.get(relType));
				r.setProperty("cnt", this.edgeCount);
			}
			tx.success();
		} finally {
			tx.finish();
		}
		this.reader.close();
		this.graphDb.shutdown();
		long endTime = System.currentTimeMillis();
		return (endTime - startTime) / 1000;
	}
}
