package cn.how2j.diytomcat;

import cn.how2j.diytomcat.classloader.CommonClassLoader;

import java.lang.reflect.Method;

public class Bootstrap {
    public static void main(String[] args) throws Exception {
        CommonClassLoader commonClassLoader = new CommonClassLoader();

        Thread.currentThread().setContextClassLoader(commonClassLoader);

        String serverClassName = "cn.how2j.diytomcat.catalina.Server";

        Class<?> serverClazz = commonClassLoader.loadClass(serverClassName);

        Object serverObject = serverClazz.newInstance();

        Method m = serverClazz.getMethod("start");

        m.invoke(serverObject);

        // 不能关闭，否则后续就不能使用啦
        // commonClassLoader.close();

    }
}