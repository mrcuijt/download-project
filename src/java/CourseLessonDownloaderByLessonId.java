
import java.util.List;
import java.util.ArrayList;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.impl.Arguments;

public class CourseLessonDownloaderByLessonId extends CourseLessonDownloader {

  public CourseLessonDownloaderByLessonId(WangyiClassSpider.CourseInfo courseInfo){
    super(courseInfo);
  }

  public static void main(String[] args){
    long begin = System.currentTimeMillis();
    Downloader downloader = new Downloader();
//    if(args == null || args.length < 2) return;
//    if(args.length == 3){
//      if(args[2].equals("proxy")){
//        URLConnectionUtil.setProxy();
//      }
//    }
    ArgumentParser parser = ArgumentParsers.newFor("CourseLessonDownloaderByLessonId").build()
        .defaultHelp(true)
        .description("网易云课堂视频下载工具");
    parser.addArgument("-p", "--proxy").action(Arguments.storeTrue())
        .dest("proxy")
        .help("use proxy by proxy_settings.ini");
    parser.addArgument("-d", "--download").action(Arguments.storeTrue())
        .dest("download")
        .help("download course xml video");
    parser.addArgument("-c", "--course")
        .dest("course")
        .help("-c file : get course info");
    parser.addArgument("-lessons")
        .dest("lessons")
        .help("input course xml file");
    Namespace ns = null;
    try {
      ns = parser.parseArgs(args);
      Boolean proxy = ns.getBoolean("proxy");
      String course = ns.getString("course");
      Boolean download = ns.getBoolean("download");
      String lessons = ns.getString("lessons");
      System.out.println(String.format("proxy:%s; course:%s; download:%s; lessons:%s;", String.valueOf(proxy), course, String.valueOf(download), String.valueOf(lessons)));
      if(proxy){
        URLConnectionUtil.setProxy();
      }
      if(course != null && lessons != null){
          srun(course, lessons);
      }
    } catch(ArgumentParserException e){
      parser.handleError(e);
      System.exit(0);
    }
    timeConsuming(begin, System.currentTimeMillis());
  }

  public static void srun(String filePath, String lessonId){
    String[] aryLessons = lessonId.split(",");
    WangyiClassSpider.CourseInfo courseInfo = WangyiClassSpider.readCourse(filePath);
    WangyiClassSpider.CourseInfo chiose = courseInfo;
    List<WangyiClassSpider.LessonInfo> lessons = new ArrayList<WangyiClassSpider.LessonInfo>();
    for(String lesson : aryLessons){
      for(WangyiClassSpider.LessonInfo lessonInfo : courseInfo.getLessons()){
        if(lessonInfo.getCount().equals(lesson)){
          lessons.add(lessonInfo);
          break;
        }
      }
    }
    chiose.setLessons(lessons);
    CourseLessonDownloaderByLessonId downloader = new CourseLessonDownloaderByLessonId(chiose);
    downloader.start();
  }

}
