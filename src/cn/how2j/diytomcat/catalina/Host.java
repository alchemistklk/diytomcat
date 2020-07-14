package cn.how2j.diytomcat.catalina;

import cn.how2j.diytomcat.util.Constant;
import cn.how2j.diytomcat.util.ServerXMLUtil;
import cn.how2j.diytomcat.watcher.WarFileWatcher;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Host {
    private String name;
    private Map<String, Context> contextMap;
    private Engine engine;
    public Host(String name, Engine engine){
        this.contextMap = new HashMap<>();
        this.name =  name;
        this.engine = engine;

        scanContextsOnWebAppsFolder();
        scanContextsInServerXML();
        scanWarOnWebAppsFolder();
        new WarFileWatcher(this).start();

    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    //获取解析文件中的context
    private  void scanContextsInServerXML() {
        List<Context> contexts = ServerXMLUtil.getContexts(this);
        for (Context context : contexts) {
            contextMap.put(context.getPath(), context);
        }
    }

    //扫描webapp目录下的context
    private  void scanContextsOnWebAppsFolder() {
        File[] folders = Constant.webappsFolder.listFiles();
        for (File folder : folders) {
            if (!folder.isDirectory())
                continue;
            loadContext(folder);
        }
    }

    //给webapp目录下的文件设置uri和context
    private  void loadContext(File folder) {
        //文件夹的名字每一级目录后面都加‘/’
        String path = folder.getName();
        if ("ROOT".equals(path))
            path = "/";
        else
            path = "/" + path;

        String docBase = folder.getAbsolutePath();
        Context context = new Context(path,docBase,this, true);

        contextMap.put(context.getPath(), context);
    }

    public Context getContext(String path) {
        return contextMap.get(path);
    }

    public void reload(Context context) {
        LogFactory.get().info("Reloading Context with name [{}] has started", context.getPath());
        String path = context.getPath();
        String docBase = context.getDocBase();
        boolean reloadable = context.isReloadable();
        // stop
        context.stop();
        // remove
        contextMap.remove(path);
        // allocate new context
        Context newContext = new Context(path, docBase, this, reloadable);
        // assign it to map
        contextMap.put(newContext.getPath(), newContext);
        LogFactory.get().info("Reloading Context with name [{}] has completed", context.getPath());

    }
    //把一个文件夹加载为Context
    public void load(File folder){
        //获取文件夹的名称
        String path = folder.getName();
        if ("ROOT".equals(path))
            path = "/";
        else
            path = "/" + path;
        //获取文件夹的绝对路径
        String docBase = folder.getAbsolutePath();
        //新建一个context
        Context context = new Context(path, docBase, this, false);
        //放入contextmap结构中
        contextMap.put(context.getPath(), context);
    }
    public void loadWar(File warFile){
        //得到warFile的文件名
        String fileName =warFile.getName();
        //得到文件名
        String folderName = StrUtil.subBefore(fileName,".",true);
        //看看是否已经有对应的 Context了
        Context context= getContext("/"+folderName);
        if(null!=context)
            return;
        //先看是否已经有对应的文件夹
        File folder = new File(Constant.webappsFolder,folderName);
        //如果文件存在
        if(folder.exists())
            return;
        //移动war文件，因为jar 命令只支持解压到当前目录下
        File tempWarFile = FileUtil.file(Constant.webappsFolder, folderName, fileName);
        //temp临时war包得到
        File contextFolder = tempWarFile.getParentFile();
        //创建路径
        contextFolder.mkdir();
        FileUtil.copyFile(warFile, tempWarFile);
        //解压
        String command = "jar xvf " + fileName;
//		System.out.println(command);
        Process p = RuntimeUtil.exec(null, contextFolder, command);
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //解压之后删除临时war
        tempWarFile.delete();
        //然后创建新的 Context
        load(contextFolder);

    }

    //扫描webapp所有目录，处理所有的war文件
    private void scanWarOnWebAppsFolder(){
        File folder = FileUtil.file(Constant.webappsFolder);
        File[] files = folder.listFiles();
        for (File file : files) {
            if(!file.getName().toLowerCase().endsWith(".war"))
                continue;
            loadWar(file);
        }
    }
}
