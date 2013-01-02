package de.typology.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import de.typology.predictors.TreeTypologySearcher;
import de.typology.trainers.SuggestTree;
import de.typology.trainers.TreeIndexer;
import de.typology.utils.Config;

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

				String suffix = file.getName().replace("training", "");
				Config.get().testingPath = file.getParent() + "/test" + suffix
						+ "/test.file";
				Config.get().normalizedEdges=file.getAbsolutePath()+"/typoEdgesNormalized/";

				System.out.println("testingPath: " + Config.get().testingPath);
				System.out.println("normalizedEdges: "+Config.get().normalizedEdges);



				// typology tests 
				TreeIndexer tti=new TreeIndexer();
				HashMap <Integer,HashMap<String,SuggestTree<Float>>> treeMapMap= tti.run(Config.get().normalizedEdges);
				TreeTypologySearcher tts = new	TreeTypologySearcher(5, topK, joinLength,treeMapMap); 
				for (int n = 2;n < 6; n++) 	{
					tts.setTestParameter(n, topK, joinLength);
					tts.run();
				}
			}
		}
	}
}
