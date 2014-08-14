package vcluster.plugin.balancer.fermibalancer;

import java.util.ArrayList;
import java.util.Random;


public class test {
	public static void main(String[] args) throws Exception {
		//testMASE();
		//Thread serverThread = new Thread(new InfoCollector(9738));
        //serverThread.start();
        //InfoCollector infoc=new InfoCollector(9738);
        //infoc.run();
		int id=14;
		int hostNo=303;
		for(int i=0;i<10;i++){
			id=id+1;
			hostNo=hostNo+1;
			String hostname="fgitb"+hostNo+".fnal.gov";
			Host h=new Host("fermicloudpp",Integer.toString(id),hostname,9734,16331456,8);
			Algorithm.getPrivateHostList().add(h);
			h.dump();
		}
		
		//Host h2=new Host("Amazon","1","aws.amazon.com",9734,0xffffffff,0xffffffff);
		//Algorithm.getPublicHostList().add(h2);
		long currentTime=System.currentTimeMillis()/1000*1000;
		Random r=new Random();
		int t=0;
		long time=currentTime+(long)(t*1000);
		for(int i=0;i<70;i++){
			//System.out.printf("Last Launch: %d CurrentTime: %d Slack: %d seconds\n", currentTime, time, t);
			//currentTime=time;
			//Algorithm.estimateDeployTest(currentTime);
			//Algorithm.dump();
			//t=r.nextInt(30);
			//time=currentTime+(long)(t*1000);
			
			//long currentTime=System.currentTimeMillis();
        	VM v=new VM();
        	v.setStartTime(currentTime);
        	v.setWaitTime(0);
        	Algorithm.getVmWaitQueue().add(v);
        	Host h=Algorithm.BestFitHost(v,false);
            //Host h=Algorithm.estimateDeploy(v,false);
        	//String str=CloudManager.getCurrentCloud().createVMonHost(1, h.getId());
           // String str=CloudManager.getCurrentCloud().createVM(1, h.getAddress());
        	//String [] sub=str.split("\\s+");
            
            v.setId(Integer.toString(i+1));
            v.setDnsName(Integer.toString(i+1));
            //v.setStartTime(time);
            v.setHostId(h.getId());
            v.setHostName(h.getAddress());
            //h.addVM(v);
           
//            int op=r.nextInt(100);
//            int real=v.getBootTime();
//            if(op<50)
//            	real=real-r.nextInt(30);
//            else
//            	real=real+r.nextInt(100);
//			v.setRealBootTime(real);
//			
//			h.trainning(true);
		}
		Algorithm.dump();
		System.out.printf("Average Launch Time: %.2f seconds\n", Algorithm.aveLaunchTime());
	
	}
	public static void testMASE(){
		ArrayList<Double> actual=new ArrayList<Double>();
		ArrayList<Double> predict=new ArrayList<Double>();
		Random r=new Random(System.currentTimeMillis());
		for(int i=0;i<100;i++){
			double a=(double)r.nextInt(100);
			//double f=(double)r.nextInt(100);
			double f=a+5;
			String sa=String.format("%-3.3f", a);
			String sf=String.format("%-3.3f", f);
			System.out.println(sa+"  "+sf+System.lineSeparator());
			actual.add(a);
			predict.add(f);
		}
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
		System.out.println(Double.toString(sum/100));
		
	}
}
