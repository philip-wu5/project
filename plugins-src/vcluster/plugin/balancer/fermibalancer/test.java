package vcluster.plugin.balancer.fermibalancer;

import java.util.Random;




public class test {
	public static void main(String[] args) throws Exception {
		int id=14;
		int hostNo=303;
		for(int i=0;i<11;i++){
			id=id+1;
			hostNo=hostNo+1;
			String hostname="fgitb"+hostNo+".fnal.gov";
			Host h=new Host("fermicloudpp",Integer.toString(id),hostname,9734,16331456,8);
			Algorithm.getPrivateHostList().add(h);
			h.dump();
		}
		
		Host h2=new Host("Amazon","1","aws.amazon.com",9734,0xffffffff,0xffffffff);
		Algorithm.getPublicHostList().add(h2);
		long currentTime=System.currentTimeMillis()/1000*1000;
		Random r=new Random();
		int t=0;
		long time=currentTime+(long)(t*1000);
		for(int i=0;i<80;i++){
			System.out.printf("Last Launch: %d CurrentTime: %d Slack: %d seconds\n", currentTime, time, t);
			currentTime=time;
			Algorithm.estimateDeployTest(currentTime);
			Algorithm.dump();
			t=r.nextInt(30);
			time=currentTime+(long)(t*1000);
			
		}
		System.out.printf("Average Launch Time: %.2f seconds\n", Algorithm.aveLaunchTime());
	}

	
}
