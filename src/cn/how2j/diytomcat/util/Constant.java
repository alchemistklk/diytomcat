package cn.how2j.diytomcat.util;

import cn.hutool.system.SystemUtil;

import java.io.File;

public class Constant {
    public static final int CODE_200 = 200;
    public static final int CODE_302 = 302;
    public static final int CODE_404 = 404;
    public static final int CODE_500 = 500;

    //存储http的响应头信息
    public static final String response_head_200 =
            "HTTP/1.1 200 OK\r\n" +
            "Content-Type: {}{}" +
            "\r\n\r\n";

    public static final String response_head_200_gzip =
            "HTTP/1.1 200 OK\r\nContent-Type: {}{}\r\n" +
            "Content-Encoding:gzip" +
            "\r\n\r\n";
    public static final String response_head_302 = "HTTP/1.1 302 Found\r\nLocation: {}\r\n\r\n";

    public static final String response_head_404 =
            "HTTP/1.1 404 Not Found\r\n" +
            "Content-Type: text/html\r\n\r\n";

    public static final String response_head_500 = "HTTP/1.1 500 Internal Server Error\r\n"
            + "Content-Type: text/html\r\n\r\n";

    public static final String textFormat_404 =
            "<html><head><title>DIY Tomcat/1.0.1 - Error report</title><style>" +
            "<!--H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} " +
            "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} " +
            "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} " +
            "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} " +
            "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} " +
            "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}" +
            "A {color : black;}A.name {color : black;}HR {color : #525D76;}--></style> " +
            "</head><body><h1>HTTP Status 404 - {}</h1>" +
            "<HR size='1' noshade='noshade'><p><b>type</b> Status report</p><p><b>message</b> <u>{}</u></p><p><b>description</b> " +
            "<u>The requested resource is not available.</u></p><HR size='1' noshade='noshade'><h3>DiyTocmat 1.0.1</h3>" +
            "</body></html>";



    public static final String textFormat_500 = "<html><head><title>DIY Tomcat/1.0.1 - Error report</title><style>"
            + "<!--H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} "
            + "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} "
            + "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} "
            + "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} "
            + "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} "
            + "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}"
            + "A {color : black;}A.name {color : black;}HR {color : #525D76;}--></style> "
            + "</head><body><h1>HTTP Status 500 - An exception occurred processing {}</h1>"
            + "<HR size='1' noshade='noshade'><p><b>type</b> Exception report</p><p><b>message</b> <u>An exception occurred processing {}</u></p><p><b>description</b> "
            + "<u>The server encountered an internal error that prevented it from fulfilling this request.</u></p>"
            + "<p>Stacktrace:</p>" + "<pre>{}</pre>" + "<HR size='1' noshade='noshade'><h3>DiyTocmat 1.0.1</h3>"
            + "</body></html>";
    public final static File webappsFolder = new File(SystemUtil.get("user.dir"),"webapps");
    public final static File rootFolder = new File(webappsFolder,"ROOT");

    //配置文件根目录
    public static final File confFolder = new File(SystemUtil.get("user.dir"),"conf");
    //配置xml文件
    public static final File serverXmlFile = new File(confFolder, "server.xml");

    public static final File webXmlFile = new File(confFolder, "web.xml");
    //contextxml文件
    public static final File contextXmlFile = new File(confFolder, "context.xml");

    //按照Tomcat逻辑，当jsp转译后会保存到%TOMCAT_HOME%/work目录下
    public static final String workFolder = SystemUtil.get("user.dir") + File.separator + "work";
}
