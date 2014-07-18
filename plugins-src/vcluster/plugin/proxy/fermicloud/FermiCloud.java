package vcluster.plugin.proxy.fermicloud;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import vcluster.elements.Host;
import vcluster.elements.Slot;
import vcluster.elements.Vm;
import vcluster.managers.VmManager.VMState;
import vcluster.plugInterfaces.CloudInterface;

public class FermiCloud implements CloudInterface {
	
	
	private static void closeStream(BufferedReader in, DataOutputStream out, Socket socket)
	{
		try {
	        if (in != null) in.close();
	        if (out != null) out.close();
	        if (socket != null) socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private ArrayList<String> socketToproxy(String cmd){
		String cmdLine=cmd;
		ArrayList<String> feedBack = new ArrayList<String>();
		 Socket socket = null;
	        BufferedReader in = null;
	        DataOutputStream out = null;
	        //System.out.println(cmdLine);
	        try {
	        	socket = new Socket(addr, port);
	        	
	            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
	            out = new DataOutputStream(socket.getOutputStream());
	            out.flush();
	            /* make an integer to unsigned int */
	            int userInput = 5;
	            userInput <<= 8;
	            userInput |=  1;
	            userInput &= 0x7FFFFFFF;

	            String s = Integer.toString(userInput);
	            byte[] b = s.getBytes();
	            
	            out.write(b, 0, b.length);
	            out.write(cmdLine.getBytes(), 0, cmdLine.getBytes().length);
	            out.flush();
	            
	            String str=null;
	        	while((str=in.readLine())!=null)
	        	{
	        		str=str.trim();
	        		feedBack.add(str);
	        	}
	            /*char[] cbuf = new char[4096];
	        	String temp = null;
	        	while (in.read(cbuf, 0, 4096) != -1) {
	            	String str = new String(cbuf);
	    	        str = str.trim();	    	        
	    	        if (!str.equals(temp)){
	    	        	//System.out.println(str);
	    	        	 feedBack.add(str);
	    	        }
	    	        
	    	        //cbuf[0] = '\0';
	            	temp = str;
	            }*/
	            
	        } catch (UnknownHostException e) {
	    		System.out.print("ERROR: " +e.getMessage());
	            closeStream(in, out, socket);
	            return feedBack;
	        } catch (IOException e) {
	    		System.out.print("ERROR: " +e.getMessage());
	            closeStream(in, out, socket);
	            return feedBack;
	        }
	        
	        closeStream(in, out, socket);
	        return feedBack;
	}
	
	
	@Override
	public boolean RegisterCloud(List<String> configurations) {
		// TODO Auto-generated method stub

		for(String aLine : configurations){
			
			StringTokenizer st = new StringTokenizer(aLine, "=");
			
			if (!st.hasMoreTokens()) return false;
			
			/* get a keyword */
			String aKey = st.nextToken().trim();
		
			/* get a value */
			if (!st.hasMoreTokens()) return false;

			String aValue = st.nextToken().trim();
			
			if (aKey.equalsIgnoreCase("username")) {
			} else if (aKey.equalsIgnoreCase("endpoint"))
				this.addr = aValue;
			else if (aKey.equalsIgnoreCase("port"))
				this.port = Integer.parseInt(aValue);
			else if (aKey.equalsIgnoreCase("template")){
				this.template = aValue;
			}			
			else if (aKey.equalsIgnoreCase("ipmiParas")){
				this.ipmiParas = aValue;
			}
			else if (aKey.equalsIgnoreCase("cluster")){
				this.clusterName=aValue;
			}
			else if (aKey.equalsIgnoreCase("Name")){
				this.cloudName=aValue;
			}
			
		}
		ArrayList<String> dateR = socketToproxy("date -R");
		//if(dateR.get(0).split(regex))
		
		try {
			if(dateR!=null&&!dateR.isEmpty()) {
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return true;
	}

	public String getVmNames(String id)
	{
        String cmdLine = "onehostname "+id;
        //System.out.println(cmdLine);
		ArrayList<String> feedBack = socketToproxy(cmdLine);
		if(feedBack!=null&&!feedBack.isEmpty())
					return feedBack.get(0);
		else
		{
			System.out.println("[Error:] Cannot get hostname!");
			return null;
		}
	}
	
	@Override
	public ArrayList<Vm> createVM(int maxCount) {
		// TODO Auto-generated method stub
		String cmdLine="onetemplate instantiate "+template +" -m "+maxCount + " -n vcluster_worker";	
		System.out.println(cmdLine);
		ArrayList<Vm> vmList = new ArrayList<Vm>();
		ArrayList<String> feedBack = socketToproxy(cmdLine);
		if(feedBack!=null&&!feedBack.isEmpty()&&feedBack.get(0).contains("ID:")){
			for(int i = 0;i<feedBack.size();i++){
				
				String [] vmEle = feedBack.get(i).split("\\s+");
				Vm vm = new Vm();
				vm.setId(vmEle[1]);
				vm.setState(VMState.PROLOG);
				vm.setDNSName(getVmNames(vmEle[1]));
				vmList.add(vm);				
			}
		}else{
			System.out.println(feedBack.get(0));
			return null;
		}
			return vmList;
	}
	
	@Override
	public ArrayList<Vm> createVM(int maxCount, String hostID)
	{
		String cmdLine="onetemplate instantiate "+template +" -m "+maxCount + " -n vcluster_worker";	
		System.out.println(cmdLine);
		ArrayList<Vm> vmList = new ArrayList<Vm>();
		ArrayList<String> feedBack = socketToproxy(cmdLine);
		if(feedBack!=null&&!feedBack.isEmpty()&&feedBack.get(0).contains("ID:")){
			for(int i = 0;i<feedBack.size();i++){
				
				String [] vmEle = feedBack.get(i).split("\\s+");
				Vm vm = new Vm();
				vm.setId(vmEle[1]);
				vm.setState(VMState.PROLOG);
				vm.setDNSName(getVmNames(vmEle[1]));
				vm.setHostname(hostID);
				vmList.add(vm);				
			}
		}else{
			System.out.println(feedBack.get(0));
			return null;
		}
		for(int j=0;j<vmList.size();j++)
		{
			cmdLine="onevm deploy "+ vmList.get(j).getId()+" "+ hostID;
			socketToproxy(cmdLine);
		}
			return vmList;
	}
	
	private boolean getVminf(Vm vm){
		ArrayList<String> feedBack = socketToproxy("onevm show "+vm.getId()+" | grep IP");

		
		if(feedBack!=null&&!feedBack.isEmpty()){
			
			for(String str : feedBack){
					try {
						if(str.contains("IP_PUBLIC")){
							vm.setPubicIP(str.split("=")[1].replace(",", "").replaceAll("\"", "").trim());
						}else if(str.contains("IP=")){
							vm.setPrivateIP(str.split("=")[1].trim().replace(",", "").replaceAll("\"", ""));
						}
						else if(str.contains("IP_PRIVATE")){
							vm.setPrivateIP(str.split("=")[1].replace(",", "").replaceAll("\"", ""));
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						return false;
					}
				}
			return true;
		}
		return true;
	}

	@Override
	public ArrayList<Vm> listVMs() {
		// TODO Auto-generated method stub
		String cmdLine="onevm list|grep vcluster";
		ArrayList<Vm> vmList = new ArrayList<Vm>();
		ArrayList<String> feedBack = socketToproxy(cmdLine);
		boolean flag = true;
		while(flag){
		if(feedBack!=null&&!feedBack.isEmpty()){
			for(int i = 0;i<feedBack.size();i++){
				//System.out.println(feedBack.get(i));
				String [] vmEle = feedBack.get(i).split("\\s+");
				if(vmEle.length<9){
					continue;
				}
				Vm vm = new Vm();
				try{
					vm.setId(vmEle[0]);
					//vm.setState(vmEle[4]);
					getVminf(vm);		
					vm.setDNSName(getVmNames(vmEle[0]));
					vm.setUser(vmEle[1]);
					vm.setGroup(vmEle[2]);
					
					if(vmEle[4].equalsIgnoreCase("runn")){
						vm.setState(VMState.RUNNING);
					}else if(vmEle[4].equalsIgnoreCase("stop")){
						vm.setState(VMState.STOP);
					}else if(vmEle[4].equalsIgnoreCase("Pend")){
						vm.setState(VMState.PENDING);
					}else if(vmEle[4].equalsIgnoreCase("Prol")){
						vm.setState(VMState.PROLOG);
					}else if(vmEle[4].equalsIgnoreCase("Susp")){
						vm.setState(VMState.SUSPEND);
					}else{
						vm.setState(VMState.NOT_DEFINED);
					}
					vm.setUcpu(vmEle[5]);
					vm.setMemory(vmEle[6]);
					if(vm.getState()==VMState.STOP){vm.setHostname("");}else{
						vm.setHostname(vmEle[7]);
					}
				}catch(Exception e){
					e.printStackTrace();
					continue;
				}

				vmList.add(vm);		
				
			}
			flag = false;
		}else if(feedBack.get(0).contains("ReadTimeout")){
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("ReadTimeout, waiting 5s... ...");
			
		}else{
			System.out.println(feedBack.get(0));
			flag = false;
			return null;
		}
		}
		//System.out.println("opennebula plugin:"+vmList.size());
		return vmList;
	}

	@Override
	public ArrayList<Vm> destroyVM(Vm vm) {
		// TODO Auto-generated method stub
			
		String cmdLine = "./rmvm "+vm.getPrivateIP()+" "+vm.getId();
		ArrayList<Vm> vmList = new ArrayList<Vm>();
		ArrayList<String> feedBack = socketToproxy(cmdLine);
		if(feedBack!=null&&!feedBack.isEmpty()){
			System.out.println(feedBack.get(0));
			return null;
		}
		Vm vm_1 = new Vm();
		vm_1.setId(vm.getId());
		vm_1.setState(VMState.STOP);
		vmList.add(vm_1);
		return vmList; 
	}

	@Override
	public ArrayList<Vm> startVM(String id) {
		// TODO Auto-generated method stub
		String cmdLine="onevm resume "+id;
		ArrayList<Vm> vmList = new ArrayList<Vm>();
		ArrayList<String> feedBack = socketToproxy(cmdLine);
		if(feedBack!=null&&!feedBack.isEmpty()){
			System.out.println(feedBack.get(0));
			return null;
		}
		Vm vm = new Vm();
		vm.setId(id);
		vm.setState(VMState.PROLOG);
		vmList.add(vm);
		return vmList;
	}

	@Override
	public ArrayList<Vm> suspendVM(String id) {
		// TODO Auto-generated method stub
		String cmdLine = "onevm suspend "+id;
		ArrayList<Vm> vmList = new ArrayList<Vm>();
		ArrayList<String> feedBack = socketToproxy(cmdLine);
		if(feedBack!=null&&!feedBack.isEmpty()){
			System.out.println(feedBack.get(0));
			return null;
		}
		Vm vm = new Vm();
		vm.setId(id);
		vm.setState(VMState.SUSPEND);
		vmList.add(vm);
		return vmList;
	}

	



	@Override
	public boolean migrate(String vmid, String hostid) {
		// TODO Auto-generated method stub
		String cmdLine = "onevm migrate "+ vmid + " "+ hostid;
		
	    socketToproxy(cmdLine);
		
	    return true;
	}

	@Override
	public boolean hoston(String ipmiID) {
		// TODO Auto-generated method stub
		String cmdLine = "ipmitool" + ipmiParas + " -H "+ipmiID+ " power on";
		
	    ArrayList<String> feedback = socketToproxy(cmdLine);
	    if(feedback.get(0).equalsIgnoreCase("Chassis Power Control: Up/On")){
	    	
	    	return true;
	    }else{
	    	return false;
	    }
	}

	@Override
	public boolean hostoff(String ipmiID) {
		// TODO Auto-generated method stub
		String cmdLine = "ipmitool" + ipmiParas + " -H "+ipmiID+ " power off";
		
	    ArrayList<String> feedback = socketToproxy(cmdLine);
	    if(feedback.get(0).equalsIgnoreCase("Chassis Power Control: Down/Off")){
	    	return true;
	    }
		return false;
	}
    
	
    
    private String addr;
	private int port;
	private String template;
	private String clusterName;
	private String ipmiParas;
	private String cloudName;
	
	
	/*
	 * Added functions to list hosts machine information
	 * @see vcluster.plugInterfaces.CloudInterface#listHost()
	 */
    @Override
	public ArrayList<Host> listHost() {
		
		return getHosts();
	}
    
    public Document getXmlHost(){
    	Document doc = null;
		File f = new File("host.xml");
		String cmdLine2 ="onehost list -x";
		ArrayList<String> arr = socketToproxy(cmdLine2);
		try {
			FileWriter fw = new FileWriter(f);
			fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			for(String line : arr){
				//if (line.contains("MESSAGE"))
				//	continue;
				fw.write(line);
				
				//fw.flush();
			}
			fw.close();
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            //factory.setValidating(true);
			DocumentBuilder dBuilder = factory.newDocumentBuilder();
			doc = dBuilder.parse(f);
			
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return doc;
    }


    public ArrayList<Host> getHosts(){
    	
    	ArrayList<Host> hosts=new ArrayList<Host>();
		Document doc = getXmlHost();
		Element hostPool=(Element)doc.getElementsByTagName("HOST_POOL").item(0);
		NodeList nlHost =  hostPool.getElementsByTagName("HOST");
		for(int i = 0;i<nlHost.getLength();i++){
			if (!nlHost.item(i).getParentNode().getNodeName().equals("HOST_POOL"))
				continue;
			NodeList nlCluster=((Element)nlHost.item(i)).getElementsByTagName("TEMPLATE");
			nlCluster=nlCluster.item(0).getChildNodes();
			boolean cluster=false;
			String cName="";
			String hostName="";
			for(int k=0;k < nlCluster.getLength();k++)
			{
				if (nlCluster.item(k).getNodeName().equals("CLUSTER"))
				{
					cName=nlCluster.item(k).getFirstChild().getTextContent();
					if (!cName.equalsIgnoreCase(clusterName))
						break;
					cluster=true;
				}
				else if (nlCluster.item(k).getNodeName().equals("HOSTNAME"))
				{
					hostName=nlCluster.item(k).getFirstChild().getTextContent();
				}
			}
			
			if (!cluster) continue;
				
			Element e= (Element)((Element) nlHost.item(i)).getElementsByTagName("ID").item(0);
			String hId=e.getTextContent();
			e=(Element)((Element) nlHost.item(i)).getElementsByTagName("STATE").item(0);
			Host.HostStat hStat=getHostStat(e.getTextContent());
			Host tempHost=new Host(1,hId,hostName,this.cloudName);
			tempHost.setStat(hStat);
			NodeList attribute=((Element)nlHost.item(i)).getElementsByTagName("HOST_SHARE").item(0).getChildNodes();
			for(int j=0;j<attribute.getLength();j++)
			{
			   	e = (Element)((Element)attribute.item(j));
			   	if (e.getNodeName().equals("USED_MEM"))
			        tempHost.setAMEM(Double.parseDouble(e.getTextContent()));
			   	else if (e.getNodeName().equals("USED_CPU"))
			        tempHost.setACPU(Integer.parseInt(e.getTextContent()));
			   	else if (e.getNodeName().equals("MAX_MEM"))    
			        tempHost.setTMEM(Double.parseDouble(e.getTextContent()));
			    else if (e.getNodeName().equals("MAX_CPU"))    
			        tempHost.setTCPU(Integer.parseInt(e.getTextContent()));
			    else if (e.getNodeName().equals("FREE_MEM"))
			        tempHost.setFMEM(Double.parseDouble(e.getTextContent()));
			    else if (e.getNodeName().equals("FREE_CPU"))
			        tempHost.setFCPU(Integer.parseInt(e.getTextContent()));
			        	
			}
			        tempHost.setMaxVmNum(tempHost.getTCPU()/100);
			        hosts.add(tempHost);
		}

		return hosts;
	}
    
    public Host.HostStat getHostStat(String stat){
    	switch (stat){
    	case "2":
    		return Host.HostStat.ON;
    	case "3":
    		return Host.HostStat.ERR;
    	case "4":
    		return Host.HostStat.OFF;
    	default:
    		return Host.HostStat.UNKNOW;
    	}
    }
    

}
