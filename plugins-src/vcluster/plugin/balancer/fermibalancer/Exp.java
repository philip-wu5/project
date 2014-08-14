package vcluster.plugin.balancer.fermibalancer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class Exp {

	public static final String fileName="LaunchPattern"; 
	
	private static ArrayList<Integer> pattern;
	
	/**
	 * Randomly generate VM launch pattern
	 * @param n 
	 * @param maxInterval
	 */
	public static void randomPattern(int n, int maxInterval){
		Random r=new Random(System.currentTimeMillis());
		pattern=new ArrayList<Integer>();
		for(int i=0;i<n;i++){
			pattern.add(r.nextInt(maxInterval));
		}
	}
	
	public static void writePattern(){
		File f=new File(fileName);
		try {
			StringBuilder sb=new StringBuilder();
			for(int i=0;i<pattern.size();i++){
				if(i==pattern.size()-1)
					sb.append(Integer.toString(pattern.get(i)));
				else
					sb.append(Integer.toString(pattern.get(i))+",");
			}
			FileWriter fw=new FileWriter(f,false);
			fw.write(sb.toString());
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static ArrayList<Integer> readPattern(){
		File f=new File(fileName);
		if(!f.exists())
		{	
			return null;
		}
    	
    	BufferedReader br ;
		try{
			br = new BufferedReader(new FileReader(f));
			String aLine = "";
			pattern=new ArrayList<Integer> ();
			while ((aLine = br.readLine()) != null) {
				String [] sub=aLine.split(",");
				for(int i=0;i<sub.length;i++){
					pattern.add(Integer.parseInt(sub[i]));
				}
			}
			
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e){
			e.printStackTrace();
		}
		return pattern;
	}
	
	public static  ArrayList<Integer> getPattern() {
		return pattern;
	}

	public static void setPattern(ArrayList<Integer> value) {
		pattern = value;
	}
	
}
