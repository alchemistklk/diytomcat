package cn.how2j.diytomcat.classloader;

import cn.how2j.diytomcat.catalina.Context;
import cn.how2j.diytomcat.util.Constant;
import cn.hutool.core.util.StrUtil;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

/**
 * 1.一个jsp文件对应一个JspClassLoader
 * 2.如果jsp文件被修改，需要换一个新的JspClassLoader
 * 3.这个类加载器是基于jsp文件转译出来的class文件，进行类加载
 */
public class JspClassLoader extends URLClassLoader {
    private static Map<String, JspClassLoader> map = new HashMap<>();
    //让Jsp和JspClassLoader
    public static void invalidJspClassLoader(String uri, Context context){
        String key = context.getPath() + "/" + uri;
        map.remove(key);
    }
    //获取jsp对用的类加载器
    public static JspClassLoader getJspClassLoader(String uri, Context context){
        String key = context.getPath() + "/" + uri;
        JspClassLoader loader = map.get(key);
        //如果没有类加载器
        if(null == loader){
            //新建一个类加载器
            loader = new JspClassLoader(context);
            map.put(key, loader);
        }
        return loader;
    }
    //JspClassLoader加载器是基于WebappCLassLoader类加载器来创建的
    private JspClassLoader(Context context){
        super(new URL[]{}, context.getWebappClassLoader());
        try {
        //子文件夹
        String subFolder;
        //根据context得到路径信息
        String path = context.getPath();
        if("/".equals(path)){
            subFolder = "_";
        }
        else
            subFolder = StrUtil.subAfter(path,'/',false);
        //根据子文件加的路径信息在work目录下新建一个文件
        File classesFolder = new File(Constant.workFolder, subFolder);
        //新建文件的url
        URL url = new URL("file:" + classesFolder.getAbsolutePath() + "/");
        this.addURL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
