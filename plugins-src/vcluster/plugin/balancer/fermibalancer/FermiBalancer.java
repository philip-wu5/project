package vcluster.plugin.balancer.fermibalancer;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import vcluster.plugInterfaces.LoadBalancer;
import vcluster.plugin.balancer.fermibalancer.VM.VmStat;
import vcluster.elements.*;
import vcluster.executors.*;
import vcluster.managers.*;
import vcluster.ui.*;

public class FermiBalancer implements LoadBalancer{

	/*
	 *   There should be several different threads to handle different info
	 *   1. update infomation. need to ensure the consistency between load balancer and real info
	 *   2. keep tracking of the start time info, training and updating host parameters
	 *   3. main threads will be decision making process
	 */
	public static final String logFile="log//Balancer_Log";
    public static final String VmLaunchLogFile="log//VmLaunch_Log";
    public static final int port=9738;
    public static final int imageRepoReadSpeed=100;
    public static final int MAX_CONVERGE_ROUND=10;
    public static final int MIN_CONVERGE_ROUND=5;
    public static final double CONVERGE_THRESHOLD=2;
    public static final double MIN_GRANULARITY=0.00001;
    private Lock lock;
	@Override
	public void activate() {
		// TODO Auto-generated method stub
		//CloudmanExecutor.register(new CmdComb("cloudman register fermi-proxy.conf"));
		//CloudmanExecutor.register(new CmdComb("cloudman register amazon.conf"));
	    for(Cloud c:CloudManager.getCloudList().values()){
	    	CloudmanExecutor.load(new CmdComb("cloudman load "+ c.getCloudName()));
	    }
	    Thread infoCollectorThread = new Thread(new InfoCollector(port));
        infoCollectorThread.start();
        Thread infoUpdateThread=new Thread(new hostInfoCollector());
        infoUpdateThread.start();
        Thread vmLaunchThread=new Thread(new VmLauncher());
        vmLaunchThread.start();
        lock=new ReentrantLock();
       
        	while (!Algorithm.ready){
        		try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        	}
        	testDefaultLaunch();
        	try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	test();
        	/*for(int i=0;i<79;i++){
        		try{
        	if(lock.tryLock()){
        	//Thread.sleep(30000);
        	long currentTime=System.currentTimeMillis();
        	VM v=new VM();
        	Host h=Algorithm.estimateDeploy(v);
            //String str=CloudManager.getCurrentCloud().createVMonHost(1, h.getId());
            String str=CloudManager.getCurrentCloud().createVM(1, h.getAddress());
        	String [] sub=str.split("\\s+");
            
            v.setId(sub[1]);
            v.setDnsName(sub[2]);
            v.setStartTime(currentTime);
            v.setHostId(h.getId());
            v.setHostName(h.getAddress());
            Algorithm.getVmWaitQueue().add(v);
            Algorithm.dump();
        	}*/
        	//Thread.sleep(60000);
        	
       // }catch(Exception e){
       // 	e.getStackTrace();
       // }
       //finally{
      //  	lock.unlock();
       // }
        //}
        
        }
	
