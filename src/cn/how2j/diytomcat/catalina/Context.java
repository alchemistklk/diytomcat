package cn.how2j.diytomcat.catalina;

import cn.how2j.diytomcat.classloader.WebappClassLoader;
import cn.how2j.diytomcat.exception.WebConfigDuplicatedException;
import cn.how2j.diytomcat.http.ApplicationContext;
import cn.how2j.diytomcat.http.StandardServletConfig;
import cn.how2j.diytomcat.util.ContextXMLUtil;
import cn.how2j.diytomcat.watcher.ContextFileChangeWatcher;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import org.apache.jasper.JspC;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.*;


//表示一个应用
public class Context {
    //访问的路径
    private String path;
    //资源在系统中的路径
    private String docBase;
    private File contextWebXmlFile;

    //Servlet的映射信息
    //地址对应Servlet的类名
    private Map<String, String> url_servletClassName;
    //地址对应Servlet的名称
    private Map<String, String> url_ServletName;
    //Servlet名称对应的类名
    private Map<String, String> servletName_className;
    //类名对应的Servlet的名称
    private Map<String, String> className_servletName;
    private Map<String, Map<String, String>> servlet_className_init_params;
    private List<String> loadOnStartupServletClassNames;
    //类加载器
    private WebappClassLoader webappClassLoader;

    private Host host;
    private boolean reloadable;
    private ContextFileChangeWatcher contextFileChangeWatcher;

    private ServletContext servletContext;
    //准备map作为存放servlet的池子
    private Map<Class<?>, HttpServlet> servletPool;

    //过滤器
    //url对应的过滤器类名
    private Map<String, List<String>> url_filterClassName;
    //url对应的过滤器名称
    private Map<String, List<String>> url_filterNames;
    //过滤器名对应的类名
    private Map<String, String> filterName_className;
    //类名对应的过滤器名
    private Map<String, String> className_filterName;
    //过滤器类名的初始化参数
    private Map<String, Map<String, String>> filter_className_init_params;
    //准备了filterPool属性
    private Map<String, Filter> filterPool;

    //监听器
    private List<ServletContextListener> listeners;

    public Context(String path, String docBase, Host host, boolean reloadable) {
        TimeInterval timeInterval = DateUtil.timer();
        this.host = host;
        this.reloadable = reloadable;

        this.path = path;
        this.docBase = docBase;
        this.contextWebXmlFile = new File(docBase, ContextXMLUtil.getWatchedResource());

        this.url_servletClassName = new HashMap<>();
        this.url_ServletName = new HashMap<>();
        this.servletName_className = new HashMap<>();
        this.className_servletName = new HashMap<>();
        this.servlet_className_init_params = new HashMap<>();
        this.loadOnStartupServletClassNames = new ArrayList<>();

        this.servletContext = new ApplicationContext(this);

        ClassLoader commonClassLoader = Thread.currentThread().getContextClassLoader();
        this.webappClassLoader = new WebappClassLoader(docBase, commonClassLoader);

        this.servletPool = new HashMap<>();

        //初始化
        this.url_filterClassName = new HashMap<>();
        this.url_filterNames = new HashMap<>();
        this.filterName_className = new HashMap<>();
        this.className_filterName = new HashMap<>();
        this.filter_className_init_params = new HashMap<>();

        //初始化
        this.filterPool = new HashMap<>();

        //初始化监听器
        listeners = new ArrayList<ServletContextListener>();
        LogFactory.get().info("Deploying web application directory {}", this.docBase);
        deploy();
        LogFactory.get().info("Deployment of web application directory {} has finished in {} ms", this.docBase,timeInterval.intervalMs());
    }


    public WebappClassLoader getWebClassLoader(){
        return webappClassLoader;
    }
    public void reload() {
        host.reload(this);
    }

