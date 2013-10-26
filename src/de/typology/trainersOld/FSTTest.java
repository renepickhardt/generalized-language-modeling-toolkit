package de.typology.trainersOld;

/**
 * this piece of source code was created with kind help of
 * Shai Erera/Haifa/IBM and Yosi Mass/Haifa/IBM@IBMIL who pointed me to the FST implementation in Lucene
 * and sent me a small sample code that demonstrated how to use the API of FST which I extended to this small test of indexing the words of 500k wikipedia articles
 */

//import java.io.BufferedReader;
//import java.io.DataInputStream;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.util.Comparator;
//
//import org.apache.lucene.util.BytesRef;
//import org.apache.lucene.util.IntsRef;
//import org.apache.lucene.util.fst.Builder;
//import org.apache.lucene.util.fst.FST;
//import org.apache.lucene.util.fst.FST.BytesReader;
//import org.apache.lucene.util.fst.PositiveIntOutputs;
//import org.apache.lucene.util.fst.Util;
//import org.apache.lucene.util.fst.Util.MinResult;
//
//import de.typology.utils.IOHelper;
//
//public class FSTTest {
//
//	public static class LongComparator implements Comparator<Long> {
//		@Override
//		public int compare(Long object1, Long object2) {
//			return object1.compareTo(object2);
//		}
//	}
//
//	public static BufferedReader openReadFile(String filename) {
//		FileInputStream fstream;
//		BufferedReader br = null;
//		try {
//			fstream = new FileInputStream(filename);
//			DataInputStream in = new DataInputStream(fstream);
//			br = new BufferedReader(new InputStreamReader(in));
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//			return null;
//		}
//		return br;
//	}
//
//	public static FST<Long> buildFST() throws IOException {
//		final PositiveIntOutputs outputs = PositiveIntOutputs
//				.getSingleton(true);
//
//		// Build an FST mapping BytesRef -> Long
//		final Builder<Long> builder = new Builder(FST.INPUT_TYPE.BYTE1, outputs);
//
//		// FST sorts from lowest to highest weight, so reverse the weight.
//		// order of strings ae, ab, acd, abc, bef, agh, bfg, ch
//		IntsRef scratch = new IntsRef();
//
//		long cnt = 0;
//		int ecnt = 0;
//		String line = "";
//		String key = "";
//		for (int i = 1; i < 700; i++) {
//			BufferedReader br = openReadFile("/var/lib/datasets/5grams/" + i
//					+ ".5gs");
//			while ((line = br.readLine()) != null) {
//				try {
//					String[] tokens = line.split("\\s");
//					if (tokens.length < 6) {
//						continue;
//					}
//					key = tokens[0] + " " + tokens[1] + " " + tokens[2] + " "
//							+ tokens[3] + " " + tokens[4];
//					Long value = (long) (Float.parseFloat(tokens[5]) * 10000000);
//					builder.add(Util.toIntsRef(new BytesRef(key), scratch),
//							Long.MAX_VALUE - value);
//					cnt++;
//					if (cnt % 500000 == 0) {
//						IOHelper.log(cnt + " terms added to a trie");
//						IOHelper.log("\t" + Runtime.getRuntime().freeMemory()
//								+ " free memory before running GC");
//						Runtime.getRuntime().gc();
//						IOHelper.log("\t" + Runtime.getRuntime().freeMemory()
//								+ " free memory after running GC");
//					}
//				} catch (UnsupportedOperationException e) {
//					IOHelper.log("trying to add: " + line
//							+ " as normalized token:" + key);
//					IOHelper.log(cnt
//							+ " items added to trie and crashed with following stack trace:");
//					e.printStackTrace();
//					continue;
//				} catch (Exception e) {
//					IOHelper.log("trying to add: " + line
//							+ " as normalized token:" + key);
//					IOHelper.log(cnt
//							+ " items added to trie and crashed with following stack trace:");
//					e.printStackTrace();
//					ecnt++;
//					if (ecnt > 10) {
//						i = 1000;
//						break;
//					}
//				}
//
//			}
//			IOHelper.log(i + " files processed and indexed to a trie");
//		}
//		return builder.finish();
//	}
//
//	public static void printStringsSorted(final FST<Long> fst)
//			throws IOException {
//		MinResult<Long>[] res = Util.shortestPaths(fst,
//				fst.getFirstArc(new FST.Arc<Long>()), Long.valueOf(0),
//				new LongComparator(), 10, true);
//		BytesRef bytes = new BytesRef();
//		for (MinResult<Long> r : res) {
//			System.out.println(Util.toBytesRef(r.input, bytes).utf8ToString()
//					+ "=" + (Long.MAX_VALUE - r.output));
//		}
//	}
//
//	public static void printPrefixSorted(final FST<Long> fst,
//			final BytesRef prefix, final int topN) throws IOException {
//		final BytesReader fstReader = fst.getBytesReader();
//		final FST.Arc<Long> arc = fst.getFirstArc(new FST.Arc<Long>());
//
//		// Accumulate output as we go
//		for (int i = 0; i < prefix.length; i++) {
//			if (fst.findTargetArc(prefix.bytes[i + prefix.offset] & 0xFF, arc,
//					arc, fstReader) == null) {
//				return; // throw new RuntimeException("prefix not found");
//			}
//		}
//
//		MinResult<Long>[] res = Util.shortestPaths(fst, arc, arc.output,
//				new LongComparator(), topN, arc.isFinal());
//		final BytesRef bytes = new BytesRef();
//		for (MinResult<Long> r : res) {
//			// System.out.println(prefix.utf8ToString()
//			// + Util.toBytesRef(r.input, bytes).utf8ToString() + "="
//			// + (Long.MAX_VALUE - r.output));
//		}
//	}
//
//	public static void main(String[] args) throws Exception {
//		final FST<Long> fst = buildFST();
//		printStringsSorted(fst);
//		System.out.println("start queries");
//		for (int i = 0; i < 1000000; i++) {
//			// System.out.println("------------");
//			printPrefixSorted(fst, new BytesRef("a"), 10);
//			// System.out.println("------------");
//			printPrefixSorted(fst, new BytesRef("b"), 5);
//			// System.out.println("------------");
//			printPrefixSorted(fst, new BytesRef("c"), 5);
//			// System.out.println("------------");
//			printPrefixSorted(fst, new BytesRef("d"), 5);
//			// System.out.println("------------");
//			printPrefixSorted(fst, new BytesRef("e"), 5);
//			// System.out.println("------------");
//			printPrefixSorted(fst, new BytesRef("t"), 5);
//			// System.out.println("------------");
//			printPrefixSorted(fst, new BytesRef("prin"), 5);
//			if (i % 9999 == 0) {
//				System.out.println(i);
//			}
//		}
//	}
//
// }