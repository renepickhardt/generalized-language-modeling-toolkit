package de.typology.predictors;

import java.util.Scanner;

import de.typology.utils.Config;

public class TypologyPredictorMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TypologyPredictor tp = new TypologyPredictor(Config.get().dbPath);

		Scanner sc = new Scanner(System.in);
		while (true) {
			System.out.println("please insert four words");
			String input = sc.nextLine();
			String[] inputSplit = input.split("\\s");
			if (inputSplit.length != 4) {
				if (inputSplit.length == 1 && inputSplit[0].equals("exit")) {
					System.out.println("exiting");
					break;
				}
				System.out.println("four words...");
				continue;
			}
			long startTime = System.currentTimeMillis();
			String[] result = tp.predict(inputSplit);
			long endTime = System.currentTimeMillis();
			System.out.println("time: " + (endTime - startTime) + " ms");
			System.out.print("result: ");
			for (String s : result) {
				System.out.print(s + " ");
			}
			System.out.println();
			System.out.println();
		}
		sc.close();
		tp.shutdown();
	}

}
