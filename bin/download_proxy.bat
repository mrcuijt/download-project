@echo off
set filePath=%1
java -classpath ".;./lib/*" WangyiClassSpider -dpr %filePath%
echo 'This program is finished.'