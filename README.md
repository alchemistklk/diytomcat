# Mini-Tomcat
## 简介
本项目内容是一个简化版的Tomcat服务器。此服务器主要是仿照Tomcat的源码,利用**Java基础、反射、log4j、junit、jsoup、hutool**等知识进行构建。
涉及到的主要功能包括：
    **构建Tomcat内置对象，处理多种文件格式(包括二进制文件)，处理Servlet请求，处理Jsp请求，客户端跳转，过滤器，文件部署以及监听器**的功能。

### Tomcat内置对象
   - #### Request
   
      Request对象主要用于获取请求的相关信息uri,requestString
      1. ##### 构造方法
           1. 请求变量的map
           2. 请求头map
           3. 套接字
           4. 连接点
       2. #### 解析request请求
           1. 读取套接字传入的输入流信息
           2. 将输入流信息转化为字节数组
           3. 将字节数组保存为字符串(当作请求信息)
       3. #### 解析uri
       4. #### 解析Context
       5. #### 解析Method
       6. #### 解析Paramaters
       7. #### 解析Headers
       8. #### 解析Cookies
   - #### Response
   
        Response用于封装返回给浏览器的数据

        1. handle200():将数据返回给浏览器
            响应头+响应体
        2. 
   - #### Context
        每个Context实例代表了一个应用
         
   
