package vcluster.plugin.balancer.fermibalancer;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Algorithm {

	private static CopyOnWriteArrayList<Host> privateHostList;
	private static CopyOnWriteArrayList<Host> publicHostList;
	private static CopyOnWriteArrayList<VM> vmWaitQueue;
	private static CopyOnWriteArrayList<VM> vmList;
	public static final int BUFFER_TIME=10000;
	public static boolean ready=false;
	static{
		privateHostList=new CopyOnWriteArrayList<Host>();
		publicHostList=new CopyOnWriteArrayList<Host>();
		setVmList(new CopyOnWriteArrayList<VM>());
		vmWaitQueue=new CopyOnWriteArrayList<VM>();
	}
	public static CopyOnWriteArrayList<Host> getPrivateHostList() {
		return privateHostList;
	}
	public static void setPrivateHostList(CopyOnWriteArrayList<Host> privateHostList) {
		Algorithm.privateHostList = privateHostList;
	}
	public static CopyOnWriteArrayList<Host> getPublicHostList() {
		return publicHostList;
	}
	public static void setPublicHostList(CopyOnWriteArrayList<Host> publicHostList) {
		Algorithm.publicHostList = publicHostList;
	}
	
	public static CopyOnWriteArrayList<SystemStat>getEstimateSysStatus(CopyOnWriteArrayList<SystemStat> current, int max_time_window){
		long currentTime=System.currentTimeMillis();
		currentTime=currentTime/1000*1000;
		boolean find=false;
		int i=0;
		if(!current.isEmpty()){
		for( ;i<current.size();i++){
			if(current.get(i).time==currentTime){
				find=true;
				break;
			}
		}
		}
		CopyOnWriteArrayList<SystemStat> newStat=new CopyOnWriteArrayList<SystemStat>();
		for(int j=0;j<max_time_window;j++){
			if(current.isEmpty()){
				SystemStat s=new SystemStat();
				s.time=currentTime+j*1000;
				s.cpuUtil=0;
				s.ioUtil=0;
				newStat.add(s);
				
			}else{
			if(find&&i<current.size()){
				newStat.add(current.get(i));
				i++;
			}
			else{
				SystemStat s=current.get(current.size()-1);
				s.time=currentTime+j*1000;
				newStat.add(s);
			}
		}
		}
		return newStat;
	}
    public static Host BestFitHost(VM v, boolean debug){
    	Host h=null;
    	VM tmp=new VM();
    	tmp.setStartTime(v.getStartTime());
    	tmp.setWaitTime(v.getWaitTime());
    	int minBootTime=0;;
    	int waitTime=0;
    	Lock lock=new ReentrantLock();
    	try{
    		if(lock.tryLock()){
    			VM lastVM=null;
    			if(Algorithm.getVmWaitQueue().size()<2)
    				lastVM=Algorithm.getVmWaitQueue().get(0);
    			else
    			    lastVM=Algorithm.getVmWaitQueue().get(Algorithm.getVmWaitQueue().size()-2);
    			long est=lastVM.getStartTime()+lastVM.getWaitTime();
    			v.setStartTime(est);
    			waitTime=(int)(est-tmp.getStartTime());
    			Host tmpHost=estimateDeploy(v,debug);
    			minBootTime=v.getBootTime();
    			h=tmpHost;
    			tmp.setOverhead(v.getOverhead());
    			for(int i=0;i<Algorithm.vmWaitQueue.size()-2;i++){
    				lastVM=Algorithm.vmWaitQueue.get(i);
    				if(lastVM.getStartTime()+lastVM.getWaitTime()+lastVM.getOverhead().transTime()*1000>est){
    					long newStart=lastVM.getStartTime()+lastVM.getWaitTime()+lastVM.getOverhead().transTime()*1000;
    					int wait=(int)(newStart-tmp.getStartTime());
    					v.setStartTime(newStart);
    					v.setWaitTime(0);
    					tmpHost=estimateDeploy(v,debug);
    					if(debug)
    						System.out.println("Delay: "+ Integer.toString(wait/1000)+" Boot Time: "+Integer.toString(v.getBootTime()+wait/1000));
    					if(v.getBootTime()+wait/1000<=minBootTime){
    						minBootTime=v.getBootTime()+wait/1000;
    						waitTime=wait;
    						h=tmpHost;
    						tmp.setOverhead(v.getOverhead());
    					}
    				}
    			}
    			v.setStartTime(tmp.getStartTime());
    			v.setWaitTime(waitTime+tmp.getWaitTime());
    			v.setBootTime(minBootTime);
    			v.setOverhead(tmp.getOverhead());
    			v.setHostId(h.getId());
    			v.setHostName(h.getAddress());
    			h.getVmList().add(v);
    			h.setMemorySize(Math.max(0, h.getMemorySize()-2000));
    			h.setSysStat(updateHostSysStat(h,v));
    		}
    	}catch(Exception e){
    		e.printStackTrace();
    	}finally{
    		lock.unlock();
    	}
    	return h;
    }
    public static CopyOnWriteArrayList<SystemStat> updateHostSysStat(Host h, VM v){
    	CopyOnWriteArrayList<SystemStat> temp =getEstimateSysStatus(h.getSysStat(),Host.MAX_TIME_FRAME);
    	for(int i=0;i<Host.MAX_TIME_FRAME;i++){
    		temp.get(i).cpuUtil=Math.min(1, temp.get(i).cpuUtil+v.getOverhead().getCpuU().get(i));
	    	temp.get(i).ioUtil=Math.min(1, temp.get(i).ioUtil+v.getOverhead().getIOU().get(i));
    	}
    	return temp;
    }
	public static Host estimateDeploy(VM v,boolean debug){
		//TODO return the host that with the minimum launching overhead
		int full=0;
		int minLaunchTime=Integer.MAX_VALUE;
		long currentTime=v.getStartTime();
		CopyOnWriteArrayList<SystemStat> sysStat=new CopyOnWriteArrayList<SystemStat>();
		Host minHost=null;
		VmLaunchOverheadModel model=null;
		for(Host h:privateHostList){
			if(h.getVmList().size()==h.getCpuCount())
			{
				full++;
				continue;
			}
			int count=calConcurrentIOUsage(h,currentTime);
			int count_read=calConcurrentRead(currentTime);
			//boolean cache=false;
			boolean cache=ifCached(h,currentTime);
			if(h.getMemorySize()<=16000)
				cache=false;
			CopyOnWriteArrayList<SystemStat> temp =getEstimateSysStatus(h.getSysStat(),Host.MAX_TIME_FRAME);
			//int imageSize=CloudManager.getCloudList().get(h.getCloudName()).getImageSize();
			//test
			int imageSize=2074;
			String msg="Image size: "+Integer.toString(imageSize)+"Mb";
			if(debug){
				System.out.println(msg);
				FermiBalancer.writeLogFile(msg);
			}
			
			
			model=new VmLaunchOverheadModel(h.getReadBandwidth(), h.getWriteBandwidth(),h.getCacheBandwidth()
					,Math.min(h.getReadBandwidth(),FermiBalancer.imageRepoReadSpeed/count_read), h.getWriteBandwidth()/count, h.getCacheBandwidth()/count,h.getMemorySize(),imageSize,h.getEpsilon(),h.getGamma(),h.getBeta(),h.getBootMin(),
					h.getA(),h.getB(),h.getC(),cache);
			
			//model.setImageSize(160000);
			ArrayList<Double> tempCpu=new ArrayList<Double>();
			ArrayList<Double> tempIO=new ArrayList<Double>();
		    for(int i=0;i<Host.MAX_TIME_FRAME;i++){
		    	double cpu=0;
		    	double io=0;
		    	if(i<model.getStartTime()){
		    		cpu=0;io=0;
		    	}else if(i>=model.getStartTime()&&i<=model.transTime()){
		    		cpu=model.util_trans(i);
		    		io=model.IO(i);
		    	}else{
		    	    if(i==0)
		    	    {
		    		    cpu=model.U_Boot(i, temp.get(0).ioUtil);
		    		    io=model.IO(i);
		    		}
		    	    else
		    	    {
		    	    	cpu=model.U_Boot(i, temp.get(i-1).ioUtil);
		    	    	io=model.IO(i);
		    	    }
		    	}
		    	tempCpu.add(cpu);
		    	tempIO.add(io);
		    	temp.get(i).cpuUtil=Math.min(1, temp.get(i).cpuUtil+cpu);
		    	temp.get(i).ioUtil=Math.min(1, temp.get(i).ioUtil+io);
		    }
		    model.setCpuU(tempCpu);
		    model.setIOU(tempIO);
		    //System.out.println(model.dump());
		    int bootTime=model.bootTime();
		    if(debug)
		    	System.out.printf("If deploy on host %s, booting time is %d seconds!\n", h.getId(),bootTime);
		    if((bootTime<minLaunchTime)|| (bootTime==minLaunchTime && h.getVmList().size()<minHost.getVmList().size())){
		    	minLaunchTime=bootTime;
		    	minHost=h;
		    	sysStat=temp;
		    	
		    }
		}
		if(full<privateHostList.size()){
			if(minHost!=null){
				//VM v=new VM();
				//v.setStartTime(currentTime);
				v.setBootTime(minLaunchTime);
				v.setHostId(minHost.getId());
				v.setOverhead(model);
				v.setId(minHost.getId()+Integer.toString(minHost.getVmList().size()));
				//minHost.addVM(v);
				//minHost.setSysStat(sysStat);
				//minHost.setMemorySize(Math.max(0, minHost.getMemorySize()-2000));
				if(debug)
					System.out.printf("[Deploy]: VM %s to Host %s, est launch time: %d\n", v.getId(),minHost.getId(),v.getBootTime());
				return minHost;
			}
			else
				System.out.println("[Error]: CANNOT find valid host for VM!\n");
		}else{
			publicHostList.get(0).addVM(new VM());
			return publicHostList.get(0); 
		}
		return null;
	}

	//Test code for estimate
	public static Host estimateDeployTest(long time){
		int full=0;
		int minLaunchTime=Integer.MAX_VALUE;
		long currentTime=time;
		CopyOnWriteArrayList<SystemStat> sysStat=new CopyOnWriteArrayList<SystemStat>();
		Host minHost=null;
		VmLaunchOverheadModel model=null;
		for(Host h:privateHostList){
			if(h.getVmList().size()==h.getCpuCount())
			{
				full++;
				continue;
			}
			int count=calConcurrentIOUsage(h,currentTime);
			int count_read=calConcurrentRead(currentTime);
			int readSpeed=Math.max(1, Math.min(h.getReadBandwidth(),FermiBalancer.imageRepoReadSpeed/count_read));
			int writeSpeed=Math.max(1, h.getWriteBandwidth()/count);
			boolean cache=ifCached(h,currentTime);
			if(h.getMemorySize()<=16000)
				cache=false;
			CopyOnWriteArrayList<SystemStat> temp =getEstimateSysStatus(h.getSysStat(),Host.MAX_TIME_FRAME);
			model=new VmLaunchOverheadModel(h.getReadBandwidth(), h.getWriteBandwidth(),h.getCacheBandwidth()
					,readSpeed, writeSpeed ,h.getCacheBandwidth(),h.getMemorySize(),2704,h.getEpsilon(),h.getGamma(),h.getBeta(),h.getBootMin(),
					h.getA(),h.getB(),h.getC(),cache);
			//model.setImageSize(160000);
			//model.setReadSpeed(Math.min(h.getReadBandwidth(),FermiBalancer.imageRepoReadSpeed/calConcurrentRead(currentTime)));
			//model.setWriteSpeed(h.getWriteBandwidth()/count);
			ArrayList<Double> tempCpu=new ArrayList<Double>();
			ArrayList<Double> tempIO=new ArrayList<Double>();
		    for(int i=0;i<Host.MAX_TIME_FRAME;i++){
		    	double cpu=0;
		    	double io=0;
		    	if(i<model.getStartTime()){
		    		cpu=0;io=0;
		    	}else if(i>=model.getStartTime()&&i<=model.transTime()){
		    		cpu=model.util_trans(i);
		    		io=model.IO(i);
		    	}else{
		    	    if(i==0)
		    	    {
		    		    cpu=model.U_Boot(i, temp.get(0).ioUtil);
		    		    io=model.IO(i);
		    		}
		    	    else
		    	    {
		    	    	cpu=model.U_Boot(i, temp.get(i-1).ioUtil);
		    	    	io=model.IO(i);
		    	    }
		    	}
		    	tempCpu.add(cpu);
		    	tempIO.add(io);
		    	temp.get(i).cpuUtil=Math.min(1, temp.get(i).cpuUtil+cpu);
		    	temp.get(i).ioUtil=Math.min(1, temp.get(i).ioUtil+io);
		    }
		    model.setCpuU(tempCpu);
		    model.setIOU(tempIO);
		    //System.out.print(model.dump());
		    FermiBalancer.writeLogFile(model.dump());
		    int bootTime=model.bootTime();
		    System.out.printf("If deploy on host %s, booting time is %d seconds!\n", h.getId(),bootTime);
		    if((bootTime<minLaunchTime)|| (bootTime==minLaunchTime && h.getVmList().size()<minHost.getVmList().size())){
		    	minLaunchTime=bootTime;
		    	minHost=h;
		    	sysStat=temp;
		    	
		    }
		}
		if(full<privateHostList.size()){
			if(minHost!=null){
				VM v=new VM();
				v.setStartTime(currentTime);
				v.setBootTime(minLaunchTime);
				v.setHostId(minHost.getId());
				v.setOverhead(model);
				v.setId(minHost.getId()+Integer.toString(minHost.getVmList().size()));
				minHost.addVM(v);
				minHost.setSysStat(sysStat);
				minHost.setMemorySize(Math.max(0, minHost.getMemorySize()-2000));
				System.out.printf("[Deploy]: VM %s to Host %s\n", v.getId(),minHost.getId());
				return minHost;
			}
			else
				System.out.println("[Error]: CANNOT find valid host for VM!");
		}else{
			publicHostList.get(0).addVM(new VM());
			return publicHostList.get(0); 
		}
		return null;
	}
	public static double aveLaunchTime(){
		int totalTime=0;
		int totalVM=0;
		for(Host h:privateHostList){
			for(VM v:h.getVmList()){
				totalTime+=v.getBootTime()+v.getWaitTime()/1000;
				totalVM+=1;
			}
		}
		return (double)totalTime/(double) totalVM;
	}
	public static int calConcurrentIOUsage(Host h, long currentTime){
		int count=1;
		for(VM v:h.getVmList()){
			int time=v.getOverhead().transTime()+BUFFER_TIME;
			long timeTemp=(long)v.getStartTime()+(long)time*1000;
			if(timeTemp>currentTime)
				count++;
		}
		return count;
	}
	public static int calConcurrentRead(long currentTime){
		int count=1;
		for(VM v:vmWaitQueue){
			if((v.getStartTime()+v.getWaitTime()+v.getOverhead().transTime()*1000+BUFFER_TIME)>currentTime)
				count++;
		}
		return count;
	}
	//TODO: Need a function to count the simultaneous read from image repo
	public static boolean ifCached(Host h, long currentTime){
		if(h.getVmList().isEmpty())
			return false;
		else{
		VM v=h.getVmList().get(h.getVmList().size()-1);
		if(v.getOverhead().ifCached())
			return true;
		else{
			long tFinish=v.getStartTime()+v.getOverhead().transTime();
			if(currentTime>=tFinish&&currentTime<=tFinish+BUFFER_TIME)
				return true;
			else 
				return false;
		}
		}
	}
    public static void dump(){
    	System.out.println("------------------------------------------------------------"+System.lineSeparator());
    	String tHid=String.format("%-8s", "HostID");
    	String tCapacity=String.format("%-10s", "Capacity");
    	String tRunVm=String.format("%-8s", "RunVMs");
    	String tType=String.format("%-8s", "TYPE");
    	String tCloudName=String.format("%-8s", "Cloud");
    	String tHAddress=String.format("%-20s", "Address");
    	System.out.println(tHid+tCapacity+tRunVm+tType+tCloudName+tHAddress+System.lineSeparator());
    	System.out.println("------------------------------------------------------------"+System.lineSeparator());
    	for(Host h:privateHostList){
        	String hHid=String.format("%-8s",h.getId() );
        	String hCapacity=String.format("%-10s", Integer.toString(h.getCpuCount()));
        	String hRunVm=String.format("%-8s", Integer.toString(h.getVmList().size()));
        	String hType=String.format("%-8s", "PRIVATE");
        	String hCloudName=String.format("%-8s", h.getCloudName());
        	String hAddress=String.format("%-20s", h.getAddress());
        	System.out.println(hHid+hCapacity+hRunVm+hType+hCloudName+hAddress+System.lineSeparator());
    	}
    	for(Host h:publicHostList){
        	String hHid=String.format("%-8s",h.getId() );
        	String hCapacity=String.format("%-10s", Integer.toString(h.getCpuCount()));
        	String hRunVm=String.format("%-8s", Integer.toString(h.getVmList().size()));
        	String hType=String.format("%-8s", "PUBLIC");
        	String hCloudName=String.format("%-8s", h.getCloudName());
        	System.out.println(hHid+hCapacity+hRunVm+hType+hCloudName+System.lineSeparator());
    	}
    	System.out.println("-----------------------VM LIST-----------------------------"+System.lineSeparator());
    	String tVMid=String.format("%-8s", "VMID");
    	String tVmName=String.format("%-20s", "VmName");
    	String tVmHost=String.format("%-20s", "Host");
    	String tVmBoot=String.format("%-8s", "BootTime");
    	System.out.println(tVMid+tVmName+tVmHost+tVmBoot+System.lineSeparator());
    	System.out.println("-------------------------RUNING----------------------------"+System.lineSeparator());
    	for(VM v:vmList){
    		String hVmId=String.format("%-8s", v.getId());
    		String hVmName=String.format("%-20s", v.getDnsName());
    		String hVmHost=String.format("%-20s", v.getHostName());
    		String hVmBoot=String.format("%-8s", v.getRealBootTime());
    		System.out.println(hVmId+hVmName+hVmHost+hVmBoot+System.lineSeparator());
    	}
    	System.out.println("------------------------WAITING----------------------------"+System.lineSeparator());
    	for(VM v:vmWaitQueue){
    		String hVmId=String.format("%-8s", v.getId());
    		String hVmName=String.format("%-20s", v.getDnsName());
    		String hVmHost=String.format("%-20s", v.getHostName());
    		String hVmBoot=String.format("%-8s", v.getBootTime()+v.getWaitTime()/1000);
    		String hVmWait=String.format("%-8s", v.getWaitTime()/1000);
    		System.out.println(hVmId+hVmName+hVmHost+hVmBoot+hVmWait+System.lineSeparator());
    	}
    }
	public static CopyOnWriteArrayList<VM> getVmList() {
		return vmList;
	}
	public static void setVmList(CopyOnWriteArrayList<VM> vmList) {
		Algorithm.vmList = vmList;
	}
	public static CopyOnWriteArrayList<VM> getVmWaitQueue() {
		return vmWaitQueue;
	}
	public static void setVmWaitQueue(CopyOnWriteArrayList<VM> vmWaitQueue) {
		Algorithm.vmWaitQueue = vmWaitQueue;
	}
	
	
}
