package cn.how2j.diytomcat.util;

import cn.how2j.diytomcat.catalina.*;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class ServerXMLUtil {
    public static List<Connector> getConnectors(Service service) {
        List<Connector> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);

        Elements es = d.select("Connector");
        for (Element e : es) {
            int port = Convert.toInt(e.attr("port"));
            String compression = e.attr("compression");

            int compressionMinSize = Convert.toInt(e.attr("compressionMinSize"), 0);
            String noCompressionUserAgents = e.attr("noCompressionUserAgents");
            String compressableMimeType = e.attr("compressableMimeType");
            Connector c = new Connector(service);
            c.setPort(port);
            c.setCompression(compression);
            c.setCompressableMimeType(compressableMimeType);
            c.setNoCompressionUserAgents(noCompressionUserAgents);
            c.setCompressableMimeType(compressableMimeType);
            c.setCompressionMinSize(compressionMinSize);
            result.add(c);
        }
        return result;
    }

    //获取配置文件中的context
    public static List<Context> getContexts(Host host) {
        List<Context> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);
        //选择器
        Elements es = d.select("Context");
        for (Element e : es) {
            //获取访问路径
            String path = e.attr("path");
            //获取绝对路径
            String docBase = e.attr("docBase");

            boolean reloadable = Convert.toBool(e.attr("reloadable"), true);
            Context context = new Context(path, docBase, host, reloadable);
            result.add(context);
        }
        return result;
    }

    //通过engine获取默认虚拟主机
    public static String getEngineDefaultHost() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);

        Element host = d.select("Engine").first();
        return host.attr("defaultHost");
    }

    public static String getServiceName() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);

        Element host = d.select("Service").first();
        return host.attr("name");
    }

    public static List<Host> getHosts(Engine engine) {
        List<Host> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document d = Jsoup.parse(xml);

        Elements es = d.select("Host");
        for (Element e : es) {
            String name = e.attr("name");
            Host host = new Host(name,engine);
            result.add(host);
        }
        return result;
    }
}