package cn.how2j.diytomcat.util;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


import cn.hutool.http.HttpUtil;

/**
 * 迷你浏览器
 * 1. 模拟发送http请求
 * 2. 获取完整的http响应
 *      1. 获取html信息
 *      2. 获取完整的http响应信息
 * 3. 方法
 *      1. getHttpBytes:返回二进制http的响应
 *      2. getHttpString:返回字符串的http向响应 - 将字节数组转化为字符串
 *      3. getContentBytes:返回二进制的http响应内容(去掉头的html部分)
 *      4. getContentString:返回字符串的http响应内容(去掉头的html部分) - 将字节数组转化为字符串
 */
public class MiniBrowser {

    public static void main(String[] args) throws Exception {
        String url = "http://static.how2j.cn/diytomcat.html";
        String contentString= getContentString(url,false);
        System.out.println(contentString);
        String httpString= getHttpString(url,false);
        System.out.println(httpString);
    }

    //根据传入的url和变量以及请求方式来获取响应内容的字节数组(去掉响应头的html信息)
    public static byte[] getContentBytes(String url, Map<String,Object> params, boolean isGet) {
        return getContentBytes(url, false,params,isGet);
    }

    //根据传入的url以及是否为压缩文件来获取响应内容的字节数组
    public static byte[] getContentBytes(String url, boolean gzip) {
        return getContentBytes(url, gzip,null,true);
    }
    //同理，根据url获取响应内容的字节数组
    public static byte[] getContentBytes(String url) {
        return getContentBytes(url, false,null,true);
    }


    public static String getContentString(String url, Map<String,Object> params, boolean isGet) {
        return getContentString(url,false,params,isGet);
    }

    public static String getContentString(String url, boolean gzip) {
        return getContentString(url, gzip, null, true);
    }

    public static String getContentString(String url) {
        return getContentString(url, false, null, true);
    }

    public static String getContentString(String url, boolean gzip, Map<String,Object> params, boolean isGet) {

        byte[] result = getContentBytes(url, gzip,params,isGet);
        if(null==result)
            return null;
        try {
            return new String(result,"utf-8").trim();
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public static byte[] getContentBytes(String url, boolean gzip, Map<String,Object> params, boolean isGet) {
        //获取相应同内容
        byte[] response = getHttpBytes(url,gzip,params,isGet);
        //双回车相当于分割线
        byte[] doubleReturn = "\r\n\r\n".getBytes();
        int pos = -1;
        for (int i = 0; i < response.length-doubleReturn.length; i++) {
            //
            byte[] temp = Arrays.copyOfRange(response, i, i + doubleReturn.length);

            if(Arrays.equals(temp, doubleReturn)) {
                pos = i;
                break;
            }
        }
        if(-1==pos)
            return null;
        //pos双分割线的起始位置
        pos += doubleReturn.length;

        //pos,response.length是html的内容
        byte[] result = Arrays.copyOfRange(response, pos, response.length);
        return result;
    }

    public static String getHttpString(String url,boolean gzip) {
        return getHttpString(url, gzip, null, true);
    }

    public static String getHttpString(String url) {
        return getHttpString(url, false, null, true);
    }

    public static String getHttpString(String url,boolean gzip, Map<String,Object> params, boolean isGet) {
        byte[]  bytes=getHttpBytes(url,gzip,params,isGet);
        return new String(bytes).trim();
    }

    public static String getHttpString(String url, Map<String,Object> params, boolean isGet) {
        return getHttpString(url,false,params,isGet);
    }

    //将响应内容转化为字节数组
    public static byte[] getHttpBytes(String url,boolean gzip, Map<String,Object> params, boolean isGet) {
        //先判断请求方式
        String method = isGet?"GET":"POST";
        byte[] result = null;
        try {
            URL u = new URL(url);
            Socket client = new Socket();
            int port = u.getPort();
            if(-1==port)
                port = 80;
            //该方法实现了IP套接字地址
            InetSocketAddress inetSocketAddress = new InetSocketAddress(u.getHost(), port);
            //将此套接字连接到服务器，超时设置为1000s
            client.connect(inetSocketAddress, 1000);
            Map<String,String> requestHeaders = new HashMap<>();

            //设置请求头的内容
            requestHeaders.put("Host", u.getHost()+":"+port);
            requestHeaders.put("Accept", "text/html");
            requestHeaders.put("Connection", "close");
            requestHeaders.put("User-Agent", "how2j mini brower / java1.8");

            if(gzip)
                requestHeaders.put("Accept-Encoding", "gzip");
            //获取url的路径部分
            String path = u.getPath();
            if(path.length()==0)
                path = "/";

            if(null!=params && isGet){
                String paramsString = HttpUtil.toParams(params);
                path = path + "?" + paramsString;
            }
            //请求行信息：
            String firstLine = method + " " + path + " HTTP/1.1\r\n";
            //http请求
            StringBuffer httpRequestString = new StringBuffer();
            httpRequestString.append(firstLine);
            Set<String> headers = requestHeaders.keySet();
            for (String header : headers) {
                String headerLine = header + ":" + requestHeaders.get(header)+"\r\n";
                httpRequestString.append(headerLine);
            }

            if(null!=params && !isGet){
                //转化为字符串
                String paramsString = HttpUtil.toParams(params);
                httpRequestString.append("\r\n");
                httpRequestString.append(paramsString);
            }

            PrintWriter pWriter = new PrintWriter(client.getOutputStream(), true);
            pWriter.println(httpRequestString);
            //返回此套接字的输入流
             InputStream is = client.getInputStream();
            //
            result = readBytes(is,true);
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                result = e.toString().getBytes("utf-8");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
        }

        return result;

    }


    public static byte[] readBytes(InputStream is, boolean fully) throws IOException {
        //设置缓冲区大小
        int buffer_size = 1024;
        //建立字节缓冲区
        byte buffer[] = new byte[buffer_size];
        //字节输出流
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //为了避免信息丢失，一致读取输入流的内容
        while(true) {
            int length = is.read(buffer);
            //字节输入流结束了
            if(-1==length)
                break;
            //将缓冲区里面的内容读取出来
            baos.write(buffer, 0, length);
            if(!fully && length!=buffer_size)
                    break;
        }
        byte[] result =baos.toByteArray();
        return result;
    }
}