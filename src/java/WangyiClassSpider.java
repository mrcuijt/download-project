import java.io.File;
import java.io.RandomAccessFile;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;

import java.net.HttpURLConnection;

import java.beans.XMLEncoder;
import java.beans.XMLDecoder;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.impl.Arguments;

/**
 *  courseLearn.html
 */
public class WangyiClassSpider {

 public static SimpleURLConnection simpleConnection;

  // PlanNewBean.getPlanCourseDetail.dwr
  public static String courseDetail = "https://study.163.com/dwr/call/plaincall/PlanNewBean.getPlanCourseDetail.dwr?%s ";

  // LessonLearnBean.getVideoLearnInfo.dwr
  public static String videoLearnInfo = "https://study.163.com/dwr/call/plaincall/LessonLearnBean.getVideoLearnInfo.dwr?%s ";

  // VOD API
  public static String vod = "https://vod.study.163.com/eds/api/v1/vod/video?videoId=%s&signature=%s&clientType=1";

  public static String appId = "400000000387008";

  public static void main(String[] args){
    
    //if(args == null || args.length == 0) return;

    ArgumentParser parser = ArgumentParsers.newFor("WangyiClassSpider").build()
        .defaultHelp(true)
        .description("网易云课堂视频下载工具");
    parser.addArgument("-p", "--proxy").action(Arguments.storeTrue())
        .dest("proxy")
        .help("use proxy by proxy_settings.ini");
    parser.addArgument("-r", "--request").action(Arguments.storeTrue())
        .dest("request")
        .help("request course form study.163.com");
    parser.addArgument("-cid", "--courseid")
        .dest("courseId")
        .help("-r -cid file : get course info");
    parser.addArgument("-u", "--update").action(Arguments.storeTrue())
        .dest("update")
        .help("-ru file : update course xml video info");
    parser.addArgument("-d", "--download").action(Arguments.storeTrue())
        .dest("download")
        .help("download course xml video");
    parser.addArgument("-v", "--view").action(Arguments.storeTrue())
        .dest("view")
        .help("-v file : view course lesson video link;");
    parser.addArgument("-m", "--merge").action(Arguments.storeTrue())
        .dest("merge")
        .help("-m merge lessoName from file[detail] to file[xml]");
    parser.addArgument("-t", "--test").action(Arguments.storeTrue())
        .dest("test")
        .help("-t file test read lesson video info");
    parser.addArgument("file").nargs("*")
        .dest("file")
        .help("input course xml file");
    //if(args.length == 2){
    //  if(args[1].equals("proxy")){
    //    URLConnectionUtil.setProxy();
    //  }
    //}
    //init();
    long begin = System.currentTimeMillis();
    //run(args[0]);
    Namespace ns = null;
    try {
      ns = parser.parseArgs(args);
      Boolean proxy = ns.getBoolean("proxy");
      Boolean request = ns.getBoolean("request");
      String courseId = ns.getString("courseId");
      Boolean update = ns.getBoolean("update");
      Boolean download = ns.getBoolean("download");
      Boolean view = ns.getBoolean("view");
      Boolean merge = ns.getBoolean("merge");
      Boolean test = ns.getBoolean("test");
      List<String> files = ns.getList("file");
      System.out.println(String.format("proxy:%s; request:%s; courseId:%s; update:%s; download:%s; files:%s;", String.valueOf(proxy), String.valueOf(request), courseId, String.valueOf(update), String.valueOf(download), String.valueOf(files)));
      if(proxy){
        URLConnectionUtil.setProxy();
      }
      init();
      if(courseId != null && request){
        run(courseId);
      }
      if(update && request && files.size() > 0){
        for(String file : files){
          updateCourseLessonsInfo(file);
        }
      }
      if(view && files.size() > 0){
        for(String file : files){
          viewCourseLessonVideoLink(file);
        }
      }
      if(merge && files.size() == 2){
        mergeCourseLessonName(files.get(0), files.get(1));
      }
      if(test && files.size() > 0){
        for(String file : files){
          updateVideoLink(new LessonInfo(), file);
        }
      }
      if(download && files.size() > 0){
        for(String file : files){
          download(file);
        }
      }
    } catch(ArgumentParserException e){
      parser.handleError(e);
      System.exit(0);
    }
    
    Downloader.timeConsuming(begin, System.currentTimeMillis());
  }

