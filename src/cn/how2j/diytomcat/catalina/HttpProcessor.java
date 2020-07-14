package cn.how2j.diytomcat.catalina;

import cn.how2j.diytomcat.http.Request;
import cn.how2j.diytomcat.http.Response;
import cn.how2j.diytomcat.servlets.DefaultServlet;
import cn.how2j.diytomcat.servlets.InvokerServlet;
import cn.how2j.diytomcat.servlets.JspServlet;
import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.SessionManager;
import cn.hutool.core.util.*;
import cn.hutool.log.LogFactory;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
//处理Socket请求
public class HttpProcessor {
    public void execute(Socket s, Request request, Response response){
        try {
            //通过request来获取uri
            String uri = request.getUri();
            if(null==uri)
                return;
            prepareSession(request, response);

            Context context = request.getContext();
            //通过uri获取servlet的类名
            String servletClassName = context.getServletClassName(uri);
            HttpServlet workingServlet;
            if(null!=servletClassName)
                workingServlet = InvokerServlet.getInstance();
            else if(uri.endsWith(".jsp"))
                workingServlet = JspServlet.getInstance();
            else
                workingServlet = DefaultServlet.getInstance();
            //得到uri匹配到的过滤器
            List<Filter> filters = request.getContext().getMatchedFilters(request.getRequestURI());
            //得到过滤器责任链
            ApplicationFilterChain filterChain = new ApplicationFilterChain(filters, workingServlet);
            filterChain.doFilter(request, response);
            if(request.isForwarded())
                return;
            if(Constant.CODE_200 == response.getStatus()){
                handle200(s, request, response);
                return;
            }
            if(Constant.CODE_302 == response.getStatus()){
                handle302(s,response);
                return;
            }
            if(Constant.CODE_404 == response.getStatus()){
                handle404(s, uri);
                return;
            }

        } catch (Exception e) {
            LogFactory.get().error(e);
            handle500(s,e);
        }
        finally{
            try {
                if(!s.isClosed())
                    s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //处理200请求
    private void handle200(Socket s, Request request, Response response)
            throws IOException {
        OutputStream os = s.getOutputStream();
        String contentType = response.getContentType();

        byte[] body = response.getBody();
        String cookiesHeader = response.getCookiesHeader();

        boolean gzip = isGzip(request, body, contentType);

        String headText;
        if (gzip)
            headText = Constant.response_head_200_gzip;
        else
            headText = Constant.response_head_200;

        headText = StrUtil.format(headText, contentType, cookiesHeader);

        //如果是压缩格式
        if (gzip)
            body = ZipUtil.gzip(body);

        byte[] head = headText.getBytes();
        byte[] responseBytes = new byte[head.length + body.length];
        ArrayUtil.copy(head, 0, responseBytes, 0, head.length);
        ArrayUtil.copy(body, 0, responseBytes, head.length, body.length);

        os.write(responseBytes,0,responseBytes.length);
        os.flush();
        os.close();

    }

    //处理404请求
    private void handle404(Socket s, String uri) throws IOException {
        OutputStream os = s.getOutputStream();
        String responseText = StrUtil.format(Constant.textFormat_404, uri, uri);
        responseText = Constant.response_head_404 + responseText;
        byte[] responseByte = responseText.getBytes("utf-8");
        os.write(responseByte);
    }

    //处理302请求
    public void handle302(Socket socket, Response response) throws IOException {
        OutputStream os = socket.getOutputStream();
        //得到响应的跳转路径
        String redirectPath = response.getRedirectPath();
        String head_text = Constant.response_head_302;
        //响应头：相应行 + 跳转路径
        String header = StrUtil.format(head_text, redirectPath);
        //响应头内容转化为字节数组
        byte[] responseBytes = header.getBytes("utf-8");
        os.write(responseBytes);
    }
    private void handle500(Socket s, Exception e) {
        try {
            OutputStream os = s.getOutputStream();
            StackTraceElement stes[] = e.getStackTrace();
            StringBuffer sb = new StringBuffer();
            sb.append(e.toString());
            sb.append("\r\n");
            for (StackTraceElement ste : stes) {
                sb.append("\t");
                sb.append(ste.toString());
                sb.append("\r\n");
            }

            String msg = e.getMessage();

            if (null != msg && msg.length() > 20)
                msg = msg.substring(0, 19);

            String text = StrUtil.format(Constant.textFormat_500, msg, e.toString(), sb.toString());
            text = Constant.response_head_500 + text;
            byte[] responseBytes = text.getBytes("utf-8");
            os.write(responseBytes);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    public void prepareSession(Request request, Response response) {
        String jsessionid = request.getJSessionIdFromCookie();
        HttpSession session = SessionManager.getSession(jsessionid, request, response);
        request.setSession(session);
    }

    private boolean isGzip(Request request, byte[] body, String mimeType) {
        String acceptEncodings=  request.getHeader("Accept-Encoding");
        if(!StrUtil.containsAny(acceptEncodings, "gzip"))
            return false;

        Connector connector = request.getConnector();

        if (mimeType.contains(";"))
            mimeType = StrUtil.subBefore(mimeType, ";", false);

        if (!"on".equals(connector.getCompression()))
            return false;

        if (body.length < connector.getCompressionMinSize())
            return false;

        String userAgents = connector.getNoCompressionUserAgents();
        String[] eachUserAgents = userAgents.split(",");
        for (String eachUserAgent : eachUserAgents) {
            eachUserAgent = eachUserAgent.trim();
            String userAgent = request.getHeader("User-Agent");
            if (StrUtil.containsAny(userAgent, eachUserAgent))
                return false;
        }

        String mimeTypes = connector.getCompressableMimeType();
        String[] eachMimeTypes = mimeTypes.split(",");
        for (String eachMimeType : eachMimeTypes) {
            if (mimeType.equals(eachMimeType))
                return true;
        }

        return false;
    }


}
