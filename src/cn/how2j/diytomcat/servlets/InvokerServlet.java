package cn.how2j.diytomcat.servlets;

import cn.how2j.diytomcat.catalina.Context;
import cn.how2j.diytomcat.http.Request;
import cn.how2j.diytomcat.http.Response;
import cn.how2j.diytomcat.util.Constant;
import cn.hutool.core.util.ReflectUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

//处理Servlet请求
public class InvokerServlet extends HttpServlet {
	private static InvokerServlet instance = new InvokerServlet();

	public static synchronized InvokerServlet getInstance() {
		return instance;
	}

	private InvokerServlet() {

	}

	public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
			throws IOException, ServletException {
			Request request = (Request) httpServletRequest;
			Response response = (Response) httpServletResponse;
			//得到uri
			String uri = request.getUri();
			//得到请求中的context
			Context context = request.getContext();
			//得到Servlet对应的ServletClass
			String servletClassName = context.getServletClassName(uri);

			try {
				Class servletClass = context.getWebappClassLoader().loadClass(servletClassName);
				Object servletObject = context.getServlet(servletClass);
				ReflectUtil.invoke(servletObject, "service", request, response);

				//判断是否跳转
				if(null != response.getRedirectPath()){
					response.setStatus(Constant.CODE_302);
				}
				else {
					response.setStatus(Constant.CODE_200);
				}

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
	}


}
