package de.typology.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * some algorithms and tasks that I frequently need
 * 
 * @author rpickhardt
 * @param <T2>
 * @param <T1>
 * 
 */

public class Algo<Key, Value extends Comparable<? super Value>> {

	public Algo() {
	}

	// TODO: include mutli treemap http://sourceforge.net/projects/multitreemap/
	public TreeMap<Value, Set<Key>> getTopkElements(HashMap<Key, Value> map,
			int k) {
		TreeMap<Value, Set<Key>> tm = new TreeMap<Value, Set<Key>>();
		int cnt = 0;
		for (Entry<Key, Value> e : map.entrySet()) {
			if (cnt++ < k) {
				Value key = e.getValue();
				if (tm.containsKey(key)) {
					Set<Key> s = tm.get(key);
					s.add(e.getKey());
					tm.put(key, s);
				} else {
					Set<Key> s = new HashSet<Key>();
					s.add(e.getKey());
					tm.put(e.getValue(), s);
				}
			} else {
				Value key = tm.firstKey();
				if (key == null) {
					continue;
				}
				if (e.getValue() == null) {
					continue;
				}
				if (key.equals(e.getValue())) {
					continue;
				}
				if (tm.containsKey(e.getValue())) {
					Set<Key> s = tm.get(key);
					s.add(e.getKey());
					tm.put(key, s);
				} else {
					Set<Key> s = new HashSet<Key>();
					s.add(e.getKey());
					tm.put(e.getValue(), s);
				}
				if (tm.size() > k) {
					tm.remove(tm.firstKey());
				}

				// if (tm.comparator().compare(key, e.getValue())<0){
				// tm.remove(key);
				// }
			}
		}
		return tm;
	}
}