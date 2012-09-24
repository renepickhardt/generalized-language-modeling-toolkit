package de.typology.nGramBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class MongoNGramBuilder {
	private BufferedReader reader;
	private BufferedWriter writer;
	private DB db;
	private Mongo m;

	/**
	 * @param args
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException,
			InterruptedException {
		MongoNGramBuilder mngb = new MongoNGramBuilder();
		// TODO drop database
		System.out.println("building");
		double time = mngb.buildNGrams(Config.get().nGramInput);
		System.out.println(time + " ms");
		System.out.println("building done");
		System.out.println("print db");
		mngb.writeNGrams(Config.get().parsedNGrams);
		System.out.println("generate indicator file");
		File done = new File(Config.get().nGramInput + "IsDone." + time + "ms");
		done.createNewFile();
		System.out.println("done");
	}

	private double buildNGrams(String path) throws IOException,
			InterruptedException {
		long start_time = System.nanoTime();
		this.reader = IOHelper.openReadFile(path);

		this.m = new Mongo();
		this.db = this.m.getDB("mydb");

		Set<String> colls = this.db.getCollectionNames();
		for (String s : colls) {
			System.out.println(s);
		}

		BasicDBObject nGramCreateOptions = new BasicDBObject();
		nGramCreateOptions.append("capped", false);
		nGramCreateOptions.append("size", 1000);
		DBCollection nGramCollection = this.db.createCollection("ngrams",
				nGramCreateOptions);

		String line = "";
		while ((line = this.reader.readLine()) != null) {
			String[] tokens = line.split(" ");
			for (int i = 0; i < tokens.length - Config.get().nGramLength + 1; i++) {
				String nGram = "";
				for (int j = 0; j < Config.get().nGramLength; j++) {
					nGram += tokens[i + j] + " ";
				}
				DBObject currentNGram = nGramCollection.findOne(nGram);
				if (currentNGram == null) {
					System.out.println(nGram);
					// current ngram does not exist
					BasicDBObject newEdge = new BasicDBObject();
					newEdge.put("_id", nGram);
					newEdge.put("cnt", 1);
				} else {
					// current ngram does exist
					BasicDBObject set = new BasicDBObject("$set",
							new BasicDBObject("cnt",
									(Integer) currentNGram.get("cnt") + 1));
					nGramCollection.update(currentNGram, set);
				}

			}

		}

		long end_time = System.nanoTime();
		return Math.round((double) (end_time - start_time) / 1000) / 1000;
	}

	private void writeNGrams(String path) throws IOException {
		this.writer = IOHelper.openWriteFile(Config.get().parsedNGrams);
		this.m = new Mongo();
		// mongodb initialization
		this.db = this.m.getDB("mydb");
		DBCollection coll = this.db.getCollection("ngrams");
		DBCursor cursor = coll.find();
		try {
			while (cursor.hasNext()) {
				System.out.println(cursor.next() + "\n");
				// this.writer.write(cursor.next() + "\n");
				// this.writer.flush();
			}
		} finally {
			cursor.close();
			this.writer.close();
		}

	}
}
