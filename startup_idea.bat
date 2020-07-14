del /q bootstrap.jar
jar cvf0 bootstrap.jar -C out/production/diytomcat cn/how2j/diytomcat/Bootstrap.class -C out/production/diytomcat cn/how2j/diytomcat/classloader/CommonClassLoader.class
del /q lib/diytomcat.jar
cd out
cd production
cd diytomcat
jar cvf0 ../../../lib/diytomcat.jar *
cd ..
cd ..
cd ..
java -cp bootstrap.jar cn.how2j.diytomcat.Bootstrap 
pause