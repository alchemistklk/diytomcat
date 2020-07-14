package cn.how2j.diytomcat.servlets;

import cn.how2j.diytomcat.catalina.Context;
import cn.how2j.diytomcat.classloader.JspClassLoader;
import cn.how2j.diytomcat.http.Request;
import cn.how2j.diytomcat.http.Response;
import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.JspUtil;
import cn.how2j.diytomcat.util.WebXMLUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * 处理逻辑先将jsp文件转换为java文件，然后编译成class文件，之后运行
 */
public class JspServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static JspServlet instance = new JspServlet();

    public static synchronized JspServlet getInstance(){
        return instance;
    }

    private JspServlet(){

    }

    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){

        try {
            Request request = (Request) httpServletRequest;
            Response response = (Response) httpServletResponse;

            String uri = request.getRequestURI();

            if("/".equals(uri)){
                //此时访问欢迎页
                uri = WebXMLUtil.getWelcomeFile(request.getContext());
            }
            String fileName = StrUtil.removePrefix(uri, "/");
            File file = FileUtil.file(request.getRealPath(fileName));
            File jspFile = file;
            //如果jsp文件存在
            if(jspFile.exists()){
                Context context = request.getContext();
                String path = context.getPath();
                String subFolder;
                if("/".equals(path))
                    //在work目录下对应的应用目录是"_"
                    subFolder = "_";
                else
                    //获取文件目录
                    subFolder = StrUtil.subAfter(path, '/', false);
                //新的servlet类目录 uri + subFolder
                String servletClassPath = JspUtil.getServletClassPath(uri, subFolder);
                //得到该目录下的jsp编译文件
                File jspServletClassFile = new File(servletClassPath);
                //如果该目录下资源不存在直接编译
                if(!jspServletClassFile.exists())
                    JspUtil.compileJsp(context,jspFile);
                //如果jsp文件晚于class文件那么也需要重新编译
                else if(jspFile.lastModified() > jspServletClassFile.lastModified()){
                    JspUtil.compileJsp(context,jspFile);
                    //jsp文件过期之后需要从类加载器中脱出
                    JspClassLoader.invalidJspClassLoader(uri,context);
                }


                String extName = FileUtil.extName(file);
                String mimeType = WebXMLUtil.getMimeType(extName);
                response.setContentType(mimeType);
                //根据context和uri获得类加载器
                JspClassLoader jspClassLoader = JspClassLoader.getJspClassLoader(uri, context);
                //获得jsp对应的文件名
                String jspServletClassName = JspUtil.getJspServletClassName(uri, subFolder);
                //根据JspClassLoader加载类对象jspServletClass
                Class jspServletClass = jspClassLoader.loadClass(jspServletClassName);

                //使用context线程的用于进行单例管理的getServlet方法获得Servlet实例，调用Service方法
                HttpServlet servlet = context.getServlet(jspServletClass);
                servlet.service(request,response);
                if(null != response.getRedirectPath()){
                    response.setStatus(Constant.CODE_302);
                }else {
                    response.setStatus(Constant.CODE_200);
                }
            }else{
                response.setStatus(Constant.CODE_404);
            }

        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
