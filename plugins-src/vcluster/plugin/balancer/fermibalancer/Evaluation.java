package vcluster.plugin.balancer.fermibalancer;

import java.util.ArrayList;


public class Evaluation {

	/**
	 * Mean Absolute Scaled Error
	 * @param actual
	 * @param predict
	 * @return
	 */
	public static double MASE(ArrayList<Double> actual, ArrayList<Double> predict){
		double sum=0;
		for(int i=1;i<actual.size();i++){
			double e=(actual.get(i)-predict.get(i));
			double y=0;
			for(int j=i;j>0;j--){
				y=y+Math.abs(predict.get(j)-predict.get(j-1));
			}
			double temp=1.00/(100.00-1.00)*y;
			sum=sum+Math.abs(e/temp);
		}
		return sum/actual.size();
	}
	
	/**
	 * Mean Square Error
	 * @param actual
	 * @param predict
	 * @return
	 */
	public static double MSE(ArrayList<Double> actual, ArrayList<Double> predict){
		double sum=0;
		for(int i=0;i<actual.size();i++){
			double e=actual.get(i)-predict.get(i);
			sum=sum+Math.pow(e, 2);
			
		}
		return sum/actual.size();
	}
	/**
	 * Mean Absolute Percentage Error
	 * @param actual
	 * @param predict
	 * @return
	 */
	public static double MAPE(ArrayList<Double> actual, ArrayList<Double> predict){
		double sum=0;
		for(int i=0;i<actual.size();i++){
			double e=actual.get(i)-predict.get(i);
			sum=sum+Math.abs(e/actual.get(i))*100;
		}
		return sum/actual.size();
	}
	/**
	 * Root Mean Square Percentage Error
	 * @param actual
	 * @param predict
	 * @return
	 */
	public static double EMSPE(ArrayList<Double> actual, ArrayList<Double> predict){
		double sum=0;
		for(int i=0;i<actual.size();i++){
			double e=actual.get(i)-predict.get(i);
			sum=sum+Math.pow(e/actual.get(i)*100,2);
		}
		return Math.sqrt(sum/actual.size());
	}
}