  public static void run(String courseId){
    String link = getCourseLink(courseId);
    String method = "POST";
    String fileName = request(link, method);
    CourseInfo courseInfo = getCourseInfo(courseId, fileName);
    String xml = String.format("course_%s.xml", courseId);
    saveCourse(xml, courseInfo);
  }

  public static void viewCourseLessonVideoLink(String fileName){
    CourseInfo courseInfo = readCourse(fileName);
    for(LessonInfo lesson : courseInfo.getLessons()){
      String info = "LessonId:%s; LessonName:%s; Link:%s;";
      System.out.println(String.format(info, lesson.getCount(), 
                                             lesson.getLessonName(),
                                             lesson.getLink()));
    }
  }

  public static void mergeCourseLessonName(String detail, String xml){
    count = 0;
    CourseInfo courseInfo = readCourse(xml);
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(detail));
      String line = "";
      while((line = br.readLine()) != null){
        LessonInfo lesson = parseLessonInfo(line);
        if(lesson != null && count <= courseInfo.getLessons().size()){
          courseInfo.getLessons().get(count -1).setLessonName(lesson.getLessonName());
        }
      }
      saveCourse(xml, courseInfo);
    } catch(Exception e){
      e.printStackTrace();
    } finally {
      try {
        if(br != null) br.close();
      } catch(Exception e){
        e.printStackTrace();
      }
    }
  }

  public static void download(String fileName){
    CourseInfo courseInfo = readCourse(fileName);
    if(courseInfo == null || courseInfo.getLessons().size() == 0) return;
    //CourseInfo course = new CourseInfo();
    //course.setCourseId(courseInfo.getCourseId());
    //course.setLessons(Arrays.asList(new LessonInfo[]{
    //                                        courseInfo.getLessons().get(0), 
    //                                        courseInfo.getLessons().get(1)}
    //                               )
    //                 );
    CourseLessonDownloader downloader = new CourseLessonDownloader(courseInfo);
    downloader.start();
  }

  public static void updateCourseLessonsInfo(String fileName){
    CourseInfo courseInfo = readCourse(fileName);
    System.out.println(courseInfo);
    if(courseInfo == null) return;
    for(LessonInfo lesson : courseInfo.getLessons()){
      //System.out.println(String.format("LessonId:%s; LessonName:%s;", lesson.getVideoInfoId(), lesson.getLessonName()));
      updateLessonInfo(lesson, courseInfo.getCourseId());
    }
    saveCourse(fileName, courseInfo);
  }

  public static void updateLessonInfo(LessonInfo lessonInfo, String courseId){
    String link = getVideoInfoLink(lessonInfo, courseId);
    String method = "POST";
    String fileName = request(link, method);
    updateVideoInfo(lessonInfo, fileName);
    String videoLink = getVideoLink(lessonInfo);
    method = "GET";
    fileName = request(videoLink, method);
    updateVideoLink(lessonInfo, fileName);
  }

  public static void saveCourse(String fileName, CourseInfo courseInfo){
    XMLEncoder xmlEncoder = null;
    try {
      xmlEncoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(fileName)));
      xmlEncoder.writeObject(courseInfo);
    } catch(Exception e){
      e.printStackTrace();
    } finally {
      try {
        if(xmlEncoder != null) xmlEncoder.close();
      } catch(Exception e){
        e.printStackTrace();
      }
    }
  }

  public static CourseInfo readCourse(String fileName){
    XMLDecoder xmlDecoder = null;
    CourseInfo courseInfo = null;
    try {
      xmlDecoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(fileName)));
      courseInfo = (CourseInfo)xmlDecoder.readObject();
    } catch(Exception e){
      e.printStackTrace();
    } finally {
      try {
        if(xmlDecoder != null) xmlDecoder.close();
      } catch(Exception e){
        e.printStackTrace();
      }
    }
    return courseInfo;
  }

  public static int count = 0;
  public static CourseInfo getCourseInfo(String courseId, String fileName){
    count = 0;
    BufferedReader br = null;
    CourseInfo courseInfo = new CourseInfo();
    courseInfo.setCourseId(courseId);
    List<LessonInfo> lessons = new ArrayList<LessonInfo>();
    try {
      br = new BufferedReader(new FileReader(fileName));
      String line = "";
      while((line = br.readLine()) != null){
        LessonInfo lesson = parseLessonInfo(line);
        if(lesson != null) lessons.add(lesson);
      }
      courseInfo.setLessons(lessons);
    } catch(Exception e){
      e.printStackTrace();
    } finally {
      try {
        if(br != null) br.close();
      } catch(Exception e){
        e.printStackTrace();
      }
    }
    return courseInfo;
  }

  public static LessonInfo parseLessonInfo(String line){
    if(line.indexOf("audioTime") == -1) return null;
    count++;
    System.out.print(String.format("Lesson %s ", String.valueOf(count + 1000).substring(1)));
    String sid = ".id";
    String slessonName = "lessonName";
    int videoInfoIdIndex = line.indexOf(sid);
    int lessonNameIndex = line.indexOf(slessonName);
    String temp = line.substring(videoInfoIdIndex + sid.length() + 1);
    String videoInfoId = temp.substring(0, temp.indexOf(";"));
    //temp = temp.substring(lessonNameIndex + 2);
    //String lessonName = temp.substring(0, temp.indexOf("\""));
    temp = line.substring(lessonNameIndex + slessonName.length() + 2);
    String lessonName = temp.substring(0, temp.indexOf("\""));
    LessonInfo lesson = new LessonInfo();
    lesson.setCount(String.valueOf(count + 1000).substring(1));
    lesson.setVideoInfoId(videoInfoId);
    lesson.setLessonName(decodeUnicode(lessonName));
    //lessons.add(lesson);
    System.out.println(String.format("VideoInfoId : %s ; LessonName : %s ;", videoInfoId, decodeUnicode(lessonName)));
    return lesson;
  }

  public static void updateVideoInfo(LessonInfo lessonInfo, String fileName){
    BufferedReader br = null;
    try {
      String signature = "signature=\"";
      String videoId = "videoId=";
      String name = ",name:\"";

      String line = "";
      br = new BufferedReader(new FileReader(fileName));
      while((line = br.readLine()) != null){
        int signatureIndex = line.indexOf(signature);
        int videoIdIndex = line.indexOf(videoId);

        String ssignature = "";
        String svideoId = "";

        if(signatureIndex > -1){
          String temp = line.substring(signatureIndex + signature.length());
          ssignature = temp.substring(0, temp.indexOf("\""));
          temp = line.substring(videoIdIndex + videoId.length());
          svideoId = temp.substring(0, temp.indexOf(";"));
          lessonInfo.setSignature(ssignature);
          lessonInfo.setVideoId(svideoId);
        } else {
          continue;
        }
      }
    } catch(Exception e){
      e.printStackTrace();
    } finally {
      try {
        if(br != null) br.close();
      } catch(Exception e){
        e.printStackTrace();
      }
    }
    //System.out.println(String.format("Video Name: %s ; Video Id: %s ; Signature: %s ;", sname, svideoId, ssignature));
  }

  public static void updateVideoLink(LessonInfo lessonInfo, String fileName){
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(fileName));
      String line = br.readLine();
      if(line == null) return;
      //System.out.println(line);
      int mp4Index = line.lastIndexOf("\"format\":\"mp4\"");
      //System.out.println(mp4Index);
      if(mp4Index == -1) return;
      String temp = line.substring(0, mp4Index);
      String dlink = "\"videoUrl\":\"";
      int dlinkIndex = temp.lastIndexOf(dlink);
      //System.out.println(dlinkIndex);
      String downloadLink = temp.substring(dlinkIndex + dlink.length(), temp.lastIndexOf("\""));
      //System.out.println(downloadLink);
      temp = temp.substring(0, dlinkIndex);
      String splitSize = "\"size\":";
      int sizeIndex = temp.lastIndexOf(splitSize);
      String ssize = temp.substring(sizeIndex + splitSize.length(), temp.lastIndexOf(","));
      try {
        //System.out.println(String.format("size:%s",ssize));
        lessonInfo.setSize(Long.valueOf(ssize));
      } catch(Exception e){
        e.printStackTrace();
      }
      lessonInfo.setLink(downloadLink);
    } catch(Exception e){
      e.printStackTrace();
    } finally {
      try {
        if(br != null) br.close();
      } catch(Exception e){
        e.printStackTrace();
      }
    }
  }

  public static String request(String link, String method){
    String url = (link.indexOf(" ") == -1) ? link : link.substring(0, link.indexOf(" "));
    method = (method == null || method.length() == 0) ? "GET" : method.toUpperCase();
    String payload = getPayload(link);
    HttpURLConnection conn = null;
    InputStream is = null;
    FileOutputStream fos = null;
    RandomAccessFile randomFile = null;
    String fileName = Downloader.getFileName(url);
    File file = new File(String.format("%s_%s", String.valueOf(System.currentTimeMillis()), fileName));
    try{
      if(link.indexOf(" ") > -1){
        link = link.substring(0, link.lastIndexOf(" "));
      }
      conn = simpleConnection.getConnection(url, method, DownloaderSettings.timeout);
      URLConnectionUtil.setRequestHeader(conn, URLConnectionUtil.getCustomerHeader(link));
      if(DownloaderSettings.readHeader){
        System.out.println("Request Header:");
        URLConnectionUtil.readHeader(conn.getRequestProperties());
        System.out.println();
      }
      // Set Request Payload
      setPayload(conn, payload);
      int code = conn.getResponseCode();
      String codeMessage = conn.getResponseMessage();
      System.out.println(String.format("Response Code : %d ; Response Message : %s ;", code, codeMessage));
      if(DownloaderSettings.readHeader){
        System.out.println("Response Header:");
        URLConnectionUtil.readHeader(conn.getHeaderFields());
        System.out.println();
      }
      is = conn.getInputStream();
      System.out.println(file.getAbsolutePath());
      fos = new FileOutputStream(file);
      //randomFile = new RandomAccessFile(file, "rw");
      // Content-Length
      long contentLength = URLConnectionUtil.getContentLength(conn.getHeaderFields());
      // buffer
      byte[] buffer = new byte[1024];
      // Total Reader
      int total = 0;
      // Current Reader
      int len = 0;
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      while((len = is.read(buffer, 0, buffer.length)) != -1){
        total += len;
        baos.write(buffer, 0, len);
        fos.write(buffer, 0, len);
        //randomFile.write(buffer, 0, len);
        String message = String.format("Total Read : %d ; Content-Length : %d ; left : %d ;", total, contentLength, contentLength - total);
        Downloader.cleanConsoleLine(message.length());
        System.out.print(message);
      }
      fos.flush();
      System.out.println();
      //System.out.println(new String(baos.toByteArray(), "UTF-8"));
    }catch(Exception e){
      e.printStackTrace();
    }finally{
      try{
        if(is != null) is.close();
      }catch(Exception e){
        e.printStackTrace();
      }
      try{
        if(fos != null) fos.close();
      } catch(Exception e){
        e.printStackTrace();
      }
      try{
        if(randomFile != null) randomFile.close();
      }catch(Exception e){
        e.printStackTrace();
      }
      try{
        if(conn != null){
          conn.disconnect();
          conn = null;
        }
      }catch(Exception e){
        e.printStackTrace();
      }
    }
    return file.getName();
  }

  public static String getPayload(String link){
    String payload = "";
    if(link.indexOf(" ") == -1) return payload;
    String[] datas = link.split(" ");
    if(datas.length < 3) return payload;
    int index = link.lastIndexOf(" ");
    String temp = link.substring(index + 1);
    if(temp.indexOf("&") == -1){
      payload = temp;
      return payload;
    }
    String lf = new String(new byte[]{0x0A});
    // reset datas
    datas = temp.split("&");
    StringBuffer strb = new StringBuffer();
    for(int i = 0; i < datas.length; i++){
      strb.append(datas[i]);
      if((i + 1) < datas.length){
        strb.append(lf);
      }
    }
    payload = strb.toString();
    
    return payload;
  }

  public static void setPayload(HttpURLConnection conn, String payload){
    if(conn == null) return;
    if(payload == null || payload.trim().equals("")) return;
    OutputStream os = null;
    PrintWriter pw = null;
    try{
      System.out.println(String.format("Payload : %s \r\n", payload));
      os = conn.getOutputStream();
      pw = new PrintWriter(new OutputStreamWriter(os));
      pw.write(payload);
      pw.close();
    } catch(Exception e){
      e.printStackTrace();
    } finally {
      try{
        if(pw != null) pw = null;
      }catch(Exception e){
        e.printStackTrace();
      }
      try{
        if(os != null) os = null;
      }catch(Exception e){
        e.printStackTrace();
      }
    }
  }

  public static String getCourseLink(String couserId){
    // Request URL
    String uri = String.format(courseDetail, String.valueOf(System.currentTimeMillis()));
    // Request Header
    String header = "";
    // Request Cookie
    String cookie = "";
    // Request Payload
    String payload = "";

    StringBuffer strbCookie = new StringBuffer();
    strbCookie.append("");
    cookie = strbCookie.toString();

    StringBuffer strbHeader = new StringBuffer();
    strbHeader.append("sec-fetch-mode=cors");
    strbHeader.append("&origin=https://study.163.com");
    strbHeader.append(String.format("&cookie=%s", cookie));
    strbHeader.append(String.format("&providerid=%s", appId));
    strbHeader.append("&authority=study.163.com");
    strbHeader.append("&scheme=https");
    strbHeader.append("&sec-fetch-site=same-origin");
    strbHeader.append("&method=POST");
    strbHeader.append("&Content-Type=text/plain");
    strbHeader.append(" ");
    header = strbHeader.toString();

    StringBuffer strbPayload = new StringBuffer();
    strbPayload.append("callCount=1");
    strbPayload.append("&scriptSessionId=${scriptSessionId}190");
    strbPayload.append("&httpSessionId=61c0946935b143b3a0f7957eac10a537");
    strbPayload.append("&c0-scriptName=PlanNewBean");
    strbPayload.append("&c0-methodName=getPlanCourseDetail");
    strbPayload.append("&c0-id=0");
    strbPayload.append(String.format("&c0-param0=string:%s", couserId));
    strbPayload.append("&c0-param1=number:0");
    strbPayload.append("&c0-param2=null:null");
    strbPayload.append(String.format("&batchId=%s", String.valueOf(System.currentTimeMillis())));
    payload = strbPayload.toString();

    return String.format("%s%s%s", uri, header, payload);
  }

  public static String getVideoInfoLink(LessonInfo lesson, String courseId){
    // video Info Id
    String videoInfoId = lesson.getVideoInfoId();
    // Request URL
    String url = String.format(videoLearnInfo, String.valueOf(System.currentTimeMillis()));
    // Request Header
    String header = "";
    // Request Payload
    String payload = "";
    // Request Cookie
    String cookie = "";

    StringBuffer strbCookie = new StringBuffer();
    cookie = strbCookie.toString();

    StringBuffer strbHeader = new StringBuffer();
    strbHeader.append("sec-fetch-mode=cors");
    strbHeader.append("&origin=https://study.163.com");
    strbHeader.append(String.format("&cookie=%s", cookie));
    strbHeader.append(String.format("&providerid=%s", appId));
    strbHeader.append("&authority=study.163.com");
    strbHeader.append("&scheme=https");
    strbHeader.append("&sec-fetch-site=same-origin");
    strbHeader.append("&method=POST");
    strbHeader.append("&Content-Type=text/plain");
    strbHeader.append(" ");
    header = strbHeader.toString();

    StringBuffer strbPayload = new StringBuffer();
    strbPayload.append("callCount=1");
    strbPayload.append("&scriptSessionId=${scriptSessionId}190");
    strbPayload.append("&httpSessionId=0fc8cf78af2b4ccc9a4540a71f210feb");
    strbPayload.append("&c0-scriptName=LessonLearnBean");
    strbPayload.append("&c0-methodName=getVideoLearnInfo");
    strbPayload.append("&c0-id=0");
    strbPayload.append(String.format("&c0-param0=string:%s", videoInfoId));
    strbPayload.append(String.format("&c0-param1=string:%s", courseId));
    strbPayload.append(String.format("&batchId=%s", String.valueOf(System.currentTimeMillis())));
    payload = strbPayload.toString();

    return String.format("%s%s%s", url, header, payload);
  }

  public static String getVideoLink(LessonInfo lesson){
    // Request URL
    return String.format(vod ,lesson.getVideoId(), lesson.getSignature());
  }

  public static String decodeUnicode(String unicode){
    if(unicode == null) return "NULL";
    List<String> list = new ArrayList<String>();
    String reg = "\\\\u[0-9,a-f,A-F]{4}";
    Pattern pattern = Pattern.compile(reg);
    Matcher matcher = pattern.matcher(unicode);
    while(matcher.find()){
      list.add(matcher.group());
    }
    for(int i = 0, j = 2; i < list.size(); i++){
      String code = list.get(i).substring(j, j + 4);
      char ch = (char) Integer.parseInt(code, 16);
      unicode = unicode.replace(list.get(i), String.valueOf(ch));
    }
    return unicode;
  }

  public static void init(){
    simpleConnection = new SimpleURLConnection();
  }

  public static class CourseInfo {

    // Course Id
    String courseId;
    // Course Lessons
    List<LessonInfo> lessons;

    public String getCourseId(){ return this.courseId; }
    public void setCourseId(String courseId){ this.courseId = courseId; }

    public List<LessonInfo> getLessons(){ return this.lessons; }
    public void setLessons(List<LessonInfo> lessons){ this.lessons = lessons; }
  }

  public static class LessonInfo {
    // Lesson Count
    String count;
    // Video Info Id
    String videoInfoId;
    // Lesson Name
    String lessonName;
    // Video Id
    String videoId;
    // Signature
    String signature;
    // Video Link
    String link;
    // Video Size
    long size;
    // File Name
    String fileName;

    public String getCount(){ return count; }
    public void setCount(String count){ this.count = count; }

    public String getVideoInfoId(){ return videoInfoId; }
    public void setVideoInfoId(String videoInfoId){ this.videoInfoId = videoInfoId; }

    public String getLessonName(){ return this.lessonName; }
    public void setLessonName(String lessonName){ this.lessonName = lessonName; }

    public String getVideoId(){ return this.videoId; }
    public void setVideoId(String videoId){ this.videoId = videoId; }

    public String getSignature(){ return this.signature; }
    public void setSignature(String signature){ this.signature = signature; }

    public String getLink(){ return this.link; }
    public void setLink(String link){ this.link = link; }

    public long getSize(){ return this.size; }
    public void setSize(long size){ this.size = size; }

    public String getFileName(){ return String.format("%s-%s.mp4", this.getCount(), this.getLessonName()); }
  }

}
