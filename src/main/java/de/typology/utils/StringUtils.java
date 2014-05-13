package de.typology.utils;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {

	/**
	 * this method should be used instead of string.split(' ') since it is much more performant
	 */
    public static List<String> splitAtSpace(String s) {
        List<String> result = new ArrayList<String>();

        int sp1 = 0, sp2;
        while (true) {
            sp2 = s.indexOf(' ', sp1);

            if (sp2 == -1) {
                String substr = s.substring(sp1);
                if (!substr.isEmpty()) {
                    result.add(substr);
                }
                break;
            } else {
                String substr = s.substring(sp1, sp2);
                if (!substr.isEmpty()) {
                    result.add(substr);
                }
                sp1 = sp2 + 1;
            }
        }

        return result;
    }

    public static String join(List<String> strings, String conjunction) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean first = true;
        for (String string : strings) {
            if (first) {
                first = false;
            } else {
                stringBuilder.append(conjunction);
            }
            stringBuilder.append(string);
        }
        return stringBuilder.toString();
    }

}
