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
		// String[] s = { "This", "is", "a", "test" };
		// DDR als untrennbarer Bestandteil der
		String[] s = { "Das", "ist", "ein", "Test" };
		long start_time = System.nanoTime();
		String[] result = tp.predict(s);
		for (String element : result) {
			System.out.println(element);
		}
		long end_time = System.nanoTime();
		System.out.println("time: "
				+ Math.round((double) (end_time - start_time) / 1000) / 1000
				+ " ms");

	}
}
