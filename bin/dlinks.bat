@echo off
set linkfile=%1
set proxy=%2
java -classpath ".;./lib/*" MulitLinkDownloader %linkfile% %proxy%
echo This program is finished.
