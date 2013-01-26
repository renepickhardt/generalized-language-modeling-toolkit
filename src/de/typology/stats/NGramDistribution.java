package de.typology.stats;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import de.typology.utils.IOHelper;

public class NGramDistribution {
	public NGramDistribution() {
	}

	public boolean countDistribution(String sourcePath, String fileExtension) {
		File[] files = IOHelper.getAllFilesInDirWithExtension(sourcePath,
				fileExtension, this.getClass().getName() + "countDistribution");

		int lCnt = 0;
		int n = 1000000;
		long[] count = new long[n];
		long totalCount=0;
		long uniqueWords=0;
		for (int i = 0; i < n; i++) {
			count[i] = 0;
		}
		for (File f : files) {
			String fileName = f.getName();
			if (!fileName.endsWith(fileExtension)) {
				IOHelper.log(fileName
						+ " is not an sorted ngram file. process next");
				continue;
			}
			String fullQualifiedFileName = sourcePath + File.separator
					+ fileName;
			IOHelper.strongLog("\t\t\tstart counting: " + fullQualifiedFileName);

			try {
				BufferedReader br = IOHelper.openReadFile(
						fullQualifiedFileName, 32 * 1024 * 1024);
				String line = "";
				while ((line = br.readLine()) != null) {
					String[] values = line.split("\t#");
					if (values.length != 2) {
						IOHelper.log("bad format for: " + line + " in file: "
								+ fullQualifiedFileName);
						continue;
					}
					Integer value = Integer.parseInt(values[1]);
					try {
						count[value.intValue()]++;
					} catch (ArrayIndexOutOfBoundsException e) {
						IOHelper.strongLog("cant count such high frequencies: "
								+ value.intValue());
						continue;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		BufferedWriter bw = IOHelper.openWriteFile(sourcePath + "/distribution"
				+ fileExtension);
		try {
			for (int i = n - 1; i > 0; i--) {
				if (count[i] > 0) {
					bw.write(i + "\t" + count[i] + "\n");
					IOHelper.log(count[i] + " ngrams " + i + " times");
					totalCount+=i*count[i];
					uniqueWords+=count[i];
				}
			}
			this.printStats(sourcePath, fileExtension, totalCount, uniqueWords);
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	private void printStats(String sourcePath, String fileExtension, long wordCount, long uniqueWords) throws IOException {
		File file2=new File(sourcePath);
		File file=new File(file2.getParentFile().getParentFile().getAbsolutePath()+"/training.file");
		BufferedWriter bw = IOHelper.openWriteFile(sourcePath + "/stats"
				+ fileExtension);
		bw.write(file.getAbsolutePath() + ":" + "\n");
		bw.write("\t" + "total words: " + wordCount+"\n");

		bw.write("\t" + "size in bytes: " + file.length() + "\n");
		bw.write("\t" + "average size of one word in bytes: "
				+ file.length() / wordCount + "\n");
		bw.write("\t" + "unique words: " + uniqueWords
				+ "\n");
		Date date = new Date();
		bw.write("\t" + "date: " + date + "\n");
		bw.flush();
		bw.close();
	}
}
