package de.typology.trainers;

/**
 * this piece of source code was created with kind help of
 * Shai Erera/Haifa/IBM and Yosi Mass/Haifa/IBM@IBMIL who pointed me to the FST implementation in Lucene
 * and sent me a small sample code that demonstrated how to use the API of FST which I extended to this small test of indexing the words of 500k wikipedia articles
 */

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRef;
import org.apache.lucene.util.fst.Builder;
import org.apache.lucene.util.fst.FST;
import org.apache.lucene.util.fst.FST.BytesReader;
import org.apache.lucene.util.fst.PositiveIntOutputs;
import org.apache.lucene.util.fst.Util;
import org.apache.lucene.util.fst.Util.MinResult;

public class FSTTest {

	public static class LongComparator implements Comparator<Long> {
		@Override
		public int compare(Long object1, Long object2) {
			return object1.compareTo(object2);
		}
	}

	public static BufferedReader openReadFile(String filename) {
		FileInputStream fstream;
		BufferedReader br = null;
		try {
			fstream = new FileInputStream(filename);
			DataInputStream in = new DataInputStream(fstream);
			br = new BufferedReader(new InputStreamReader(in));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return br;
	}

	public static FST<Long> buildFST() throws IOException {
		final PositiveIntOutputs outputs = PositiveIntOutputs
				.getSingleton(true);

		// Build an FST mapping BytesRef -> Long
		final Builder<Long> builder = new Builder(FST.INPUT_TYPE.BYTE1, outputs);

		// FST sorts from lowest to highest weight, so reverse the weight.
		// order of strings ae, ab, acd, abc, bef, agh, bfg, ch
		IntsRef scratch = new IntsRef();

		BufferedReader br = openReadFile("/var/lib/datasets/rawdata/wikipedia/normalized.txt");
		String line = "";
		HashMap<String, Long> map = new HashMap<String, Long>();
		int cnt = 1;
		while ((line = br.readLine()) != null) {
			String[] tokens = line.split("\\s");
			for (String token : tokens) {
				if (map.containsKey(token)) {
					map.put(token, map.get(token) + 1);
				} else {
					map.put(token, (long) 1);
				}
			}
			if (cnt++ % 1000 == 0) {
				System.out.println(cnt - 1 + "\t articles processed");
			}
			if (cnt > 500000) {
				break;
			}
		}
		System.out.println(map.size());

		TreeMap<String, Long> tm = new TreeMap<String, Long>();
		for (String key : map.keySet()) {
			tm.put(key, map.get(key));
		}

		for (String key : tm.keySet()) {
			builder.add(Util.toIntsRef(new BytesRef(key), scratch),
					Long.MAX_VALUE - tm.get(key));
		}
		return builder.finish();
	}

	public static void printStringsSorted(final FST<Long> fst)
			throws IOException {
		MinResult<Long>[] res = Util.shortestPaths(fst,
				fst.getFirstArc(new FST.Arc<Long>()), Long.valueOf(0),
				new LongComparator(), 10, true);
		BytesRef bytes = new BytesRef();
		for (MinResult<Long> r : res) {
			System.out.println(Util.toBytesRef(r.input, bytes).utf8ToString()
					+ "=" + (Long.MAX_VALUE - r.output));
		}
	}

	public static void printPrefixSorted(final FST<Long> fst,
			final BytesRef prefix, final int topN) throws IOException {
		final BytesReader fstReader = fst.getBytesReader();
		final FST.Arc<Long> arc = fst.getFirstArc(new FST.Arc<Long>());

		// Accumulate output as we go
		for (int i = 0; i < prefix.length; i++) {
			if (fst.findTargetArc(prefix.bytes[i + prefix.offset] & 0xFF, arc,
					arc, fstReader) == null) {
				return; // throw new RuntimeException("prefix not found");
			}
		}

		MinResult<Long>[] res = Util.shortestPaths(fst, arc, arc.output,
				new LongComparator(), topN, arc.isFinal());
		final BytesRef bytes = new BytesRef();
		for (MinResult<Long> r : res) {
			// System.out.println(prefix.utf8ToString()
			// + Util.toBytesRef(r.input, bytes).utf8ToString() + "="
			// + (Long.MAX_VALUE - r.output));
		}
	}

	public static void main(String[] args) throws Exception {
		final FST<Long> fst = buildFST();
		printStringsSorted(fst);
		System.out.println("start queries");
		for (int i = 0; i < 1000000; i++) {
			// System.out.println("------------");
			printPrefixSorted(fst, new BytesRef("a"), 10);
			// System.out.println("------------");
			printPrefixSorted(fst, new BytesRef("b"), 5);
			// System.out.println("------------");
			printPrefixSorted(fst, new BytesRef("c"), 5);
			// System.out.println("------------");
			printPrefixSorted(fst, new BytesRef("d"), 5);
			// System.out.println("------------");
			printPrefixSorted(fst, new BytesRef("e"), 5);
			// System.out.println("------------");
			printPrefixSorted(fst, new BytesRef("t"), 5);
			// System.out.println("------------");
			printPrefixSorted(fst, new BytesRef("prin"), 5);
			if (i % 9999 == 0) {
				System.out.println(i);
			}
		}
	}

}