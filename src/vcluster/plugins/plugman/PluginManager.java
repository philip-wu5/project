package vcluster.plugins.plugman;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import vcluster.plugins.BatchInterface;
import vcluster.plugins.CloudInterface;
import vcluster.ui.Command;
import vcluster.util.HandleXML;

/**
 *  This class is responsible for managing plugins,including loading plugins into the JVM,unloading plugins,maintaining a plugin list.
 * @author kjwon15 modified by Dada Huang
 * 
 */
public class PluginManager {

	public static BatchInterface current_proxyExecutor;
	//private static CloudInterface current_cloudExecutor;
	public static final String CLOUD_PLUGIN_DIR = "plugins"+File.separator+"cloud";
	public static final String BATCH_PLUGIN_DIR = "plugins"+File.separator+"batch";	
	
	public static TreeMap<String,Plugin> pluginList;
	//public static Map<String, BatchInterface> loadedBatchPlugins = new HashMap<String, BatchInterface>();
	//public static Map<String, CloudInterface> loadedCloudPlugins = new HashMap<String, CloudInterface>();
	private static CustomClassLoader cl;//An instance of CustomClassLoader,it is responsible for loading classes form jar files.

	
	static {
		// TODO Auto-generated constructor stub
		cl = new CustomClassLoader();
		pluginList = new TreeMap<String,Plugin>();
		getCloudPluginList();
		getBatchPluginList();		
	}

	/**
	 * Loads a specified plugin(JAR file) into the JVM from the plugin directory. 
	 * @param name JAR file name without the extension ".jar",case sensitive.
	 * @throws ClassNotFoundException
	 */
	public static void LoadPlugin(String path,String type) throws ClassNotFoundException {
		File f = new File(path);
		String name = f.getName().replace(".jar", "");		
		//System.out.println(path);
		if (isLoaded(name)) {
				System.out.println(name + " : has already been loaded!");
				return;
			}
			Class<?> plugin = cl.loadClass(path);
			if(Command.TYPE_CLOUD.contains(type)){
				try {							
					CloudInterface ce = (CloudInterface) plugin.newInstance();
					pluginList.get(name).setInstance(ce);
					pluginList.get(name).setPluginStatus("loaded");
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}				
				
			}else if(Command.TYPE_BATCH.contains(type)){
						try {						
							for(Plugin pli : pluginList.values()){
								if(pli.getPluginType().equals("batch")&&pli.getPluginStatus().equals("loaded")){
									UnloadPlugin(pli.getPluginName());
								}
							}
							BatchInterface pe = (BatchInterface) plugin.newInstance();
							current_proxyExecutor = pe;
							pluginList.get(name).setInstance(pe);
							pluginList.get(name).setPluginStatus("loaded");
							HandleXML.loadBatchPlugin(name);
						} catch (InstantiationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

			}
	}

		
	/**
	 * Loads all plugins from the plugin directory.
	 * @see #LoadPlugin(String)
	 */
	public static void LoadAllCloudPlugins() {
		File dir = new File(System.getProperty("user.dir") + File.separator
				+ CLOUD_PLUGIN_DIR);
		List<String> list = getCloudPluginList();
		for (String filename : list) {
			try {
				String name = filename.substring(dir.toString().length() + 1,
						filename.indexOf(".")).replace(File.separatorChar, '.');
				LoadPlugin(name,"-c");
			}catch (ClassNotFoundException e) {
				System.err.println(filename
						+ " does not contain valid class data");
			}
		}
	}

	
	/**
	 * Scan the plugins directiory get all the paths of JAR file
	 * @see #LoadAllPlugins() 
	 * @return a list of paths.
	 */
	public static List<String> getCloudPluginList(){
		File dir = new File(System.getProperty("user.dir") + File.separator
				+ CLOUD_PLUGIN_DIR);
		File f = new File(dir.getPath());
		File[] list = f.listFiles();
		List<String> jars = new ArrayList<String>();
			for (File file : list) {				
					if (file.getPath().endsWith(".jar")) {
						Plugin pi = new Plugin();
						String name = file.getName().replace(".jar", "");
						String status = "unloaded";
						jars.add(name);
						pi.setPluginName(name);
						pi.setPluginStatus(status);
						pi.setPluginType("cloud");
						pluginList.put(name, pi);
				}
			}
		return jars;				
	}
	
	public static List<String> getBatchPluginList(){

		File dir = new File(System.getProperty("user.dir") + File.separator
				+ BATCH_PLUGIN_DIR);
		File f = new File(dir.getPath());
		File[] list = f.listFiles();
		List<String> jars = new ArrayList<String>();
		if(list.length>0){
			for (File file : list) {
				
				if (file.getPath().endsWith(".jar")) {
					Plugin pi = new Plugin();
					String name = file.getName().replace(".jar", "");
					String status="unloaded";
					jars.add(name);
					pi.setPluginName(name);
					pi.setPluginStatus(status);
					pi.setPluginType("batch");
					pluginList.put(name, pi);					
				}
			}
		}
		return jars;				
	}	

	/**
	 * Unload a specified plugin.remove the plugin form the plugins collection 
	 * @param name a plugin name to be unloaded.
	 * @throws ClassNotFoundException
	 */
	public static void UnloadPlugin(String name) throws ClassNotFoundException {
		if (!isLoaded(name)) {
			throw new ClassNotFoundException("Plugin didn't loaded: " + name);
		}
		pluginList.get(name).setInstance(null);
		pluginList.get(name).setPluginStatus("unloaded");
		if(getBatchPluginList().contains(name)){
			HandleXML.unloadBatchPlugin();
		}
	}

	
	public static String getInfo(String pluginName) {
		// TODO Auto-generated method stub
		StringBuffer info =new StringBuffer("");
		String path = "";
		if(getCloudPluginList().contains(pluginName))path=CLOUD_PLUGIN_DIR;
		if(getBatchPluginList().contains(pluginName))path=BATCH_PLUGIN_DIR;
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(path+File.separator+pluginName+"Readme.txt")));
			String aLine = "";
			while ((aLine = br.readLine()) != null) {				
				System.out.println(aLine);
			}
			br.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Readme file doesn't exist ,please check it on the plugin directory!");
		}
		
		return info.toString();
	}
	
	public static boolean isLoaded(String name){
		
		if (pluginList.get(name).getPluginStatus().equals("loaded")) {
			return true;
		}else{
			return false;
		}		
	}
}