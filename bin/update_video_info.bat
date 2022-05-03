@echo off
set fileName=%1
java -classpath ".;./lib/*" WangyiClassSpider -ru %fileName%

del *.getVideoLearnInfo.dwr
del *_video

echo 'This program is finished.'