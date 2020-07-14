package cn.how2j.diytomcat.test;
 
import java.io.File;
import java.lang.reflect.Method;
 
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
 
public class CustomizedClassLoader extends ClassLoader {
    private File classesFolder = new File(System.getProperty("user.dir"),"classes_4_test");
 
    protected Class<?> findClass(String QualifiedName) throws ClassNotFoundException {
        byte[] data = loadClassData(QualifiedName);
        return defineClass(QualifiedName, data, 0, data.length);
    }
 
    private byte[] loadClassData(String fullQualifiedName) throws ClassNotFoundException {
        String fileName = StrUtil.replace(fullQualifiedName, ".", "/") + ".class";
        File classFile = new File(classesFolder, fileName);
        if(!classFile.exists())
            throw new ClassNotFoundException(fullQualifiedName);
        return FileUtil.readBytes(classFile);
    }
 
    public static void main(String[] args) throws Exception {
 
        CustomizedClassLoader loader = new CustomizedClassLoader();
 
        Class<?> how2jClass = loader.loadClass("cn.how2j.diytomcat.test.HOW2J");
 
        Object o = how2jClass.newInstance();
 
        Method m = how2jClass.getMethod("hello");
 
        m.invoke(o);
         
        System.out.println(how2jClass.getClassLoader());
 
    }
 
}