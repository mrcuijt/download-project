@echo off
set courseId=%1
java -classpath ".;./lib/*" WangyiClassSpider -r -cid %courseId%
echo 'This program is finished.'