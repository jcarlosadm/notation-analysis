package notationAnalysis.util.jplag;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;

/**
 * Using Jplag for Similarity.
 * 
 * @author Luiz Carvalho / IC/UFAL
 *
 */
public class CompareJplag {

	private final String _JPLAGPATH = "jplag";
	private final String _JPLAGJARNAME = "jplag-2.11.8-SNAPSHOT-jar-with-dependencies.jar";
	private final String _JPLAGRUNNAME = "runJplag.sh";
	
	private static CompareJplag instance = null;
	
	private CompareJplag() {
	}
	
	public static CompareJplag getInstance () {
		if (instance == null) {
			instance = new CompareJplag();
		}
		
		return instance;
	}

	/**
	 * Similarity for two strings using Jplag
	 * 
	 * @param str1
	 * @param str2
	 * @return Between 0.0 and 1.0. Otherwise -1.0 in case of error.
	 */
	public double compareTo(String str1, String str2) {
		try {
			String semiPath = _JPLAGPATH + File.separator + "codes";
			File folder = new File(semiPath);
			if (folder.isDirectory() && folder.exists()) {
				this.deleteFolder(folder);
			}
			if (!folder.mkdir()) {
				return -1;
			}

			Writer writer1 = new BufferedWriter(new FileWriter(new File(semiPath + File.separator + "code1.c")));
			Writer writer2 = new BufferedWriter(new FileWriter(new File(semiPath + File.separator + "code2.c")));
			writer1.write(str1);
			writer2.write(str2);
			writer1.close();
			writer2.close();

			double result = run(semiPath);
			this.deleteFolder(folder);

			return result;
		} catch (Exception ioe) {
			System.err.println("Exception in compareTo with Jplag");
			ioe.printStackTrace();
			return -1.0; // Flag for problems
		}
	}

	private void deleteFolder(File folder) {
		File[] files = folder.listFiles();
		if (files != null) {
			for (File f : files) {
				if (f.isDirectory()) {
					this.deleteFolder(f);
				} else {
					f.delete();
				}
			}
		}
		folder.delete();
	}

	/**
	 * Run similarity for two strings using Jplag.
	 * 
	 * @param str1
	 * @param str2
	 * @return Normalized between 0.0 and 1.0. Otherwise -1.0 in case of error.
	 */
	private double run(String pathFolder) {
		final String command = this._JPLAGPATH + File.separator + this._JPLAGRUNNAME + " " + _JPLAGPATH + File.separator
				+ _JPLAGJARNAME + " " + pathFolder;
		final int indexValue = 1;
		final float normalize = 100;
		String result = null;
		String results[];
		String strValue;
		float value;
		BufferedReader br = null;

		try {
			Runtime rt = Runtime.getRuntime();
			Process process = rt.exec(command);
			final InputStream is = process.getInputStream();
			final InputStreamReader isr = new InputStreamReader(is);
			br = new BufferedReader(isr);

			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("Comparing")) {
					result = line;
					break;
				}
			}
			if (result == null) {
			    System.err.println("Erro in Comparing with JPlag.");
			    return -1.0; //Flag for problems
			} else {
			    results = result.split(":");
			    strValue = results[indexValue];
			    value = Float.parseFloat(strValue) / normalize;
			    return value;
			}
		} catch (IOException ioe) {
			System.err.println("IOException when comparing codes");
			ioe.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				System.err.println("IOException when comparing codes and close BufferedReader");
			}
		}
		return -1.0; // Flag for problems
	}

}
