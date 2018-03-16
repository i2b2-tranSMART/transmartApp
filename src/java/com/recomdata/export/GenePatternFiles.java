package com.recomdata.export;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;

/**
 * @author mmcduffie
 */
class GenePatternFiles {

	protected File clsfile;
	protected File gctfile;
	protected File csvfile;
	protected PrintStream clsPS;
	protected PrintStream gctPS;
	protected PrintStream csvPS;

	private static int FLUSH_COUNT = 100;
	private int gctFlushCount = 0;
	private int csvFlushCount = 0;

	public GenePatternFiles() throws IOException {
		// put files in a directory
		File tmpdir = new File(System.getProperty("java.io.tmpdir"), "datasetexplorer");
		if (!tmpdir.exists()) {
			tmpdir.mkdir();
		}

		clsfile = File.createTempFile("gp_df_", ".cls", tmpdir);
		gctfile = File.createTempFile("gp_df_", ".gct", tmpdir);
		csvfile = File.createTempFile("gp_df_exp_", ".csv", tmpdir);
	}

	public void writeClsFile(String subjectIds1, String subjectIds2) throws IOException {
		FileOutputStream fos = new FileOutputStream(clsfile);
		PrintStream ps = new PrintStream(fos);

		if (subjectIds1 != null && subjectIds2 != null) {
			StringTokenizer st1 = new StringTokenizer(subjectIds1, ",");
			StringTokenizer st2 = new StringTokenizer(subjectIds2, ",");

			Integer count1 = st1.countTokens();
			Integer count2 = st2.countTokens();
			Integer total = count1 + count2;

			ps.println(total + " 2 1");
			ps.println("# S1 S2");
			while (st1.hasMoreTokens()) {
				String id = st1.nextToken();
				ps.print("0 ");
			}
			while (st2.hasMoreTokens()) {
				String id = st2.nextToken();
				ps.print("1 ");
			}
		}
		else {
			StringTokenizer st;
			if (subjectIds1 != null) {
				st = new StringTokenizer(subjectIds1, ",");
			}
			else {
				st = new StringTokenizer(subjectIds2, ",");
			}

			int count = st.countTokens();

			ps.println(count + " 1 1");
			ps.println("# subset1");
			while (st.hasMoreTokens()) {
				String id = st.nextToken();
				ps.print("0 ");
			}
		}

		ps.print("\n");

		fos.close();
	}

	public void writeGctFile(HeatMapTable table, Boolean addMeans) throws IOException {
		FileOutputStream fos = new FileOutputStream(gctfile);
		PrintStream ps = new PrintStream(fos, true);
		table.writeToFile("\t", ps, addMeans);
		fos.flush();
		fos.close();
	}

	public void writeGctFile(HeatMapTable table) throws IOException {
		FileOutputStream fos = new FileOutputStream(gctfile);
		PrintStream ps = new PrintStream(fos, true);
		table.writeToFile("\t", ps);
		fos.flush();
		fos.close();
	}

	public void openGctFile() throws IOException {
		OutputStream fos = new BufferedOutputStream(new FileOutputStream(gctfile));
		gctPS = new PrintStream(fos, true);
	}

	public void createGctHeader(Integer rows, String[] ids, String delimiter) {
		gctPS.println("#1.2");

		gctPS.println(rows + delimiter + ids.length);
		gctPS.print("NAME" + delimiter + "Description");

		for (String id : ids) {
			gctPS.print(delimiter + id);
		}

		gctPS.print("\n");
	}

	public void writeToGctFile(String value) throws IOException {
		gctPS.println(value);
		gctFlushCount++;
		if (gctFlushCount > FLUSH_COUNT) {
			gctPS.flush();
			gctFlushCount = 0;
		}
	}

	public void closeGctFile() throws IOException {
		gctPS.close();
	}

	public void openCSVFile() throws IOException {
		OutputStream fos = new BufferedOutputStream(new FileOutputStream(csvfile));
		csvPS = new PrintStream(fos, true);
	}

	public void createCSVHeader(String[] ids, String delimiter) throws IOException {
		csvPS.print("NAME" + delimiter + "Description");
		for (String id : ids) {
			csvPS.print(delimiter + id);
		}
		csvPS.print("\n");
	}

	public void writeToCSVFile(String value) throws IOException {
		csvPS.println(value);
		csvFlushCount++;
		if (csvFlushCount > FLUSH_COUNT) {
			csvPS.flush();
			csvFlushCount = 0;
		}
	}

	public void writeToCSVFile(String[] ids, String delimiter, String value) throws IOException {
		csvPS.print("NAME" + delimiter + "Description");
		for (String id : ids) {
			csvPS.print(delimiter + id);
		}
		csvPS.print("\n");
		csvPS.println(value);
		csvFlushCount++;
		if (csvFlushCount > FLUSH_COUNT) {
			csvPS.flush();
			csvFlushCount = 0;
		}
	}


	public void closeCSVFile() throws IOException {
		csvPS.flush();
		csvPS.close();
	}

	public Boolean openClsFile() throws IOException {
		FileOutputStream fos = new FileOutputStream(clsfile);
		clsPS = new PrintStream(fos);
		return true;
	}

	public void writeToClsFile(String value) throws IOException {
		clsPS.println(value);
	}

	public void closeClsFile() throws IOException {
		clsPS.flush();
		clsPS.close();
	}

	public File clsFile() {
		return clsfile;
	}

	public File gctFile() {
		return gctfile;
	}

	public String getCSVFileName() {
		return csvfile.getName();
	}
}
