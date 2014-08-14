package vcluster.plugin.balancer.fermibalancer;

import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import vcluster.Vcluster;
import vcluster.managers.CloudManager;
import vcluster.plugin.balancer.fermibalancer.VM.VmStat;

//TODO: TO BE TESTED
public class VmLauncher implements Runnable{

	@Override
	public void run() {
		// TODO Auto-generated method stub
		Lock lock=new ReentrantLock();
		while(!Vcluster.terminate){
			int sleepTime=0;
			boolean launch=false;
			Host h=null;
			VM v=null;
			try{
				if(lock.tryLock()){
					if(Algorithm.getVmWaitQueue().isEmpty()){
						sleepTime=1000;
						launch=false;
					}
					else
					{
						for(int i=0;i<Algorithm.getVmWaitQueue().size();i++){
							if(Algorithm.getVmWaitQueue().get(i).getStatus()==VmStat.PENDING){
								v=Algorithm.getVmWaitQueue().get(i);
								long currentTime=System.currentTimeMillis()/1000*1000;
								long diff=v.getStartTime()-currentTime;
									
								sleepTime=Math.max(1, Algorithm.getVmWaitQueue().get(i).getWaitTime()+(int)diff);
								launch=true;
								break;
							}
						}
					}
					}
				}
			catch(Exception e){
				e.printStackTrace();
			}finally{
				lock.unlock();
			}
			
			try{
				Thread.sleep(sleepTime);
			}catch(Exception e){
				e.printStackTrace();
			}
			
			if(launch&&v!=null){
				if(!v.getHostName().equalsIgnoreCase("-1")){
				int privateIndex=-1;
				int publicIndex=-1;
				privateIndex=hostInfoCollector.findHost(v.getHostName(),Algorithm.getPrivateHostList());
				if(privateIndex==-1){
					publicIndex=hostInfoCollector.findHost(v.getHostName(),Algorithm.getPublicHostList());
				    if(publicIndex==-1){
				    	System.out.println("[ERROR: ] No host can be found for launching process!");
				        continue;
				    } else
				    	h=Algorithm.getPublicHostList().get(publicIndex);
				}else
					h=Algorithm.getPrivateHostList().get(privateIndex);
				if(h!=null){
					Date d=new Date(System.currentTimeMillis());
					System.out.println(d.toString()+" Launch a VM!");
					String str=CloudManager.getCurrentCloud().createVMonHost(1, h.getId());
	
					String [] sub=str.split("\\s+");
					
					v.setId(sub[1]);
					v.setDnsName(sub[2]);
					v.setHostId(h.getId());
					v.setHostName(h.getAddress());
					v.setStatus(VmStat.PROLOG);
					}
				}else
				{
					Date d=new Date(System.currentTimeMillis());
					System.out.println(d.toString()+" Launch a VM!");
					String str=CloudManager.getCurrentCloud().createVM(1, "-1");
	
					String [] sub=str.split("\\s+");
					
					v.setId(sub[1]);
					v.setDnsName(sub[2]);
					//v.setHostId(h.getId());
					//v.setHostName(h.getAddress());
					v.setStatus(VmStat.PROLOG);
				}
				}
			}
		}
	}

