package cn.how2j.diytomcat.catalina;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 存放Filter的初始化参数
 */
public class StandardFilterConfig implements FilterConfig {
    //Servlet的相关信息
    private ServletContext servletContext;
    //初始化参数
    private Map<String, String> initParameters;
    //过滤器的名字
    private String filterName;

    public StandardFilterConfig(ServletContext servletContext, String filterName, Map<String, String> initParameters) {
        this.servletContext = servletContext;
        this.initParameters = initParameters;
        this.filterName = filterName;
        if(null == this.initParameters)
            this.initParameters = new HashMap<>();
    }

    @Override
    public String getFilterName() {
        return filterName;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String getInitParameter(String name) {
        return initParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }
}
