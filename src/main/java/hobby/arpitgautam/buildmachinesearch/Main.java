package hobby.arpitgautam.buildmachinesearch;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class Main {

	public static void main(String... s) throws Exception {
		Invoker iv = new Invoker();
		// load properties here
		FileInputStream fis = new FileInputStream(new File(
				"prop/finder.properties"));
		Properties prop = new Properties();
		prop.load(fis);
		String ips = prop.getProperty("iprange");
		if (ips == null) {
			throw new Exception(
					"Please define iprange property of format 10.148.220.1/19 in props/finder.properties");
		}
		iv.setProperties(prop);
		iv.start(ips);
		fis.close();
	}

}
