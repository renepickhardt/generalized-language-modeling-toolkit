package de.typology.trainers;

import java.io.File;
import java.io.IOException;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class MongoTypologyTrainerMain {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		MongoTypologyTrainer mtt = new MongoTypologyTrainer(1);
		// TODO drop database
		System.out.println("training");
		double time = mtt
				.train(IOHelper.openReadNGrams(Config.get().googleNgramsMergedPath));
		System.out.println(time + " ms");
		System.out.println("training done");
		System.out.println("print db");
		mtt.writeDB(Config.get().testDb);
		System.out.println("generate indicator file");
		File done = new File(Config.get().testDb + "IsDone." + time + "ms");
		done.createNewFile();
		System.out.println("done");
	}

}
