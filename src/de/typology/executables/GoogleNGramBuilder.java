package de.typology.executables;

import java.io.File;
import java.io.IOException;

<<<<<<< HEAD
import de.typology.nGramBuilder.NGramFromGoogleBuilder;
=======
import de.typology.googleNGrams.NGramMergerMain;
import de.typology.googleNGrams.NGramParserMain;
import de.typology.nGramBuilder.NGramNormalizer;
>>>>>>> refs/remotes/origin/socketTree
import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class GoogleNGramBuilder {

	/**
	 * executes the following steps:
	 * <p>
	 * 1) parse and normalize google ngram data
	 * <p>
	 * 
	 * @author Rene Pickhardt, Martin Koerner
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// parse and normalize google ngram data:
		IOHelper.log("start building ngrams");
		NGramNormalizer ngn = new NGramNormalizer();
		File dir = new File(Config.get().googleInputDirectory);
		new File(Config.get().outputDirectory).mkdirs();
		for (File f : dir.listFiles()) {
			IOHelper.log(f.getAbsolutePath() + ":");
			// PARSE NGRAMS!
			String googleTyp = f.getName();

			//			String outPath = Config.get().outputDirectory + "google/"
			//					+ googleTyp + "/";
			//			String mergedGoogle = outPath+ "merged/";
			//			new File(mergedGoogle).mkdirs();
			//			if (Config.get().parseData) {
			//				NGramMergerMain.run(f.getAbsolutePath(), mergedGoogle);
			//				NGramParserMain.run(mergedGoogle,
			//						outPath);
			//			}

			NGramFromGoogleBuilder.run(Config.get().outputDirectory+"/google/"+googleTyp+"/", Config.get().outputDirectory+"/google/"+googleTyp+"/"+"normalized/1/1gram-normalized.txt");

		}
	}
}