    private void deploy() {
        loadListeners();
        init();

        if(reloadable){
            contextFileChangeWatcher = new ContextFileChangeWatcher(this);
            contextFileChangeWatcher.start();
        }
        JspC c = new JspC();
        //进行JspRuntimeContext初始化
        new JspRuntimeContext(servletContext, c);
    }

    private void init() {
        if (!contextWebXmlFile.exists())
            return;

        try {
            checkDuplicated();
        } catch (WebConfigDuplicatedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }

        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document d = Jsoup.parse(xml);
        parseServletMapping(d);



        //解析过滤器xml文件
        parseFilterMapping(d);
        parseServletInitParams(d);
        parseFilterInitParams(d);

        //初始化过滤器
        initFilter();
        parseLoadOnStartup(d);
        handleLoadOnStartup();

        fireEvent("init");
    }

    private void initFilter() {
        Set<String> classNames = className_filterName.keySet();
        for(String className : classNames){
            try {
                //加载这个类
                Class clazz = this.getWebClassLoader().loadClass(className);
                //通过类名获得初始参数
                Map<String, String> initParameters = filter_className_init_params.get(className);
                //获得过滤器名称
                String filterName = className_filterName.get(className);
                //过滤器的配置文件
                FilterConfig filterConfig = new StandardFilterConfig(servletContext, filterName, initParameters);
                Filter filter = filterPool.get(clazz);
                if(null == filter){
                    filter = (Filter) ReflectUtil.newInstance(clazz);
                    filter.init(filterConfig);
                    filterPool.put(className, filter);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void parseFilterInitParams(Document d) {
        Elements filterClassNameElements = d.select("filter-class");
        for(Element filterClassNameElement : filterClassNameElements){
            String filterClassName = filterClassNameElement.text();

            Elements initElements = filterClassNameElement.parent().select("init-param");
            if(initElements.isEmpty()){
                continue;
            }

            Map<String, String> initParams = new HashMap<>();
            for(Element element : initElements){
                String name = element.select("param-name").get(0).text();
                String value = element.select("param-value").get(0).text();
                initParams.put(name,value);
            }

            filter_className_init_params.put(filterClassName, initParams);
        }
    }

    private void parseFilterMapping(Document d) {
        //filter_url_name
        Elements mappingurlElements = d.select("filter-mapping url-pattern");
        for(Element mappingurlElement : mappingurlElements){
            //获取配置文件中的url-pattern
            String urlPattern = mappingurlElement.text();
            String filterName = mappingurlElement.parent().select("filter-name").first().text();
            //将过滤器的名称放在表格中
            List<String> filterNames = url_filterNames.get(urlPattern);
            if(null == filterNames){
                filterNames = new ArrayList<>();
                url_filterNames.put(urlPattern, filterNames);
            }
            filterNames.add(filterName);
        }

        //class_name_filter_name
        Elements filterNameElements = d.select("filter filter-name");
        for(Element filterNameElement : filterNameElements){
            String filterName = filterNameElement.text();
            //通过filter-class获取filterClass
            String filterClass = filterNameElement.parent().select("filter-class").first().text();
            filterName_className.put(filterName, filterClass);
            className_filterName.put(filterClass, filterName);
        }

        //url_filterClassName
        Set<String> urls = url_filterNames.keySet();
        for(String url : urls){
            //得到url对应的类名
            List<String> filterNames = url_filterNames.get(url);
            if(null == filterNames){
                filterNames = new ArrayList<>();
                url_filterNames.put(url, filterNames);
            }
            for(String filterName : filterNames){
                //通过url类名得到过滤器的类名
                String filterClassName = filterName_className.get(filterName);
                //通过唯一资源定位获取过滤器的所有类名
                List<String> filterClassNames = url_filterClassName.get(url);
                //如果过滤器的类名为null
                if(filterClassNames == null){
                    filterClassNames = new ArrayList<>();
                    url_filterClassName.put(url, filterClassNames);
                }
                filterClassNames.add(filterClassName);
            }
        }
    }

    private void parseServletMapping(Document d) {
        // url_ServletName
        Elements mappingurlElements = d.select("servlet-mapping url-pattern");
        for (Element mappingurlElement : mappingurlElements) {
            String urlPattern = mappingurlElement.text();
            String servletName = mappingurlElement.parent().select("servlet-name").first().text();
            url_ServletName.put(urlPattern, servletName);
        }
        // servletName_className / className_servletName
        Elements servletNameElements = d.select("servlet servlet-name");
        for (Element servletNameElement : servletNameElements) {
            String servletName = servletNameElement.text();
            String servletClass = servletNameElement.parent().select("servlet-class").first().text();
            servletName_className.put(servletName, servletClass);
            className_servletName.put(servletClass, servletName);
        }
        // url_servletClassName
        Set<String> urls = url_ServletName.keySet();
        for (String url : urls) {
            String servletName = url_ServletName.get(url);
            String servletClassName = servletName_className.get(servletName);
            url_servletClassName.put(url, servletClassName);
        }
    }

    private void checkDuplicated(Document d, String mapping, String desc) throws WebConfigDuplicatedException {
        Elements elements = d.select(mapping);
        // 判断逻辑是放入一个集合，然后把集合排序之后看两临两个元素是否相同
        List<String> contents = new ArrayList<>();
        for (Element e : elements) {
            contents.add(e.text());
        }

        Collections.sort(contents);

        for (int i = 0; i < contents.size() - 1; i++) {
            String contentPre = contents.get(i);
            String contentNext = contents.get(i + 1);
            if (contentPre.equals(contentNext)) {
                throw new WebConfigDuplicatedException(StrUtil.format(desc, contentPre));
            }
        }

    }

    private void checkDuplicated() throws WebConfigDuplicatedException {
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document d = Jsoup.parse(xml);

        checkDuplicated(d, "servlet-mapping url-pattern", "servlet url 重复,请保持其唯一性:{} ");
        checkDuplicated(d, "servlet servlet-name", "servlet 名称重复,请保持其唯一性:{} ");
        checkDuplicated(d, "servlet servlet-class", "servlet 类名重复,请保持其唯一性:{} ");
    }

    public String getServletClassName(String uri) {
        return url_servletClassName.get(uri);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDocBase() {
        return docBase;
    }

    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }

    public WebappClassLoader getWebappClassLoader() {
        return webappClassLoader;
    }

    public void stop() {
        webappClassLoader.stop();
        contextFileChangeWatcher.stop();

        destroyServlets();
        fireEvent("destroy");
    }

    public boolean isReloadable() {
        return reloadable;
    }

    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }


    public synchronized HttpServlet  getServlet(Class<?> clazz)
            throws InstantiationException, IllegalAccessException, ServletException {
        //传入类对象获取Servlet
        HttpServlet servlet = servletPool.get(clazz);

        if (null == servlet) {
            servlet = (HttpServlet) clazz.newInstance();
            ServletContext servletContext = this.getServletContext();

            String className = clazz.getName();
            String servletName = className_servletName.get(className);

            Map<String, String> initParameters = servlet_className_init_params.get(className);
            ServletConfig servletConfig = new StandardServletConfig(servletContext, servletName, initParameters);

            //放入map结构之前进行初始化
            servlet.init(servletConfig);
            servletPool.put(clazz, servlet);
        }

        return servlet;
    }

    private void parseServletInitParams(Document d) {
        Elements servletClassNameElements = d.select("servlet-class");
        for (Element servletClassNameElement : servletClassNameElements) {
            String servletClassName = servletClassNameElement.text();

            Elements initElements = servletClassNameElement.parent().select("init-param");
            if (initElements.isEmpty())
                continue;


            Map<String, String> initParams = new HashMap<>();

            for (Element element : initElements) {
                String name = element.select("param-name").get(0).text();
                String value = element.select("param-value").get(0).text();
                initParams.put(name, value);
            }

            servlet_className_init_params.put(servletClassName, initParams);

        }

//		System.out.println("class_name_init_params:" + servlet_className_init_params);

    }
    private void destroyServlets() {
        Collection<HttpServlet> servlets = servletPool.values();
        for (HttpServlet servlet : servlets) {
            servlet.destroy();
        }
    }

    public void parseLoadOnStartup(Document d) {
        Elements es = d.select("load-on-startup");
        for (Element e : es) {
            String loadOnStartupServletClassName = e.parent().select("servlet-class").text();
            loadOnStartupServletClassNames.add(loadOnStartupServletClassName);
        }
    }
    public void handleLoadOnStartup() {
        for (String loadOnStartupServletClassName : loadOnStartupServletClassNames) {
            try {
                Class<?> clazz = webappClassLoader.loadClass(loadOnStartupServletClassName);
                getServlet(clazz);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ServletException e) {
                e.printStackTrace();
            }
        }
    }
    //匹配模式
    public boolean match(String pattern, String uri){
        //完全匹配
        if(StrUtil.equals(pattern,uri)){
            return true;
        }
        // /*通配符匹配
        if(StrUtil.equals(pattern,"/*")){
            return true;
        }
        //后缀名 /*.jsp
        if(StrUtil.startWith(pattern,"/*.")){
            String patternExtName = StrUtil.subAfter(pattern, '.', false);
            String uriExtName = StrUtil.subAfter(uri,'.', false);
            if(StrUtil.equals(patternExtName, uriExtName)){
                return true;
            }
        }
        return false;
    }
    //获取匹配了的过滤器集合
    public List<Filter> getMatchedFilters(String uri){
        //将所有的过滤器
        List<Filter> filters = new ArrayList<>();
        //得到所有过滤器的url
        Set<String> patterns = url_filterClassName.keySet();
        Set<String> matchedPatterns = new HashSet<>();
        for(String pattern : patterns){
            if(match(pattern, uri)){
                //将所有的url加入到匹配模式中
                matchedPatterns.add(pattern);
            }
        }
        //匹配的过滤器的类名放入到set集合中
        Set<String> matchedFilterClassNames = new HashSet<>();

        for(String pattern : patterns){
            //pattern指的是url，通过url获得过滤器的类名
            List<String> filterClassNames = url_filterClassName.get(pattern);
            //将过滤器类名加入到匹配过滤器的类名
            matchedFilterClassNames.addAll(filterClassNames);
        }
        //获得了所有的过滤器
        for(String filterClassName : matchedFilterClassNames){
            Filter filter = filterPool.get(filterClassName);
            filters.add(filter);
        }
        return filters;
    }
    //新建addListener
    public void addListener(ServletContextListener listener){
        listeners.add(listener);
    }
    //从web.xml中扫面监听器类
    public void loadListeners(){
        try{
            if(!contextWebXmlFile.exists())
            return;
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        //解析xml成Document文件
        Document d = Jsoup.parse(xml);

        Elements es = d.select("listener listener-class");
        for (Element e : es){
            String listenerClassName = e.text();

            //加载监听器类
            Class<?> clazz = this.getWebClassLoader().loadClass(listenerClassName);
            //实例化监听器
            ServletContextListener listener = (ServletContextListener) clazz.newInstance();
            addListener(listener);
        }
        }catch (IORuntimeException | ClassNotFoundException | IllegalAccessException | InstantiationException e ){
            throw new RuntimeException(e);
        }
    }
    public void fireEvent(String type){
        ServletContextEvent event = new ServletContextEvent(servletContext);
        for (ServletContextListener servletContextListener : listeners){
            if("init".equals(type)){
                servletContextListener.contextInitialized(event);
            }
            if("destroy".equals(type)){
                servletContextListener.contextDestroyed(event);
            }
        }

    }
}
