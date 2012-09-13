package de.typology.trainers;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class TypologyTrainerTest {
	TypologyTrainer tt;
	EmbeddedGraphDatabase graphDb;

	@Before
	public void setUp() throws Exception {
		File f = new File(Config.get().testDb);
		if (f.exists()) {
			deleteTree(f);
		}

		this.tt = new TypologyTrainer(1, Config.get().testDb);

	}

	@After
	public void tearDown() throws Exception {

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		// deleteTree(new File(Config.get().testDb));
	}

	@Test
	public void test() {

		System.out.println("training");
		// this.tt.train(IOHelper
		// .openReadNGrams("tests/de/typology/trainers/ngrams.txt"));
		this.tt.train(IOHelper
				.openReadNGrams("D:/Arbeit/Typology/googlengrams_short.txt"));
		System.out.println("training done");
		this.graphDb = new EmbeddedGraphDatabase(Config.get().testDb);
		this.registerShutdownHook(this.graphDb);

		for (Node n : this.graphDb.getAllNodes()) {
			if (n.hasProperty("word")) {
				System.out.println(n.getProperty("word"));
				for (Relationship r : n.getRelationships(Direction.OUTGOING)) {
					System.out.println("	-" + r.getProperty("cnt") + "-> "
							+ r.getEndNode().getProperty("word"));
				}
			}
		}

		this.graphDb.shutdown();
	}

	/**
	 * deletes a directory recursively
	 * <p>
	 * taken from:
	 * <p>
	 * http://openbook.galileodesign.de/javainsel5/javainsel12_000.htm
	 * <p>
	 * at: 12.1.8 Dateien und Verzeichnisse löschen
	 */
	public static void deleteTree(File path) {
		for (File file : path.listFiles()) {
			if (file.isDirectory()) {
				deleteTree(file);
			}
			file.delete();
		}
		path.delete();
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
