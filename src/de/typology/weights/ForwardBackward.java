package de.typology.weights;

import java.util.ArrayList;

public class ForwardBackward {

	private ArrayList<double[]> emissionProbabilities = new ArrayList<double[]>();
	
	private double[] transitionProbabilities = null;
	
	private int maxProbs = 1000000;
	
	private final double smoothing = 0.0000;
	private final double preSmoothing = 0.001;
	
	public ForwardBackward(LogReader reader, int prefixLength) {
		this.transitionProbabilities = new double[reader.modelNumber()];
		for (int i = 0; i < this.transitionProbabilities.length; i++) {
			this.transitionProbabilities[i] = 1f/this.transitionProbabilities.length;
		}
		while (this.emissionProbabilities.size() < this.maxProbs) {
			double[][] entry = reader.getNextProbabilities();
			if (entry == null) {
				break;
			} else {
				if (entry.length > prefixLength) {
				// calculate prob sum
				double sum = 0;
				for (int j = 0; j < entry[prefixLength].length; j++) {
					entry[prefixLength][j] += this.preSmoothing;
					sum += entry[prefixLength][j];
				}
				// normalize
				for (int j = 0; j <entry[prefixLength].length; j++) {
					entry[prefixLength][j] /= sum;
				}

				this.emissionProbabilities.add(entry[prefixLength]);
				}
			}
		}
	}
	
	public double[] computeMarginalDistribution() {
		double[] result  = new double[this.transitionProbabilities.length];

		// forward steps
		double[][] fwd = new double[this.emissionProbabilities.size()+1][this.transitionProbabilities.length];
		double[] sums = new double[fwd.length];
		fwd[0] = this.transitionProbabilities;
		
		for (int i = 0; i < this.emissionProbabilities.size(); i++) {
			double[] p = this.emissionProbabilities.get(i);
			for (int j = 0; j < fwd[i+1].length; j++) {
				fwd[i+1][j] = 0; 
				for (int k = 0; k < fwd[i].length; k++) {
					fwd[i+1][j] += fwd[i][k] * (this.transitionProbabilities[j]) * (p[j]+this.smoothing);
				}
			}			
			// calculate prob sum
			double sum = 0;
			for (int j = 0; j < fwd[i+1].length; j++) {
				sum += fwd[i+1][j];
			}
			// normalize
			for (int j = 0; j < fwd[i+1].length; j++) {
				fwd[i+1][j] /= sum;
			}
			sums[i+1] = sum;
		}
		
		// backward step
		double[][] bwd = new double[this.emissionProbabilities.size()+1][this.transitionProbabilities.length];
		
		bwd[this.emissionProbabilities.size()] = this.emissionProbabilities.get(this.emissionProbabilities.size()-1);
		
		for (int i = this.emissionProbabilities.size()-1; i >= 0; i--) {
			double[] p = this.emissionProbabilities.get(i);
			for (int j = 0; j < bwd[i].length; j++) {
				bwd[i][j] = 0; 
				for (int k = 0; k < bwd[i+1].length; k++) {
					bwd[i][j] += (bwd[i+1][k]/sums[i+1]) * (this.transitionProbabilities[j]) * (p[j]+this.smoothing);
				}
			}			
//			// calculate prob sum
//			double sum = 0;
//			for (int j = 0; j < bwd[i].length; j++) {
//				sum += bwd[i][j];
//			}
//			// normalize
//			for (int j = 0; j < bwd[i].length; j++) {
//				bwd[i][j] /= sum;
//			}
		}
		
		// compute the probability of being in state at given time and the marginal distribution of these probabilities
		double[][] gamma = new double[fwd.length][this.transitionProbabilities.length];
		
		for (int i = 0; i < fwd.length; i++) {
			for (int j = 0; j < fwd[i].length; j++) {
				gamma[i][j] = fwd[i][j] * bwd[i][j];
				result[j] += gamma[i][j];
			}
		}
		// normalize
		double sum = 0;
		for (int j = 0; j <result.length; j++) {
			sum += result[j];
		}
		for (int j = 0; j <result.length; j++) {
			result[j] /= sum;
		}
		
		return result;
	}
	
}
