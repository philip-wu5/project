package vcluster.plugin.balancer.fermibalancer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import vcluster.plugin.balancer.fermibalancer.VM.VmStat;

public class clientInfoCollector implements Runnable {

	private final Socket clientSocket;
	private long bootTime;
	private String hostName;
	private Lock lock;

    public clientInfoCollector(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.lock=new ReentrantLock(); 
    }
	
	@Override
	public void run() {
		// TODO Not work properply, needs to be done.
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
			String str=null;
			str=in.readLine();
			System.out.println(str);
			Date date = new SimpleDateFormat("EEE MMM d H:m:s zzz yyyy", Locale.ENGLISH).parse(str);
			bootTime=date.getTime();
			str=in.readLine().trim();
			System.out.println(str);
			hostName=str;
			if(lock.tryLock()){
			for(int i=0;i<Algorithm.getVmWaitQueue().size();i++){
				if(Algorithm.getVmWaitQueue().get(i).getDnsName().equalsIgnoreCase(str)){
					long start=Algorithm.getVmWaitQueue().get(i).getStartTime();
					Date startD=new Date(start);
					Algorithm.getVmWaitQueue().get(i).setRealBootTime((int)(bootTime-start)/1000);
					Algorithm.getVmWaitQueue().get(i).setStatus(VmStat.RUNNING);
					Host h=hostInfoCollector.findHost(Algorithm.getVmWaitQueue().get(i).getHostName());
					String line = "VM: "+ Algorithm.getVmWaitQueue().get(i).getId()+" Start: " + startD.toString()+
							"Booted: "+ date.toString()+" Estimated Boot Time: "+ Integer.toString(Algorithm.getVmWaitQueue().get(i).getBootTime())
							+"s Actual Boot Time: "+Integer.toString(Algorithm.getVmWaitQueue().get(i).getRealBootTime())+" s";
					Algorithm.getVmList().add(Algorithm.getVmWaitQueue().get(i));
					Algorithm.getVmWaitQueue().remove(i);
					if(h!=null)
						h.trainning(false);
					FermiBalancer.writeLaunchLogFile(line);
					break;
					}
				}
			}
			clientSocket.close();
			Algorithm.dump();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally{
			lock.unlock();
		}
	}

	public long getBootTime() {
		return bootTime;
	}

	public void setBootTime(long bootTime) {
		this.bootTime = bootTime;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
}
