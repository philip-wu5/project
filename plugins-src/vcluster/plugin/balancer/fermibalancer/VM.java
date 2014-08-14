package vcluster.plugin.balancer.fermibalancer;



public class VM {
	public static enum VmStat{RUNNING,PENDING,PROLOG,UNKNOWN,DELETED,SCHEDULE}
	private String id;
	private String hostName;
	private VmStat status;
	private long startTime;
	private long actualStartTime;
	private int bootTime;
	private int realBootTime;
	private String realHostName;
	private String hostId;
	private String dnsName;
	private int waitTime;
	private VmLaunchOverheadModel overhead;
	
	public VM(){
		setStartTime(System.currentTimeMillis()/1000*1000);
		actualStartTime=startTime;
		waitTime=0;
		setStatus(VmStat.PENDING);
		setOverhead(new VmLaunchOverheadModel());
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public VmStat getStatus() {
		return status;
	}

	public void setStatus(VmStat status) {
		this.status = status;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public int getBootTime() {
		return bootTime;
	}

	public void setBootTime(int bootTime) {
		this.bootTime = bootTime;
	}

	public VmLaunchOverheadModel getOverhead() {
		return overhead;
	}

	public void setOverhead(VmLaunchOverheadModel overhead) {
		this.overhead = overhead;
	}

	public String getHostId() {
		return hostId;
	}

	public void setHostId(String hostId) {
		this.hostId = hostId;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public int getRealBootTime() {
		return realBootTime;
	}

	public void setRealBootTime(int realBootTime) {
		this.realBootTime = realBootTime;
	}

	public String getRealHostName() {
		return realHostName;
	}

	public void setRealHostName(String realHostName) {
		this.realHostName = realHostName;
	}

	public String getDnsName() {
		return dnsName;
	}

	public void setDnsName(String dnsName) {
		this.dnsName = dnsName;
	}

	public void setWaitTime(int t){
		waitTime=t;
	}
	public int getWaitTime(){
		return waitTime;
	}

	public long getActualStartTime() {
		return actualStartTime;
	}

	public void setActualStartTime(long actualStartTime) {
		this.actualStartTime = actualStartTime;
	}
}
