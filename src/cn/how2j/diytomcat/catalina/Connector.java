package cn.how2j.diytomcat.catalina;
 
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
 
import cn.how2j.diytomcat.http.Request;
import cn.how2j.diytomcat.http.Response;
import cn.how2j.diytomcat.util.ThreadPoolUtil;
import cn.hutool.log.LogFactory;

//接受Socket请求
public class Connector implements Runnable {
    int port;
    private Service service;

    //压缩文件的相关信息
    private String compression;
    private int compressionMinSize;
    private String noCompressionUserAgents;
    private String compressableMimeType;
    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public int getCompressionMinSize() {
        return compressionMinSize;
    }

    public void setCompressionMinSize(int compressionMinSize) {
        this.compressionMinSize = compressionMinSize;
    }

    public String getNoCompressionUserAgents() {
        return noCompressionUserAgents;
    }

    public void setNoCompressionUserAgents(String noCompressionUserAgents) {
        this.noCompressionUserAgents = noCompressionUserAgents;
    }

    public String getCompressableMimeType() {
        return compressableMimeType;
    }

    public void setCompressableMimeType(String compressableMimeType) {
        this.compressableMimeType = compressableMimeType;
    }
    public Connector(Service service) {
        this.service = service;
    }
 
    public Service getService() {
        return service;
    }
 
    public void setPort(int port) {
        this.port = port;
    }
 
    @Override
    public void run() {
        try {
            ServerSocket ss = new ServerSocket(port);
            while(true) {
                Socket s = ss.accept();
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Request request = new Request(s, Connector.this);
                            Response response = new Response();
                            HttpProcessor processor = new HttpProcessor();
                            processor.execute(s,request, response);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if (!s.isClosed())
                            try {
                                s.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                ThreadPoolUtil.run(r);
            }

        } catch (IOException e) {
            LogFactory.get().error(e);
            e.printStackTrace();
        }
    }
 
    public void init() {
        LogFactory.get().info("Initializing ProtocolHandler [http-bio-{}]",port);
    }
 
    public void start() {
        LogFactory.get().info("Starting ProtocolHandler [http-bio-{}]",port);
        new Thread(this).start();
    }
 

}