/*
 * Hao: Need a function to deregister the slots in condor pool, 
 *      Command: condor_off hostname
 */

package vcluster.plugin.proxy.fermihtcondor;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import vcluster.Vcluster;
import vcluster.elements.PoolStatus;
import vcluster.elements.QStatus;
import vcluster.elements.Slot;
import vcluster.elements.Job;
import vcluster.managers.CloudManager;

public class CheckCondor {


	private ArrayList<Slot> slots;
	private QStatus q;
	private PoolStatus pool;
	private ArrayList<Job> jobs;
	private String addr;
	private int port;
	
	
	
	public CheckCondor() {
		this.slots = new ArrayList<Slot> ();
		this.jobs = new ArrayList<Job> ();
		this.addr = Vcluster.CONDOR_IPADDR;
		this.port = Vcluster.PORTNUM;
		//setQ();
		//setPool();
		//System.out.println("condor instance has generated!");
	}


	public QStatus getQ() {
		this.setQ();
		q.setJobs(jobs);
		this.setJobHost();
		return q;
	}


	public PoolStatus getPool() {
	//	this.setPool();
		pool = new PoolStatus();
		getSlots();
		pool.setSlotList(slots);
		int claimed=0;
		int unclaimed=0;
		int total = pool.getSlotList().size();
		for(Slot s : pool.getSlotList()){
			if(s.getActivity()==1)claimed++;
		}
		unclaimed = total - claimed;
		pool.setTotalSlot(total);
		pool.setUnClaimedSlot(unclaimed);
		pool.setClaimedSlot(claimed);
		return pool;
	}


	public void setQ() {
		
		ArrayList<String> status = socketToproxy("condor_q");
		for(String str : status){
	        try {
	        	if(str.contains("Submitter")||str.contains("OWNER")||str.equals(""))
	        		continue;
    	        if (str.contains("jobs")) {
    	        	extractQInfo(str);
    	        }
    	        else
    	        	extractJobInfo(str);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				continue;
			}
		}
	}

	public void setJobHost(){
		ArrayList<String> status = socketToproxy("condor_q -run");
		for(String str : status){
	        try {
	        	if(str.contains("Submitter")||str.contains("OWNER")||str.equals(""))
	        		continue;
    	        extractJobHost(str);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				continue;
			}
		}
	}	

