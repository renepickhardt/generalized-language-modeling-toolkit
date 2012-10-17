package de.typology.stats;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import de.typology.utils.Config;
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

		BufferedWriter bw = IOHelper
				.openWriteFile(Config.get().nGramsNotAggregatedPath
						+ "/distribution" + fileExtension);
		try {
			for (int i = n - 1; i > 0; i--) {
				if (count[i] > 0) {
					bw.write(i + "\t" + count[i] + "\n");
					// System.out.println(count[i] + " ngrams " + i + " times");
				}
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
}
