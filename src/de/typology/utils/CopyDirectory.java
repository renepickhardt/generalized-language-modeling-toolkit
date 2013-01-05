package de.typology.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * thanks to
 * 
 * @author http://www.roseindia.net/java/example/java/io/CopyDirectory.shtml
 * 
 */
public class CopyDirectory {

	public CopyDirectory(String source, String destination) {
		File src = new File(source);
		File dst = new File(destination);
		try {
			this.copyDirectory(src, dst);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void copyDirectory(File srcPath, File dstPath) throws IOException {
		if (srcPath.isDirectory()) {
			if (!dstPath.exists()) {
				dstPath.mkdir();
			}

			String files[] = srcPath.list();
			for (String file : files) {
				this.copyDirectory(new File(srcPath, file), new File(dstPath,
						file));
			}
		} else {
			if (!srcPath.exists()) {
				System.out.println("File or directory does not exist.");
				return;
			} else {
				InputStream in = new FileInputStream(srcPath);
				OutputStream out = new FileOutputStream(dstPath);

				// Transfer bytes from in to out
				byte[] buf = new byte[1024];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
				in.close();
				out.close();
			}
		}
		// System.out.println("Directory copied.");
	}
}