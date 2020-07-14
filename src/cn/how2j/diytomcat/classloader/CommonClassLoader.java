package cn.how2j.diytomcat.classloader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class CommonClassLoader extends URLClassLoader {

	public CommonClassLoader() {
		super(new URL[] {});

		try {
			File workingFolder = new File(System.getProperty("user.dir"));
			File libFolder = new File(workingFolder, "lib");
			//获取所有的jar文件
			File[] jarFiles = libFolder.listFiles();
			//遍历所有的jar文件加到当前库中
			for (File file : jarFiles) {
				if (file.getName().endsWith("jar")) {
					URL url = new URL("file:" + file.getAbsolutePath());
					this.addURL(url);
				}
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
