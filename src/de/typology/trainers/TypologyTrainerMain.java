package de.typology.trainers;

import java.io.File;
import java.io.IOException;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class TypologyTrainerMain {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		TypologyTrainer tt;
		File f = new File(Config.get().testDb);
		if (f.exists()) {
			deleteTree(f);
		}
		tt = new TypologyTrainer(1, Config.get().testDb);

		System.out.println("training");
		double time = tt
				.train(IOHelper.openReadNGrams(Config.get().googleNgramsMergedPath));
		System.out.println(time + " ms");
		System.out.println("training done");

		// this.writeDB();
		System.out.println("generate indicator file");
		File done = new File(Config.get().testDb + "IsDone." + time + "ms");
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
	public static void deleteTree(File path) {
		for (File file : path.listFiles()) {
			if (file.isDirectory()) {
				deleteTree(file);
			}
			file.delete();
		}
		path.delete();
	}
}
