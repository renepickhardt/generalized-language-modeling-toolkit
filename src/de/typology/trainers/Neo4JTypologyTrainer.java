package de.typology.trainers;

import static de.typology.trainers.RelTypes.FOUR;
import static de.typology.trainers.RelTypes.ONE;
import static de.typology.trainers.RelTypes.THREE;
import static de.typology.trainers.RelTypes.TWO;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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

	private int edgeCount;
	private int lineCount;

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
		long startTime = System.currentTimeMillis();

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
					String[] lineSplit = this.line.split("\t");
					if (lineSplit.length < 3) {
						continue;
					}
					String temp = lineSplit[lineSplit.length - 1].substring(1);
					this.edgeCount = Integer.parseInt(temp);

					for (int i = 0; i < 2; i++) {
						if (!this.nodeMap.containsKey(lineSplit[i])) {
							Map<String, Object> properties = new HashMap<String, Object>();
							properties.put("word", lineSplit[i]);
							long n = this.inserter.createNode(properties);
							this.nodeMap.put(lineSplit[i], n);
						}

					}
					Map<String, Object> properties = new HashMap<String, Object>();
					properties.put("cnt", this.edgeCount);
					this.inserter.createRelationship(
							this.nodeMap.get(lineSplit[0]),
							this.nodeMap.get(lineSplit[1]),
							relTypesMap.get(relType), properties);
				}
			}
		}
		this.inserter.shutdown();
		long endTime = System.currentTimeMillis();
		return (endTime - startTime) / 1000;

	}
}
