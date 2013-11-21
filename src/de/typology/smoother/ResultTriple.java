package de.typology.smoother;

public class ResultTriple {

	private double smoothedValue;
	private double backOffWeight;
	private double backOffResult;

	public ResultTriple() {
		this.smoothedValue = 0;
		this.backOffWeight = 0;
		this.backOffResult = 0;
	}

	public double getSmoothedValue() {
		return this.smoothedValue;
	}

	public void setSmoothedValue(double smoothedValue) {
		this.smoothedValue = smoothedValue;
	}

	public double getBackOffWeight() {
		return this.backOffWeight;
	}

	public void setBackOffWeight(double backOffWeight) {
		this.backOffWeight = backOffWeight;
	}

	public double getBackOffResult() {
		return this.backOffResult;
	}

	public void setBackOffResult(double backOffResult) {
		this.backOffResult = backOffResult;
	}

	public void addToBackOffResult(double backOffResult) {
		this.backOffResult += backOffResult;
	}

}
