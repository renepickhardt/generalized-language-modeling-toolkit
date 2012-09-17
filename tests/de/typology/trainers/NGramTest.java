package de.typology.trainers;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * @author Martin Koerner
 * 
 */
public class NGramTest {

	@Test
	public void testNGramEdgeTypesNormal() {
		String[] s1 = { "this", "is", "a", "test" };
		NGram n1 = new NGram(s1, 42);
		List<Pair> e1 = new ArrayList<Pair>();
		e1.add(new Pair("this", "is"));
		e1.add(new Pair("is", "a"));
		e1.add(new Pair("a", "test"));
		List<Pair> a1 = n1.getPairsWithEdgeType(1);
		assertEquals(e1, a1);

		List<Pair> e2 = new ArrayList<Pair>();
		e2.add(new Pair("this", "a"));
		e2.add(new Pair("is", "test"));
		List<Pair> a2 = n1.getPairsWithEdgeType(2);
		assertEquals(e2, a2);

		List<Pair> e3 = new ArrayList<Pair>();
		e3.add(new Pair("this", "test"));
		List<Pair> a3 = n1.getPairsWithEdgeType(3);
		assertEquals(e3, a3);

		List<Pair> e4 = new ArrayList<Pair>();
		List<Pair> a4 = n1.getPairsWithEdgeType(4);
		assertEquals(e4, a4);

	}

	@Test
	public void testNGramEdgeTypesBorder() {
		String[] s1 = { "" };
		NGram n1 = new NGram(s1, 42);
		List<Pair> e1 = new ArrayList<Pair>();
		List<Pair> a1 = n1.getPairsWithEdgeType(1);
		assertEquals(e1, a1);
		String[] s2 = {};
		NGram n2 = new NGram(s2, 42);
		List<Pair> e2 = new ArrayList<Pair>();
		List<Pair> a2 = n2.getPairsWithEdgeType(1);
		assertEquals(e2, a2);
	}
}
