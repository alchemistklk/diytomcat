# Mini-Tomcat
## 简介
本项目内容是一个简化版的Tomcat服务器。

此服务器主要是仿照Tomcat的源码,利用**Java基础、反射、log4j、junit、jsoup、hutool**等知识进行构建。

涉及到的主要功能包括:
    **构建Tomcat内置对象，处理多种文件格式(包括二进制文件)，处理Servlet请求，处理Jsp请求，客户端跳转，过滤器，文件部署以及监听器**的功能。

## Tomcat内置对象
   - ### Request
      Request对象主要用于获取请求的相关信息uri,requestString   
   - ### Response
        Response用于封装返回给浏览器的数据
   - ### Context
     
      每个Context实例代表了一个应用,用于多应用的加载
      1. 属性：path(访问路径),docBase(文件夹所在的绝对路径)
      
      2. 加载时机：在服务器启动的时候把webapps目录下的文件夹加载成Context对象了
      
      3. 解析时机：在构造Request的时候就把Context解析出来
        
        ```
            String fileName = StrUtil.removePrefix(uri, "/");
            File file = FileUtil.file(context.getDocBase(), fileName);
        ````
        
        优势：将uri中访问的资源与实际服务器资源对应起来，代码如上
        1. 通过配置的方式来访问Context
            主要是使用XML文件来进行配置，使用Jsoup进行解析，遍历所有的Context标签的将里面的内容封装到Context
        2. 扫描webapp目录下面的文件夹然后进行context装载

   - ### Host    
        代表虚拟主机，每个Host下面有多个Context因为Host的下一层是Context所以将
        
        scanContextsOnWebAppsFolder();：用于解析Webapp目录下下面的文件夹导入ContextMap
        
        scanContextsInServerXML();：解析配置文件中的内容并装入Context中
        
        放在Host的构造方法中。
        ```
        public Host(String name, Engine engine){
            this.contextMap = new HashMap<>();
            this.name =  name;
            this.engine = engine;
            scanContextsOnWebAppsFolder();
            scanContextsInServerXML();
        }
        ```
   - ### Engine
        代表Servlet引擎，每个Engine下面有多个Host
        
        通过解析XML文件获得Engine中默认虚拟主机和所有主机
        
        ```
        public Engine(Service service) {
            this.service = service;
            this.defaultHost = ServerXMLUtil.getEngineDefaultHost();
            this.hosts = ServerXMLUtil.getHosts(this);
            checkDefault();
        }
        ```
   - ### Connector
        代表连接的不同连接点，一个Service一般对应多个Connector端口
        构造方法中要加入Service，启动服务器套接字，之后使用多线程来处理客户端的连接请求
        ```
        ServerSocket s = new ServerSocket(port);
        while(true){
            //服务器随时接受请求并分配一个线程工作
            Socket s = ss.accept();
            Runnable r = new Runnable(){
                @Override
                public void run(){
                    //操作
                }
            };
            ThreadPoolUtil.run(r);
        }
        ```
