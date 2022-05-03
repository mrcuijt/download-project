@echo off
set filePath=%1
java -classpath ".;./lib/*" WangyiClassSpider -dr %filePath%
echo 'This program is finished.'