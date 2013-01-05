package de.typology.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileSystemSorter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		FileSystemSorter fss = new FileSystemSorter();
		String input = "/var/lib/datasets/out/wikipedia/typoEdgesDENormalizedGer7095";

		for (int n = 1; n < 5; n++) {
			fss.sort(input + "/" + n, "." + n + "es");
		}

	}

	// sort comand if no hash is available!

	public void sort(String sourcePath, String fileExtension) {
		File[] files = IOHelper.getAllFilesInDirWithExtension(sourcePath,
				fileExtension, this.getClass().getName() + "aggregateNGrams");

		for (File f : files) {
			String fileName = f.getName();
			if (!fileName.endsWith(fileExtension)) {
				IOHelper.log(fileName
						+ " is not an aggregated ngram file. process next");
				continue;
			}

			String fullQualifiedFileName = sourcePath + File.separator
					+ fileName;

			IOHelper.log("start sorting " + fullQualifiedFileName);
			String aggregatedFileExtension = fileExtension.replace("s", "i");

			BufferedReader bcr = IOHelper.openReadFile(fullQualifiedFileName,
					Config.get().memoryLimitForWritingFiles);
			BufferedWriter bw = IOHelper.openWriteFile(
					sourcePath + "/tmp.file",
					Config.get().memoryLimitForWritingFiles);
			String l = "";
			try {
				while ((l = bcr.readLine()) != null) {
					String[] val = l.split("#");
					if (val.length != 2) {
						continue;
					}
					bw.write(val[0] + val[1] + "\n");
				}
				bw.close();
				bcr.close();

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			String outFileName = fullQualifiedFileName.replaceFirst(
					fileExtension, aggregatedFileExtension);

			String sortCommand = "LC_COLLATE=C sort -t$\'t\' -k1 -k3gr -S3G \""
					+ sourcePath + "/tmp.file\" --output=\"" + outFileName
					+ "\"";

			// String sortCommand = "sort -r -t\'#\' -k 2 -n -S3G \""
			// + fullQualifiedFileName + "\" --output=\"" + outFileName
			// + "\"";
			Runtime rt = Runtime.getRuntime();
			Process p = null;
			try {
				p = rt.exec(new String[] { "bash", "-c", sortCommand });
				System.out.println(sortCommand);
			} catch (IOException ioe) {
				IOHelper.log("Error executing: " + sortCommand);
			}
			InputStream output = p.getInputStream();

			BufferedReader br = new BufferedReader(
					new InputStreamReader(output));
			String line = "";
			try {
				// File f1 = new File(sourcePath + "/tmp.file");
				// f1.delete();
				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// SystemHelper.runUnixCommand(sortCommand);
			// TODO: comment out to delete aggregated files after sorting
		}
	}
}
