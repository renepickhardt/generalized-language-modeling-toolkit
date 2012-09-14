package de.typology.trainers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

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

	@Test
	public void test() throws IOException {

		System.out.println("training");
		// this.tt.train(IOHelper
		// .openReadNGrams("tests/de/typology/trainers/ngrams.txt"));
		System.out
				.println(this.tt.train(IOHelper
						.openReadNGrams("D:/Arbeit/Typology/datasets/out/google-ngrams/degooglengramsmerged.txt"))
						+ " ms");
		System.out.println("training done");

		this.writeDB();

	}

	private void writeDB() throws IOException {
		this.graphDb = new EmbeddedGraphDatabase(Config.get().testDb);
		this.registerShutdownHook(this.graphDb);
		Writer writer = new OutputStreamWriter(new FileOutputStream(
				"D:/Arbeit/Typology/ngramsdboutput.txt"));
		for (Node n : this.graphDb.getAllNodes()) {
			if (n.hasProperty("word")) {
				writer.write((String) n.getProperty("word") + "\n");
				for (Relationship r : n.getRelationships(Direction.OUTGOING)) {
					writer.write("	-" + "type: " + r.getType()
							+ ", occurrences: " + r.getProperty("cnt") + "-> "
							+ r.getEndNode().getProperty("word") + "\n");
				}
				writer.flush();
			}
		}
		writer.close();
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
