@echo off
set taskfile=%1
set proxy=%2
java -classpath ".;./lib/*" MulitTaskDownloader %taskfile% %proxy%
echo This program is finished.
