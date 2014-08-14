package vcluster.managers;

public class LoadManager {

	private static boolean load;
	private static boolean start;
	static{
		load=false;
		start=false;
	}
	public static boolean isLoad() {
		return load;
	}
	public static void setLoad(boolean load) {
		LoadManager.load = load;
	}
	public static boolean isStart() {
		return start;
	}
	public static void setStart(boolean start) {
		LoadManager.start = start;
	}
	
}
