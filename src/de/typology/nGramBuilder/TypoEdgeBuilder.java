package de.typology.nGramBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashMap;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;


public class TypoEdgeBuilder {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		//		Config.get().createNGramChunks=false;
		//		Config.get().createSecondLevelNGramChunks=false;
		//		Config.get().aggregateNGramChunks=false;


		TypoEdgeBuilder.run("/home/martin/out/google/test/","/home/martin/out/google/test/normalized/1/1gram-normalized.txt");


	}
	public static void run(String trainingPath, String trainingFile) throws IOException {
		long startTime = System.currentTimeMillis();
		long endTime = 0;
		long sek = 0;

		String[] letters = countMostFrequentStartingLetters(62, trainingFile,
				trainingPath);

		BufferedReader br;
		BufferedWriter bw;
		String line;
		String[] lineSplit;
		int edgeCount;
		int lineCount;


		//cut down ngrams
		NGramNormalizer ngn=new NGramNormalizer();
		new File(trainingPath+"/cut/").mkdirs();
		ngn.removeNGrams(trainingPath+"/normalized/", trainingPath+"/cut/");

		for(int n=2;n<6;n++){
			NGramAndCountChunkCreator ngcc = new NGramAndCountChunkCreator(letters);
			if (Config.get().createNGramChunks) {

				ngcc.createNGramChunks(trainingPath+"cut/"+n+"/"+n+"gram-normalized.txt", n, trainingPath);
				endTime = System.currentTimeMillis();
				sek = (endTime - startTime) / 1000;
				IOHelper.strongLog(sek
						+ " seconds to: finnish creating first chunks");
			}
			//			//most outer typo edges can be retrieved without aggregation
			//			new File(nGramsDir.getParent()+"/typoEdges/aggregated/"+(n-1)+"/").mkdirs();
			//			br = IOHelper.openReadFile(nGramsDir.getParent()+"/cut/"+(n-1)+"/"+(n-1)+"ngram-normalized.txt", 8 * 1024 * 1024);
			//			bw = IOHelper.openWriteFile(nGramsDir.getParent()+"/typoEdges/aggregated/"+(n-1)+"/other."+(n-1)+"ga", 32 * 1024 * 1024);
			//
			//			br = IOHelper.openReadFile(nGramsInput+"/"+n+"/", 8 * 1024 * 1024);
			//			lineCount=0;
			//			while((line=br.readLine())!=null){
			//				lineCount++;
			//
			//				// extract information from line
			//				// line format: word\tword\t#edgeCount\n
			//				lineSplit = line.split("\t");
			//				if (lineSplit.length !=n+1) {
			//					IOHelper.strongLog("wrong ngram format:"+lineSplit.length+" should be "+n+1);
			//					continue;
			//				}
			//				edgeCount = Integer
			//						.parseInt(lineSplit[lineSplit.length - 1]
			//								.substring(1));
			//				bw.write(lineSplit[0]+"\t"+lineSplit[lineSplit.length-2]+"\t#"+edgeCount);
			//			}
			//			bw.flush();
			//			bw.close();

		}

	}

	// TODO: need to generalize this function if it should fit in more general
	// tests! need to take away the pointing to trainingFile and
	// put it as a parameter sourceFile. also the output file should be saved to
	// a different spot

	private static String[] countMostFrequentStartingLetters(int k,
			String trainingFile, String trainingPath) {
		IOHelper.log("Counting the " + k + " most frequent letters in: "
				+ trainingFile);
		String[] result = new String[k];
		try {
			IOHelper.log("Check if most frequent letters have been counted");
			File file = new File(trainingPath
					+ Config.get().nGramsNotAggregatedPath
					+ "mostFrequentLetters.txt");
			if (file.exists()) {
				IOHelper.log("Yes! load from file");
				FileInputStream f = new FileInputStream(file);
				ObjectInputStream s;
				s = new ObjectInputStream(f);
				result = (String[]) s.readObject();
				s.close();
				return result;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		IOHelper.log("No! just start counting now");

		BufferedReader br = IOHelper.openReadFile(trainingFile);// Config.get().nGramKeyFile);
		String line = "";
		int cnt = 0;

		HashMap<String, Integer> letters = new HashMap<String, Integer>();

		try {
			while ((line = br.readLine()) != null) {
				cnt++;
				String[] temp=line.split("\t");
				String[] tokens = Arrays.copyOf(temp, temp.length-1);
				for (String token : tokens) {
					if (token == null || token.length() < 1) {
						continue;
					}
					String key = token.substring(0, 1);
					Integer c = letters.get(key);
					if (c == null) {
						letters.put(key, 1);
					} else {
						letters.put(key, c + 1);
					}
				}
				if (cnt % 50000 == 0) {
					System.out.println(cnt);
				}
			}

			int done = 0;
			while (letters.size() > 0 && done < k) {
				int max = 0;
				String maxLetter = null;
				for (String key : letters.keySet()) {
					int tmp = letters.get(key);
					if (tmp > max) {
						maxLetter = key;
						max = tmp;
					}
				}
				letters.remove(maxLetter);
				if (maxLetter.equals("-")) {
					continue;
				}
				result[done] = maxLetter;
				done++;
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		File file = new File(trainingPath
				+ Config.get().nGramsNotAggregatedPath
				+ "mostFrequentLetters.txt");
		try {
			IOHelper.log("Counting done! save results to file");

			FileOutputStream f = new FileOutputStream(file);
			ObjectOutputStream s = new ObjectOutputStream(f);

			s.writeObject(result);
			s.flush();
			s.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}

}
