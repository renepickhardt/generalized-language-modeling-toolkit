package de.typology.trainers;

import static de.typology.trainers.RelTypes.FOUR;
import static de.typology.trainers.RelTypes.ONE;
import static de.typology.trainers.RelTypes.THREE;
import static de.typology.trainers.RelTypes.TWO;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import de.typology.interfaces.Trainable;

public class MongoTypologyTrainer implements Trainable{
	private int corpusId;
	private DB db;
	private NGramReader nGramReader;
	private NGram currentNGram;
	private List<Pair> currentListOfPairs;
	private Mongo m;

	public static HashMap<Integer, RelTypes> relTypesMap = new HashMap<Integer, RelTypes>();
	static {
		relTypesMap.put(1, ONE);
		relTypesMap.put(2, TWO);
		relTypesMap.put(3, THREE);
		relTypesMap.put(4, FOUR);
	}

	public MongoTypologyTrainer(int corpusId) {
		this.corpusId = corpusId;
	}

	@Override
	public double train(NGramReader nGramReader) {
		try {
			this.m = new Mongo();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long start_time = System.nanoTime();

		// mongodb initialization
		this.db = this.m.getDB( "mydb" );


		//create new node collection
		BasicDBObject nodeCreateOptions = new BasicDBObject();
		nodeCreateOptions.append("capped", true);
		//TODO change size (at the moment: 2gb)
		nodeCreateOptions.append("size", 2147483648.0);
		DBCollection nodeCollection = this.db.createCollection("nodes", nodeCreateOptions);

		//create new edge collection
		BasicDBObject edgeCreateOptions = new BasicDBObject();
		edgeCreateOptions.append("capped", true);
		//TODO change size (at the moment: 2gb)
		edgeCreateOptions.append("size", 2147483648.0); 
		DBCollection  edgeCollection=this.db.createCollection("edges", edgeCreateOptions);

		//TODO remove this
		Set<String> colls = this.db.getCollectionNames();
		colls = this.db.getCollectionNames();
		for (String s : colls) {
			System.out.println(s);
		}


		this.nGramReader = nGramReader;

		int nGramCount = 0;


		while ((this.currentNGram = this.nGramReader.readNGram()) != null) {
			nGramCount++;
			if (nGramCount % 100 == 0) {
				System.out
				.println(nGramCount
						+ " "
						+ Math.round((double) (System.nanoTime() - start_time) / 1000)
						/ 1000 + " ms");

				// commit transaction

			}
			for (int edgeType = 1; edgeType < 5; edgeType++) {
				// generate pairs of words with distance=edgeType
				this.currentListOfPairs = this.currentNGram
						.getPairsWithEdgeType(edgeType);
				for (Pair p : this.currentListOfPairs) {
					// add new words to graphDb
					BasicDBObject first = new BasicDBObject();
					first.put("_id", p.getFirst());
					if(nodeCollection.findOne(first)==null) {
						nodeCollection.insert(first);
					}

					BasicDBObject second = new BasicDBObject();
					second.put("_id", p.getSecond());
					if(nodeCollection.findOne(second)==null) {
						nodeCollection.insert(second);
					}

					// iterate over all outgoing relationships of start with
					// current edgeType
					String edgeID=p.getFirst()+"|"+edgeType+"|"+p.getSecond();
					// if relationship does not exists
					DBObject currentEdge=edgeCollection.findOne(edgeID);
					if(currentEdge==null) {
						BasicDBObject newEdge = new BasicDBObject();
						newEdge.put("_id", edgeID);
						newEdge.put("type", edgeType);
						newEdge.put("cnt", this.currentNGram.getOccurrences());
						edgeCollection.insert(newEdge);
					}else{
						BasicDBObject set = new BasicDBObject("$set", new BasicDBObject("cnt", (Integer)currentEdge.get("cnt")+this.currentNGram.getOccurrences()));
						edgeCollection.update(currentEdge, set);
					}}
			}
		}




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

	public void writeDB(String path){
		//		try {
		//			Writer writer = new OutputStreamWriter(new FileOutputStream(
		//					path));
		//		} catch (FileNotFoundException e) {
		//			// TODO Auto-generated catch block
		//			e.printStackTrace();
		//		}

		try {
			this.m = new Mongo();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// mongodb initialization
		this.db = this.m.getDB( "mydb" );
		for(String s:this.db.getCollectionNames()){
			DBCollection coll=this.db.getCollection(s);
			DBCursor cursor = coll.find();
			try {
				while(cursor.hasNext()) {
					System.out.println(cursor.next());
				}
			} finally {
				cursor.close();
			}
		}
	}
}
