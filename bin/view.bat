@echo off
set filePath=%1
java -classpath ".;./lib/*" WangyiClassSpider -v %filePath%
echo 'This program is finished.'