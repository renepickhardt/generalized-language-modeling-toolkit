package de.typology.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import de.typology.predictors.TreeTypologySearcher;
import de.typology.trainers.SuggestTree;
import de.typology.trainers.TreeTypologyIndexer;
import de.typology.utils.Config;
import de.typology.utils.CopyDirectory;
import de.typology.utils.IOHelper;

public class TestTreeSampleEffectsEvaluator {
	private static String wikiType;

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO change wikiType
		wikiType = "dewiki";
		//part1BuildTrees();
		part2runTests();
	}

	//	private static void part1PrepareIndices() {
	//		for (int sampleRate = 98; sampleRate > 98; sampleRate -= 5) {
	//			Config.get().sampleRate = sampleRate;
	//			Config.get().sampleSplitData = true;
	//			try {
	//				String parsedEnglishWiki = Config.get().outputDirectory
	//						+ "wiki/" + wikiType + "/normalized.txt";
	//				String outputDirectory = Config.get().outputDirectory + "wiki/"
	//						+ wikiType + "/";
	//				WikiNGramBuilder.splitAndTrain(outputDirectory,
	//						parsedEnglishWiki);
	//			} catch (IOException e) {
	//				// TODO Auto-generated catch block
	//				e.printStackTrace();
	//			}
	//		}
	//	}

	private static void part2runTests() throws IOException {
		File dir = new File(Config.get().outputDirectory + "wiki/" + wikiType
				+ "/");
		for (File file : dir.listFiles()) {
			if (file.getName().startsWith("trainingSam")
					&& file.getName().endsWith("Split95Test50")) {
				// set parameters
				Config.get().sampleRate = Integer.parseInt(file.getName()
						.replace("trainingSam", "")
						.replace("Split95Test50", ""));
				Config.get().splitDataRatio = 95;
				int joinLength = 12;
				int topK = 5;

				// set paths for ngram tests
				Config.get().nGramIndexPath = file.getAbsolutePath() + "/nGramsIndex/";

				String suffix = file.getName().replace("training", "");
				Config.get().testingPath = file.getParent() + "/test" + suffix
						+ "/test.file";

				System.out.println("testingPath: " + Config.get().testingPath);

				//				// ngram tests
				//				TreeNGramSearcher lns = new TreeNGramSearcher(2, topK,
				//						joinLength);
				//				for (int n = 2; n < 6; n++) {
				//					lns.setTestParameter(n, topK, joinLength);
				//					lns.run();
				//				}

				// remove ngram indices
				IOHelper.deleteDirectory("/dev/shm/nGramsIndex/");

				// move typology indices 
				new
				CopyDirectory(file.getAbsolutePath() + "/typoEdgesIndex/",
						"/dev/shm/typoEdgesIndex/");

				// set path for typology tests
				Config.get().indexPath ="/dev/shm/typoEdgesIndex/";

				// typology tests 
				TreeTypologyIndexer tti=new TreeTypologyIndexer();
				HashMap <Integer,HashMap<String,SuggestTree<Float>>> treeMapMap= tti.run(Config.get().normalizedEdges);
				TreeTypologySearcher lts = new	TreeTypologySearcher(2, topK, joinLength,treeMapMap); 
				for (int n = 2;n < 6; n++) 	{
					lts.setTestParameter(n, topK, joinLength);
					lts.run();
				}

				// remove typology indices
				IOHelper.deleteDirectory("/dev/shm/typoEdgesIndex/");

			}
		}
	}
}
