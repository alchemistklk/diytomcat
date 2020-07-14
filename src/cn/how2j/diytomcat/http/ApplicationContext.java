package cn.how2j.diytomcat.http;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import cn.how2j.diytomcat.catalina.Context;
import com.sun.prism.impl.BaseContext;

public class ApplicationContext extends BaseServletContext {

	private Map<String, Object> attributesMap;
	private Context context;

	public ApplicationContext(Context context) {
		this.attributesMap = new HashMap<>();
		this.context = context;
	}

	public void removeAttribute(String name) {
		attributesMap.remove(name);
	}

	public void setAttribute(String name, Object value) {
		attributesMap.put(name, value);
	}

	public Object getAttribute(String name) {
		return attributesMap.get(name);
	}

	public Enumeration<String> getAttributeNames() {
		Set<String> keys = attributesMap.keySet();
		return Collections.enumeration(keys);
	}

	public String getRealPath(String path) {
		return new File(context.getDocBase(), path).getAbsolutePath();
	}
}
