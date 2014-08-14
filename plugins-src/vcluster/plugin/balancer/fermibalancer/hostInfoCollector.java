package vcluster.plugin.balancer.fermibalancer;

import vcluster.elements.*;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class hostInfoCollector implements Runnable{

	private static final int UPDATE_INTERVAL=60000;
	private int port=9734;
	private Lock lock;
	private CopyOnWriteArrayList<vcluster.elements.Host> vclusterPrivateHostList;
	private CopyOnWriteArrayList<vcluster.elements.Host> vclusterPublicHostList;
	private CopyOnWriteArrayList<vcluster.elements.Vm> vmList;
	public hostInfoCollector(){
		lock=new ReentrantLock();
		vclusterPrivateHostList=new CopyOnWriteArrayList<vcluster.elements.Host> ();
		vclusterPublicHostList=new CopyOnWriteArrayList<vcluster.elements.Host> ();
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(!vcluster.Vcluster.terminate){
			vclusterPrivateHostList=new CopyOnWriteArrayList<vcluster.elements.Host> ();
			vclusterPublicHostList=new CopyOnWriteArrayList<vcluster.elements.Host> ();
			vmList=new CopyOnWriteArrayList<vcluster.elements.Vm> ();
			for(vcluster.elements.Cloud c:vcluster.managers.CloudManager.getCloudList().values()){
				c.listVMs();
				c.listHost();
				c.matchVMtoHost();
				boolean privateCloud=true;
				for(Vm vm:c.getVmList().values()){
					vmList.add(vm);
				}
				if(c.getCloudType()!=vcluster.elements.Cloud.CloudType.PRIVATE)
					privateCloud=false;
				for(vcluster.elements.Host h:c.getHostList().values()){
					if(h.getStat()!=vcluster.elements.Host.HostStat.ON)
						continue;
					if(privateCloud)
						vclusterPrivateHostList.add(h);
					else
						vclusterPublicHostList.add(h);
				}
			}
			try{
				/*
				 *  1. add new host
				 *  2. delete old one
				 *  3. update the real system info
				 *  4. update VM info
				 */
			
				if(lock.tryLock()){
					long currentTime=System.currentTimeMillis()/1000*1000;
					if(!Algorithm.getPrivateHostList().isEmpty()){
							for(Host h:Algorithm.getPrivateHostList()){
							int index=findHost(h,vclusterPrivateHostList);
							if(index==-1){
								Algorithm.getPrivateHostList().remove(h);
								}
							else{
								h.setSysStat(updateSysStat(h,vclusterPrivateHostList.get(index).getCPUUtil(),vclusterPrivateHostList.get(index).getIOUtil(),currentTime));
								h.setVmList(new CopyOnWriteArrayList<VM>());
								vclusterPrivateHostList.remove(index);
								} 
							}
			    	}
			    	if(!vclusterPrivateHostList.isEmpty()){
				    	for(int i=0;i<vclusterPrivateHostList.size();i++){
				    		Host newHost=new Host(vclusterPrivateHostList.get(i).getCloudName(),vclusterPrivateHostList.get(i).getId(),vclusterPrivateHostList.get(i).getName(),port,
				    				(int)vclusterPrivateHostList.get(i).getTMEM(),vclusterPrivateHostList.get(i).getTCPU()/100);
					    Algorithm.getPrivateHostList().add(newHost);
				    	}
				    }
				    for(Host h:Algorithm.getPublicHostList()){
				    	h.setVmList(new CopyOnWriteArrayList<VM>());
				    }
				    updateVMs();
				    //Algorithm.dump();
				}
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				Algorithm.ready=true;
				lock.unlock();
			}
			try {
				Thread.sleep(UPDATE_INTERVAL);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
    /**
     * Find a Host in private host list, if find, return the index of position in private host list. If not find, return -1
     * @param h : vcluster host
     * @see vcluster.elements.Host
     * @param privateCloud :if the host is in a private cloud
     * @return
     */
	private static int findHost(vcluster.elements.Host h, boolean privateCloud){
		int find=-1;
		CopyOnWriteArrayList<Host> hostList=null;
		if(privateCloud)
			hostList=Algorithm.getPrivateHostList();
		else
			hostList=Algorithm.getPublicHostList();
		for(int i=0;i<hostList.size();i++){
			if(h.getName().equalsIgnoreCase(hostList.get(i).getAddress()))
			{
				find=i;
				break;
			}
		}
		return find;
	}
	/**
	 * Find a Host in private host list, if find, return the index of position in private host list. If not find, return -1
	 * @param h : Load balancer Host type
	 * @param hostList : list of host
	 * @return
	 */
	private int findHost(Host h, CopyOnWriteArrayList<vcluster.elements.Host> hostList){
		int find=-1;
		if(hostList.isEmpty())
			return -1;
		for(int i=0;i<hostList.size();i++)
		{
			if(hostList.get(i).getName().equalsIgnoreCase(h.getAddress()))
			{
				find=i;break;
			}
		}
		return find;
	}
	public static int findHost(String hostName, CopyOnWriteArrayList<Host> hostList){
		int index=-1;
		for(int i=0;i<hostList.size();i++){
			if(Cloud.hostNameMatch(hostName, hostList.get(i).getAddress()))
				return index=i;
		}
		return index;
	}
	public static Host findHost(String hostName){
		Host h=null;
		for(Host hTmp:Algorithm.getPrivateHostList()){
			if(Cloud.hostNameMatch(hostName, hTmp.getAddress()))
				return hTmp;
		}
		for(Host hTmp:Algorithm.getPublicHostList()){
			if(Cloud.hostNameMatch(hostName, hTmp.getAddress()))
				return hTmp;
		}
		return h;
	}
	private CopyOnWriteArrayList<SystemStat> updateSysStat(Host h, double cpu, double io, long time){
		CopyOnWriteArrayList<SystemStat> tempStat=h.getSysStat();
		int size=Host.MAX_TIME_FRAME;
		if(tempStat.isEmpty()||time>tempStat.get(size-1).time||time<tempStat.get(0).time)
		{
			tempStat=new CopyOnWriteArrayList<SystemStat>();
			for(int i=0;i<size;i++){
				SystemStat s=new SystemStat();
				s.cpuUtil=cpu;
				s.ioUtil=io;
				s.time=(time/1000+i)*1000;
				tempStat.add(s);
			}
			return tempStat;
		}
		//else if(time<=tempStat.get(size-1).time&&time>=tempStat.get(0).time){
		else{
			int index=0;
			for(int i=0;i<size;i++){
				if(tempStat.get(i).time==time){
					index=i;
					break;
				}
			}
			double diffCpu=cpu-tempStat.get(index).cpuUtil;
			double diffIO=io-tempStat.get(index).ioUtil;
			CopyOnWriteArrayList<SystemStat> newStat=new CopyOnWriteArrayList<SystemStat>();
			for(int j=0;j<size;j++){
				SystemStat s=new SystemStat();
				s.time=(time/1000+j)*1000;
				if(j+index<size){
					s.cpuUtil=Math.min(1, Math.max(tempStat.get(index).cpuUtil+diffCpu, 0));
					s.ioUtil=Math.min(1, Math.max(tempStat.get(index).ioUtil+diffIO, 0));
				}
				else
				{
					s.cpuUtil=cpu;
					s.ioUtil=io;
				}
				newStat.add(s);
			}
			return newStat;
		}
	}

    private boolean updateVMs(){
    	boolean success=true;
    	//CopyOnWriteArrayList<Vm> vmList= new CopyOnWriteArrayList<Vm>(vcluster.managers.VmManager.getVmList().values());
        if(Algorithm.getVmList().size()+Algorithm.getVmWaitQueue().size()!=vmList.size()){
        	String line="[Warning:] VM numbers are not matching, some manual operations are made!";
        	FermiBalancer.writeLogFile(line);
        	System.out.println(line+System.lineSeparator());
        }
        
        for(VM v:Algorithm.getVmList()){
        	int index=findVM(v,vmList);
        	if(index==-1){
        		String line="[Warning: ]VM "+v.getId()+" does not exist!";
        		FermiBalancer.writeLogFile(line);
        		Algorithm.getVmList().remove(v);
        	}else{
        		if(!Cloud.hostNameMatch(v.getHostName(),vmList.get(index).getHostname())){
        			String line="[Warning: ] VM "+v.getId()+" that suppose to be launched on Host "+v.getHostName()+
        					" is being launched on Host "+vmList.get(index).getHostname();
        			FermiBalancer.writeLogFile(line);
        			v.setRealHostName(vmList.get(index).getHostname());
        			vmList.remove(index);
           		}
        		else{
        			v.setRealHostName(v.getHostName());
        			vmList.remove(index);
        		}
        		int privatehostindex=findHost(v.getRealHostName(),Algorithm.getPrivateHostList());
        		int publichostindex=findHost(v.getRealHostName(),Algorithm.getPublicHostList());
        		if(privatehostindex==-1&&publichostindex==-1){
        			String line="[Error: ] Host "+v.getRealHostName()+" does not exist in the list!";
        			FermiBalancer.writeLogFile(line);
        			success=false;
        		}else if(publichostindex==-1){
        		Algorithm.getPrivateHostList().get(privatehostindex).addVM(v);}
        		else{
        			Algorithm.getPublicHostList().get(publichostindex).addVM(v);
        		}
        	}
        }
        for(VM v:Algorithm.getVmWaitQueue()){
        	int index=findVM(v,vmList);
        	if(index==-1){
        		String line="[Warning: ]VM "+v.getId()+" does not exist!";
        		FermiBalancer.writeLogFile(line);
        		Algorithm.getVmList().remove(v);
        		success=false;
        	}else{
        		String temp1=v.getHostName();
        		String temp2=vmList.get(index).getHostname();
        		if(!Cloud.hostNameMatch(temp1, temp2)){
        			String line="[Warning: ] VM "+v.getId()+" that suppose to be launched on Host "+v.getHostName()+
        					" is being launched on Host "+vmList.get(index).getHostname();
        			FermiBalancer.writeLogFile(line);
        			v.setRealHostName(vmList.get(index).getHostname());
        			vmList.remove(index);
           		}
        		else{
        			v.setRealHostName(v.getHostName());
        			vmList.remove(index);
        		}
        		int privatehostindex=findHost(v.getRealHostName(),Algorithm.getPrivateHostList());
        		int publichostindex=findHost(v.getRealHostName(),Algorithm.getPublicHostList());
        		if(privatehostindex==-1&&publichostindex==-1){
        			String line="[Error: ] Host "+v.getRealHostName()+" does not exist in the list!";
        			FermiBalancer.writeLogFile(line);
        			success=false;
        		}else if(publichostindex==-1){
        		Algorithm.getPrivateHostList().get(privatehostindex).addVM(v);}
        		else{
        			Algorithm.getPublicHostList().get(publichostindex).addVM(v);
        		}
        	}
        }
        if(!vmList.isEmpty()){
        	for(int i=0;i<vmList.size();i++){
        		VM v=new VM();
        		v.setId(vmList.get(i).getId());
        		v.setHostName(vmList.get(i).getHostname());
        		v.setDnsName(vmList.get(i).getDNSName());
        		v.setRealHostName(vmList.get(i).getHostname());
        		v.setRealBootTime(-1);
        		v.setBootTime(-1);
        		Algorithm.getVmList().add(v);
        		int privatehostindex=findHost(v.getRealHostName(),Algorithm.getPrivateHostList());
        		int publichostindex=findHost(v.getRealHostName(),Algorithm.getPublicHostList());
        		if(privatehostindex==-1&&publichostindex==-1){
        			String line="[Error: ] Host "+v.getRealHostName()+" does not exist in the list!";
        			FermiBalancer.writeLogFile(line);
        			success=false;
        		}else if(publichostindex==-1){
        		Algorithm.getPrivateHostList().get(privatehostindex).addVM(v);}
        		else{
        			Algorithm.getPublicHostList().get(publichostindex).addVM(v);
        		}
        	}
        }
        return success;
    }
    
    private int findVM(VM v, CopyOnWriteArrayList<Vm> vmList){
    	int index=-1;
    	for(int i=0;i<vmList.size();i++){
    		if(vmList.get(i).getId().equalsIgnoreCase(v.getId())&&
    				vmList.get(i).getDNSName().equalsIgnoreCase(v.getDnsName()))
    			return index=i;
    	}
    	return index;
    }
}
