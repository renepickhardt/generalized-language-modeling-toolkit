package de.typology.predictors;

import org.junit.BeforeClass;
import org.junit.Test;

import de.typology.utils.Config;

public class TypologyPredictorTest {
	static TypologyPredictor tp;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		tp = new TypologyPredictor(Config.get().testDb);
	}

	@Test
	public void test() {
		System.out.println(tp.buildIndex());
		String[] s = { "This", "is", "a", "test" };
		tp.predict(s);
		// System.out.println(tp.predict(s));
	}

}
