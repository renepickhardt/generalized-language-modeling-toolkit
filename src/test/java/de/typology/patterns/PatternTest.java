package de.typology.patterns;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class PatternTest {

	@Test
	public void test() {
		System.out.println("GLM patterns: ");
		//TODO: on the long term I also expect a different output here
		for (Pattern pat: Pattern.getGlmForSmoothingPatterns(3)){
			System.out.println(pat);
		}
		System.out.println("LM patterns: ");
		for (Pattern pat: Pattern.getReverseLmPatterns(3)){
			System.out.println(pat);
		}
		System.out.println("missing POS pattern");
		
		System.out.println("missing SKP pattern");
		
		List<PatternType> pattern = new ArrayList<PatternType>();
		pattern.add(PatternType.POS);
		pattern.add(PatternType.DEL);
		pattern.add(PatternType.SKP);
		pattern.add(PatternType.CNT);
		Pattern p = new Pattern(pattern);
		//TODO: I also don't expect 
		assertTrue(p.numCnt()==1);
		System.out.println(p.toString());
		//TODO: I don't understand this method (I would expect that we need without Del and not without Skp)
		p = Pattern.newWithoutSkp(p);
		System.out.println(p.toString());
		
	}

}
