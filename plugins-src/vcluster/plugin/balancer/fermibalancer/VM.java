package vcluster.plugin.balancer.fermibalancer;



public class VM {
	public static enum VmStat{RUNNING,PENDING,PROLOG,UNKNOWN}
	private String id;
	private VmStat status;
	private long startTime;
	private int bootTime;
	private String hostId;
	private VmLaunchOverheadModel overhead;
	
	public VM(){
		setStartTime(System.currentTimeMillis());
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

}
