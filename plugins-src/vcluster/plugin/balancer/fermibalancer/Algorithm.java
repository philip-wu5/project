package vcluster.plugin.balancer.fermibalancer;

import java.util.ArrayList;

public class Algorithm {

	private static ArrayList<Host> privateHostList;
	private static ArrayList<Host> publicHostList;
	public static final int BUFFER_TIME=10;
	static{
		privateHostList=new ArrayList<Host>();
		publicHostList=new ArrayList<Host>();
	}
	public static ArrayList<Host> getPrivateHostList() {
		return privateHostList;
	}
	public static void setPrivateHostList(ArrayList<Host> privateHostList) {
		Algorithm.privateHostList = privateHostList;
	}
	public static ArrayList<Host> getPublicHostList() {
		return publicHostList;
	}
	public static void setPublicHostList(ArrayList<Host> publicHostList) {
		Algorithm.publicHostList = publicHostList;
	}
	
	public static ArrayList<SystemStat>getEstimateSysStatus(ArrayList<SystemStat> current, int max_time_window){
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
		ArrayList<SystemStat> newStat=new ArrayList<SystemStat>();
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

	public static Host estimateDeploy(){
		//TODO return the host that with the minimum launching overhead
		int full=0;
		int minLaunchTime=Integer.MAX_VALUE;
		long currentTime=System.currentTimeMillis();
		ArrayList<SystemStat> sysStat=new ArrayList<SystemStat>();
		Host minHost=null;
		VmLaunchOverheadModel model=null;
		for(Host h:privateHostList){
			if(h.getVmList().size()==h.getCpuCount())
			{
				full++;
				continue;
			}
			int count=calConcurrentIOUsage(h,currentTime);
			boolean cache=ifCached(h,currentTime);
			if(h.getMemorySize()<=16000)
				cache=false;
			ArrayList<SystemStat> temp =getEstimateSysStatus(h.getSysStat(),Host.MAX_TIME_FRAME);
			model=new VmLaunchOverheadModel(h.getReadBandwidth()/count, h.getWriteBandwidth()/count,h.getCacheBandwidth()
					,h.getMemorySize(),16000,h.getEpsilon(),h.getGamma(),h.getBeta(),h.getBootMin(),
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
		    		io=model.U_IO(i, h.getCpuCount());
		    	}else{
		    	    if(i==0)
		    	    {
		    		    cpu=model.U_Boot(i, temp.get(0).ioUtil);
		    		    io=model.U_IO(i, h.getCpuCount());
		    		}
		    	    else
		    	    {
		    	    	cpu=model.U_Boot(i, temp.get(i-1).ioUtil);
		    	    	io=model.U_IO(i, h.getCpuCount());
		    	    }
		    	}
		    	tempCpu.add(cpu);
		    	tempIO.add(io);
		    	temp.get(i).cpuUtil=Math.min(1, temp.get(i).cpuUtil+cpu);
		    	temp.get(i).ioUtil=Math.min(1, temp.get(i).ioUtil+io);
		    }
		    model.setCpuU(tempCpu);
		    model.setIOU(tempIO);
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
				System.out.printf("[Deploy]: VM %s to Host %s", v.getId(),minHost.getId());
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

	//Test code for estimate
	public static Host estimateDeployTest(long time){
		int full=0;
		int minLaunchTime=Integer.MAX_VALUE;
		long currentTime=time;
		ArrayList<SystemStat> sysStat=new ArrayList<SystemStat>();
		Host minHost=null;
		VmLaunchOverheadModel model=null;
		for(Host h:privateHostList){
			if(h.getVmList().size()==h.getCpuCount())
			{
				full++;
				continue;
			}
			int count=calConcurrentIOUsage(h,currentTime);
			boolean cache=ifCached(h,currentTime);
			if(h.getMemorySize()<=16000)
				cache=false;
			ArrayList<SystemStat> temp =getEstimateSysStatus(h.getSysStat(),Host.MAX_TIME_FRAME);
			model=new VmLaunchOverheadModel(h.getReadBandwidth()/count, h.getWriteBandwidth()/count,h.getCacheBandwidth()
					,h.getMemorySize(),16000,h.getEpsilon(),h.getGamma(),h.getBeta(),h.getBootMin(),
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
		    		io=model.U_IO(i, h.getCpuCount());
		    	}else{
		    	    if(i==0)
		    	    {
		    		    cpu=model.U_Boot(i, temp.get(0).ioUtil);
		    		    io=model.U_IO(i, h.getCpuCount());
		    		}
		    	    else
		    	    {
		    	    	cpu=model.U_Boot(i, temp.get(i-1).ioUtil);
		    	    	io=model.U_IO(i, h.getCpuCount());
		    	    }
		    	}
		    	tempCpu.add(cpu);
		    	tempIO.add(io);
		    	temp.get(i).cpuUtil=Math.min(1, temp.get(i).cpuUtil+cpu);
		    	temp.get(i).ioUtil=Math.min(1, temp.get(i).ioUtil+io);
		    }
		    model.setCpuU(tempCpu);
		    model.setIOU(tempIO);
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
				totalTime+=v.getBootTime();
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
    	System.out.println(tHid+tCapacity+tRunVm+tType+tCloudName+System.lineSeparator());
    	System.out.println("------------------------------------------------------------"+System.lineSeparator());
    	for(Host h:privateHostList){
        	String hHid=String.format("%-8s",h.getId() );
        	String hCapacity=String.format("%-10s", Integer.toString(h.getCpuCount()));
        	String hRunVm=String.format("%-8s", Integer.toString(h.getVmList().size()));
        	String hType=String.format("%-8s", "PRIVATE");
        	String hCloudName=String.format("%-8s", h.getCloudName());
        	System.out.println(hHid+hCapacity+hRunVm+hType+hCloudName+System.lineSeparator());
    	}
    	for(Host h:publicHostList){
        	String hHid=String.format("%-8s",h.getId() );
        	String hCapacity=String.format("%-10s", Integer.toString(h.getCpuCount()));
        	String hRunVm=String.format("%-8s", Integer.toString(h.getVmList().size()));
        	String hType=String.format("%-8s", "PUBLIC");
        	String hCloudName=String.format("%-8s", h.getCloudName());
        	System.out.println(hHid+hCapacity+hRunVm+hType+hCloudName+System.lineSeparator());
    	}
    	
    }
}
