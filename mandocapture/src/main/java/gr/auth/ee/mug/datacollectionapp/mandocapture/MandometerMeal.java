package gr.auth.ee.mug.datacollectionapp.mandocapture;

public class MandometerMeal {
	
	private class MandometerMealIndicators {
		
		private double fs;
		private int[] FoodIntakeCurve;
		
	}

	private int[] measurements;
	private int p;
	
	
	public MandometerMeal() {
		measurements = new int[MAX_MEASUREMENTS];
		p = 0;
	}
	
	public void appendMeasurement(int w) {
		measurements[p] = w;
		p++;
	}
	
	public void makeCorrectedCurve() {
		
	}
	
	
	
	
	private final static int MAX_MEASUREMENTS = 2*60*60;
}
