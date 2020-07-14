package cn.how2j.diytomcat.util;

import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class ContextXMLUtil {

	public static String getWatchedResource() {
		try {
			String xml = FileUtil.readUtf8String(Constant.contextXmlFile);
			Document d = Jsoup.parse(xml);
			Element e = d.select("WatchedResource").first();
			return e.text();
		} catch (Exception e) {
			e.printStackTrace();
			return "WEB-INF/web.xml";
		}
	}
}