	private void test(){
		FermiBalancer.writeLaunchLogFile("VM Simulaneous launch with algorithm-------------");
		ArrayList<Integer> timing=Exp.readPattern();
		if (timing==null||timing.isEmpty()){
			System.out.println("[ERROR: ] No pattern available!");
		}
		else{
			lock= new ReentrantLock();
			try{
				if(lock.tryLock()){
					long currentTime=System.currentTimeMillis()/1000*1000;
					int sum=0;
					for(int i=0;i<timing.size();i++){
						sum=timing.get(i)+sum;
						VM v=new VM();
						v.setStartTime(currentTime+sum*1000);
						v.setStatus(VmStat.SCHEDULE);
						Algorithm.getVmWaitQueue().add(v);
						v=Algorithm.getVmWaitQueue().get(Algorithm.getVmWaitQueue().size()-1);
						Host h=Algorithm.BestFitHost(v,false);
						
						v.setStatus(VmStat.PENDING);
						//v.setWaitTime(0);
						v.setHostName(h.getAddress());
						
					}
					for(VM v:Algorithm.getVmWaitQueue()){
						Date d=new Date(v.getStartTime());
						System.out.println(d.toString()+" "+Integer.toString(v.getWaitTime()));
					}
				}
			}catch (Exception e){
				e.printStackTrace();
			}finally{
				lock.unlock();
			}
		}
		while (!Algorithm.getVmWaitQueue().isEmpty()){
        	try {
				Thread.sleep(100000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        for(VM v:Algorithm.getVmList()){
        	deleteVM(v);
        }
	}
	
	private void testDefaultLaunch(){
		FermiBalancer.writeLaunchLogFile("VM Simulaneous launch with OpenNebula Defualt Scheduler-------------");
		ArrayList<Integer> timing=Exp.readPattern();
		if (timing==null||timing.isEmpty()){
			System.out.println("[ERROR: ] No pattern available!");
		}
		else{
			lock= new ReentrantLock();
			try{
				if(lock.tryLock()){
					long currentTime=System.currentTimeMillis()/1000*1000;
					int sum=0;
					for(int i=0;i<timing.size();i++){
						sum=timing.get(i)+sum;
						VM v=new VM();
						v.setStartTime(currentTime+sum*1000);
						v.setStatus(VmStat.SCHEDULE);
						Algorithm.getVmWaitQueue().add(v);
						//v=Algorithm.getVmWaitQueue().get(Algorithm.getVmWaitQueue().size()-1);
						//Host h=Algorithm.BestFitHost(v,false);
						
						v.setStatus(VmStat.PENDING);
						//v.setWaitTime(0);
						v.setHostName("-1");
						
					}
					for(VM v:Algorithm.getVmWaitQueue()){
						Date d=new Date(v.getStartTime());
						System.out.println(d.toString()+" "+Integer.toString(v.getWaitTime()));
					}
				}
			}catch (Exception e){
				e.printStackTrace();
			}finally{
				lock.unlock();
			}
		}
		while (!Algorithm.getVmWaitQueue().isEmpty()){
        	try {
				Thread.sleep(100000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        for(VM v:Algorithm.getVmList()){
        	deleteVM(v);
        }
	}
	public static void writeLogFile(String line){
		try{
    		Writer output;
    		output = new BufferedWriter(new FileWriter(FermiBalancer.logFile,true));
    		Date d=new Date(System.currentTimeMillis());
    		String str="["+d.toString()+"] "+ line+System.lineSeparator();
    		output.append(str);
    		output.close();
    	}catch(Exception e){
    			e.getStackTrace();
    	}
	}
	
	public static void writeLaunchLogFile(String line){
		try{
    		Writer output;
    		output = new BufferedWriter(new FileWriter(FermiBalancer.VmLaunchLogFile,true));
    		Date d=new Date(System.currentTimeMillis());
    		String str="["+d.toString()+"] "+ line+System.lineSeparator();
    		output.append(str);
    		output.close();
    	}catch(Exception e){
    			e.getStackTrace();
    	}
	}

	
	private void deleteVM(VM v){
		
		int index=hostInfoCollector.findHost(v.getHostName(), Algorithm.getPrivateHostList());
		if(index!=-1){
			String cloudname=Algorithm.getPrivateHostList().get(index).getCloudName();
			for(Cloud c:CloudManager.getCloudList().values()){
				if(c.getCloudName().equalsIgnoreCase(cloudname)){
					Vm vm=new Vm();
					vm.setId(v.getId());
					c.destroyVM(vm);
					break;
				}
			}
			Algorithm.getPrivateHostList().get(index).getVmList().remove(v);
			Algorithm.getVmList().remove(v);
		}
		
	}
}
