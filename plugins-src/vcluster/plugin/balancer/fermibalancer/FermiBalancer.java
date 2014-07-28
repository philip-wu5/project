package vcluster.plugin.balancer.fermibalancer;


import vcluster.plugInterfaces.LoadBalancer;
import vcluster.elements.*;
import vcluster.executors.*;
import vcluster.managers.*;
import vcluster.ui.*;
public class FermiBalancer implements LoadBalancer{

	
	
	@Override
	public void activate() {
		// TODO Auto-generated method stub
		CloudmanExecutor.register(new CmdComb("cloudman register fermi-proxy.conf"));
		CloudmanExecutor.register(new CmdComb("cloudman register amazon.conf"));
	    for(Cloud c:CloudManager.getCloudList().values()){
	    	CloudmanExecutor.load(new CmdComb("cloudman load "+ c.getCloudName()));
	    }
	}
	
}
