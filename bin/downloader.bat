@echo off
set linkfile=%1
set proxy=%2
java -classpath ".;./lib/*" Downloader %linkfile% %proxy%
echo This program is finished.
