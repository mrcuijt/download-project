@echo off
set course=%1
set lessonId=%2
java -classpath ".;./lib/*" CourseLessonDownloaderByLessonId -dpc %course% -lessons %lessonId%
echo 'This program is finished.'