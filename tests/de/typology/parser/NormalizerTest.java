package de.typology.parser;

import java.util.Locale;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NormalizerTest {
	Normalizer normalizer;

	@Before
	public void setUp() throws Exception {
		this.normalizer = new Normalizer(Locale.ENGLISH);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testNormal1() {
		String input = "This is a T.L.A. test. Yet, another test.";
		String expected = "This is a T.L.A. test.\nYet, another test.\n";
		System.out.println(this.normalizer
				.splitSentences(input, Locale.ENGLISH));
		Assert.assertEquals(expected,
				this.normalizer.splitSentences(input, Locale.ENGLISH));
	}

}
