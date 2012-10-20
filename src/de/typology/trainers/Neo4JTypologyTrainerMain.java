package de.typology.trainers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import de.typology.utils.Config;

public class Neo4JTypologyTrainerMain {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		Neo4JTypologyTrainer tt;
		File f = new File(Config.get().dbPath);
		if (f.exists()) {
			deleteTree(f);
		}
		tt = new Neo4JTypologyTrainer(1, Config.get().dbPath);

		System.out.println("training");
		double time = tt.train(Config.get().edgeInput);
		System.out.println(time + " s");
		System.out.println("training done");

		// writeDB(Config.get().dbPath);
		System.out.println("generate indicator file");
		File done = new File(Config.get().dbPath + "IsDone." + time + "s");
		done.createNewFile();
		System.out.println("done");
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
	private static void deleteTree(File path) {
		for (File file : path.listFiles()) {
			if (file.isDirectory()) {
				deleteTree(file);
			}
			file.delete();
		}
		path.delete();
	}

	private static void writeDB(String path) throws IOException {

		EmbeddedGraphDatabase graphDb = new EmbeddedGraphDatabase(path);

		Writer writer = new OutputStreamWriter(new FileOutputStream(path
				+ ".txt"));

		for (Node n : graphDb.getAllNodes()) {
			if (n.hasProperty("word")) {
				writer.write((String) n.getProperty("word") + "\n");
				for (Relationship r : n.getRelationships(Direction.OUTGOING)) {
					writer.write("  -" + "type: " + r.getType() + ", cnt: "
							+ r.getProperty("cnt") + ", end: "
							+ r.getEndNode().getProperty("word") + "\n");
				}
				writer.flush();

			}

		}
		writer.close();
		graphDb.shutdown();

	}

}
