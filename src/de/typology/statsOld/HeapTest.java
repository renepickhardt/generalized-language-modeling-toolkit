package de.typology.statsOld;

public class HeapTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int mb = 1024 * 1024;

		// Getting the runtime reference from system
		Runtime runtime = Runtime.getRuntime();

		System.out.println("##### Heap utilization statistics [MB] #####");

		// Print used memory
		System.out.println("Used Memory:\t"
				+ (runtime.totalMemory() - runtime.freeMemory()) / mb);

		// Print free memory
		System.out.println("Free Memory:\t" + runtime.freeMemory() / mb);

		// Print total available memory
		System.out.println("Total Memory:\t" + runtime.totalMemory() / mb);

		// Print Maximum available memory
		System.out.println("Max Memory:\t" + runtime.maxMemory() / mb);
	}
}