	public boolean extractJobHost(String str){
StringTokenizer st = new StringTokenizer(str, ";, ");
		
		String jId="";
	    String hostName="";
		
		String token = null;
		
		if (!st.hasMoreTokens()) {
    		System.out.println("ERROR: no more token.");
			return false;
		}
		
		/* get job ID */
		token = st.nextToken();

		jId=token;
				
		if (!st.hasMoreTokens()) {
    		System.out.println("ERROR: no more token");
			return false;
		}

		/* get owner */
		token = st.nextToken();
				
		if (!st.hasMoreTokens()) {
    		System.out.println("ERROR: no more token");
			return false;
		}

		/* get submitted time */
		token = st.nextToken();

		if (!st.hasMoreTokens()) {
    		System.out.println("ERROR: no more token");
			return false;
		}
		
		token = st.nextToken();
		
		if (!st.hasMoreTokens()) {
    		System.out.println("ERROR: no more token");
			return false;
		}
		
		/* get RUN Time */
		token = st.nextToken();
		
		if (!st.hasMoreTokens()) {
    		System.out.println("ERROR: no more token");
			return false;
		}
		
		/* get HostName */
		token = st.nextToken();

		hostName = token;
				
		if(!q.setJobHost(jId, hostName))
		{
			System.out.println("[ERROR: ] CANNOT FIND HOST FOR RUNNING JOB");
			return false;
		}
		return true;
	}
	private String getDomain(String domainStr){
		String domain="";
		for(String cn : CloudManager.getCloudList().keySet()){
			if(domainStr.toLowerCase().contains(cn.toLowerCase())){
				return cn;
			}
		}
		return domain;
	}
	public void setPool() {
		String cmd = "condor_status -total";
		ArrayList<String> status = socketToproxy(cmd);
		
		for(String str : status){
	        try {
				if (str.contains("Total")&&!str.contains("Owner")){
					this.extractPoolInfo(str);
					//System.out.println("hanlding pool extraction");
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				continue;
			}
		}
	}


	public boolean extractJobInfo(String qStatus)
	{
        StringTokenizer st = new StringTokenizer(qStatus, ";, ");
		
		Job j=new Job();
		
		String token = null;
		
		if (!st.hasMoreTokens()) {
    		System.out.println("ERROR: no more token.");
			return false;
		}
		
		/* get job ID */
		token = st.nextToken();

		j.setID(token);
				
		if (!st.hasMoreTokens()) {
    		System.out.println("ERROR: no more token");
			return false;
		}

		/* get owner */
		token = st.nextToken();

		j.setOwner(token);
				
		if (!st.hasMoreTokens()) {
    		System.out.println("ERROR: no more token");
			return false;
		}

		/* get submitted time */
		token = st.nextToken();

		if (!st.hasMoreTokens()) {
    		System.out.println("ERROR: no more token");
			return false;
		}
		token=token + " " + st.nextToken();		
		j.setSubmit(token);
		if (!st.hasMoreTokens()) {
    		System.out.println("ERROR: no more token");
			return false;
		}
		
		/* get RUN Time */
		token = st.nextToken();

		j.setRunT(token);
				
		if (!st.hasMoreTokens()) {
    		System.out.println("ERROR: no more token");
			return false;
		}
		
		/* get Status */
		token = st.nextToken();

		j.setStat(token);
				
		if (!st.hasMoreTokens()) {
    		System.out.println("ERROR: no more token");
			return false;
		}
		
		/* get priority  */
		token = st.nextToken();

		j.setPri(token);
				
		if (!st.hasMoreTokens()) {
    		System.out.println("ERROR: no more token");
			return false;
		}
		
		/* get size */
		token = st.nextToken();

		j.setSize(token);
				
		if (!st.hasMoreTokens()) {
    		System.out.println("ERROR: no more token");
			return false;
		}
		jobs.add(j);
		return true;
	}

	public boolean extractQInfo(String qStatus)
	{
		StringTokenizer st = new StringTokenizer(qStatus, ";, ");
		
		int numJob = 0;
		String token = null;
		
		if (!st.hasMoreTokens()) {
			System.out.println("ERROR: no more token.");
			return false;
		}
		
		/* get total jobs */
		token = st.nextToken();
	
		numJob = Integer.parseInt(token);
		if (!st.hasMoreTokens()) return false;
		if (!st.nextToken().equalsIgnoreCase("jobs")) {
			System.out.println("ERROR: token, jobs, expected, but not");
			return false;
		}
		int totalJob = numJob;
		
		if (!st.hasMoreTokens()) {
			System.out.println("ERROR: no more token");
			return false;
		}
	
		/* get completed jobs */
		token = st.nextToken();
	
		numJob = Integer.parseInt(token);
	
		if (!st.hasMoreTokens()) return false;
		if (!st.nextToken().equalsIgnoreCase("completed")) {
			System.out.println("ERROR: token, completed, expected, but not");
			return false;
		}
		int completedJob = numJob;
	
	
		
		if (!st.hasMoreTokens()) {
			System.out.println("ERROR: no more token");
			return false;
		}
		
		/* get removed jobs */
		token = st.nextToken();
	
		numJob = Integer.parseInt(token);
	
		if (!st.hasMoreTokens()) return false;
		if (!st.nextToken().equalsIgnoreCase("removed")) {
			System.out.println("ERROR: token, removed, expected, but not");
			return false;
		}
		int removedJob = numJob;
		
		if (!st.hasMoreTokens()) {
			System.out.println("ERROR: no more token");
			return false;
		}
		
		/* get idle jobs */
		token = st.nextToken();
	
		numJob = Integer.parseInt(token);
	
		if (!st.hasMoreTokens()) return false;
		if (!st.nextToken().equalsIgnoreCase("idle")) {
			System.out.println("ERROR: token, idle, expected, but not");
			return false;
		}
		int idleJob = numJob;
		
		if (!st.hasMoreTokens()) {
			System.out.println("ERROR: no more token");
			return false;
		}
	
		/* get running jobs */
		token = st.nextToken();
		numJob = Integer.parseInt(token);
	
		if (!st.hasMoreTokens()) return false;
		if (!st.nextToken().equalsIgnoreCase("running")) {
			System.out.println("ERROR: token, running, expected, but not");
			return false;
		}
		int runningJob = numJob;
		
		if (!st.hasMoreTokens()) {
			System.out.println("ERROR: no more token");
			return false;
		}
	
		/* get held jobs */
		token = st.nextToken();
		numJob = Integer.parseInt(token);
	
		if (!st.hasMoreTokens()) return false;
		if (!st.nextToken().equalsIgnoreCase("held")) {
			System.out.println("ERROR: token, held, expected, but not");
			return false;
		}
		int heldJob = numJob;
		
		if (!st.hasMoreTokens()) {
			System.out.println("ERROR: no more token");
			return false;
		}
		
		/* get suspended jobs */
		token = st.nextToken();
		numJob = Integer.parseInt(token);
	
		if (!st.hasMoreTokens()) return false;
		if (!st.nextToken().equalsIgnoreCase("suspended")) {
			System.out.println("ERROR: token, held, expected, but not");
			return false;
		}
		int suspendedJob = numJob;
		
		/* check the number of jobs */
		numJob = idleJob + runningJob + heldJob;
		q = new QStatus(totalJob, completedJob, removedJob, idleJob, runningJob, heldJob, suspendedJob);
		if (totalJob != numJob) {
			System.out.println("ERROR: Number of jobs does not mache, total="
					+totalJob+", sum="+numJob);
			return false;
		}
	
		return true;
	}

	public boolean getSlots(){
		Document doc = getXmlPool();
		Element classads = (Element) doc.getElementsByTagName("classads").item(0);
		NodeList nl = classads.getElementsByTagName("c");
		for(int i = 0;i<nl.getLength();i++){
			Slot s = new Slot();
			Element e = (Element) nl.item(i);
			NodeList nolst = e.getElementsByTagName("a");			
			for(int j = 0;j<nolst.getLength();j++){
				Element ele = (Element) nolst.item(j);
				String eleName = ele.getAttribute("n");
				Element subele = (Element) ele.getElementsByTagName("s").item(0);
				if(subele==null)continue;
				//subele.get
				String value = subele.getTextContent();				
				if(eleName.equalsIgnoreCase("Activity")){					
					if(value.equalsIgnoreCase("Idle")){
						s.setActivity(0);
					}else{
						s.setActivity(1);
					}
				}else if(eleName.equalsIgnoreCase("Name")){
					s.setHostName(value);
					String domain = getDomain(value);
					//System.out.println(value);
					s.setDomain(domain);					
				}else if(eleName.equalsIgnoreCase("MyAddress")){
					String regexString=".*(\\d{3}(\\.\\d{1,3}){3}).*";
					String ip = value.replaceAll(regexString,"$1");
					//System.out.println("ip");
					s.setIdentifier(ip);
				}else if(eleName.equalsIgnoreCase("MyType")){
					//System.out.println(value);
				}				
			}
			s.setIdType(Slot.IdType.PRIVATEIP);
			slots.add(s);
		}
		
		return true;
		
	}
	
	public boolean extractPoolInfo(String poolStatus)
	{
		StringTokenizer st = new StringTokenizer(poolStatus, " \t");
		
		int numSlot = 0;
		String token = null;
		
		if (!st.hasMoreTokens()) {
			System.out.println("[Error] : no more token");
			return false;
		}
		
		/*
		 * 
		 *         Total Owner Claimed Unclaimed Matched Preempting Backfill
		 *
		 *   Total    16     2       0        14       0          0        0
		 */
	
		/* skip token Total */
		token = st.nextToken();
		// System.out.println("token = "+token);
	
		if (!st.hasMoreTokens()) return false;
	
		/* get total */
		token = st.nextToken();
		//System.out.println("Token = "+token);
		numSlot = Integer.parseInt(token);
		int totalSlot = numSlot;
		if (!st.hasMoreTokens()) return false;
	
		/* get owner */
		token = st.nextToken();
		//System.out.println("Token = "+token);
		numSlot = Integer.parseInt(token);
		int ownerSlot = numSlot;
		if (!st.hasMoreTokens()) return false;
		
	
		/* get claimed */
		token = st.nextToken();
		//System.out.println("Token = "+token);
		numSlot = Integer.parseInt(token);
		int claimedSlot = numSlot;
		if (!st.hasMoreTokens()) return false;
	
	
		/* get unclaimed */
		token = st.nextToken();
		//System.out.println("Token = "+token);
		numSlot = Integer.parseInt(token);
		int unClaimedSlot = numSlot;
		if (!st.hasMoreTokens()) return false;
		
	
		/* get matched */
		token = st.nextToken();
		//System.out.println("Token = "+token);
		numSlot = Integer.parseInt(token);
		int matchedSlot = numSlot;
		if (!st.hasMoreTokens()) return false;
		
	
		/* get preempting */
		token = st.nextToken();
		//System.out.println("Token = "+token);
		numSlot = Integer.parseInt(token);
		int preemptingSlot = numSlot;
		if (!st.hasMoreTokens()) return false;
	
	
		/* get backfill */
		token = st.nextToken();
		//System.out.println("Token = "+token);
		numSlot = Integer.parseInt(token);
		int backfillSlot = numSlot;
		//if (!st.hasMoreTokens()) return false;

		
		/* check the number of jobs */
		numSlot = ownerSlot + claimedSlot + unClaimedSlot + matchedSlot + preemptingSlot + backfillSlot;
		pool = new PoolStatus(totalSlot, ownerSlot, claimedSlot, unClaimedSlot, matchedSlot, preemptingSlot, backfillSlot);
		if (totalSlot != numSlot) {
			System.out.println("[Error] : Number of jobs does not mache");
			System.out.println("        : Total = "+totalSlot+", Sum = "+numSlot);
			return false;
		}

		return true;
	}
	
	/*
	 * Function to deregister workers from the condor pool
	 */
	public boolean deregisterWorker(String dnsName){
		ArrayList<String> status = socketToproxy("condor_off "+dnsName);
		if(status.get(0).contains("Sent"))
			return true;
		else
			return false;
	}
	
	private ArrayList<String> socketToproxy(String cmd){
		String cmdLine=cmd+"\n";
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
	           // out.flush();
	           // out.close();
	            //char[] cbuf = new char[1024];
	        	//String temp = null;
	        	String str=null;
	        	while((str=in.readLine())!=null)
	        	{
	        		str=str.trim();
	        		feedBack.add(str);
	        	}
	        	/*while (in.read(cbuf, 0, 1024) != -1) {
	            	//String str = new String(cbuf);
	            	//str.replaceAll("\u0000", " ");
	    	        //str = str.trim();
	    	        String str=trimNullChar(cbuf);
	    	        str=str.trim();
	    	        if (!str.equals(temp)){
	    	        	//System.out.println(str);
	    	        	 feedBack.add(str);
	    	        }
	    	        
	    	       // cbuf[0] = '\0';
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
	private Document getXmlPool(){
		Document doc = null;
	
		File f = new File("condor.xml");
		/*try {
			FileReader fr=new FileReader(f);
			char[] buffer =new char[512];
			fr.read(buffer);
			for(char c:buffer){
			System.out.printf("U+%04x ", (int) c);
			}
			fr.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		//String cmdLine2 ="condor_status -xml -attributes Name,Activity,MyAddress";
		String cmdLine2 ="condor_status -xml";
		ArrayList<String> arr = socketToproxy(cmdLine2);
		try {
			FileWriter fw = new FileWriter(f);
			for(String line : arr){
				//line=line.trim();
				fw.write(line);
				fw.flush();
			}
			fw.close();
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(true);
			doc = factory.newDocumentBuilder().parse(f);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return doc;
	}
	private void closeStream(BufferedReader in, DataOutputStream out, Socket socket)
	{
		try {
	        if (in != null) in.close();
	        if (out != null) out.close();
	        if (socket != null) socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Remove all the null chars in a string
	 */
	private String trimNullChar(String s){
		StringBuilder sb=new StringBuilder();
		for(char c:s.toCharArray())
		{
			if(c!='\0')
				sb.append(c);
		}
		return sb.toString();
	}
	private String trimNullChar(char [] cArray){
		StringBuilder sb=new StringBuilder();
		for(char c:cArray)
		{
			if(c!='\0')
				sb.append(c);
		}
		return sb.toString();
	}
}
