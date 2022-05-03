import java.io.*;
import java.net.*;
import java.util.*;
import com.alibaba.fastjson.JSON;

public class Downloader {

  public static SimpleURLConnection simpleConnection;
  public static ProxyURLConnection proxyConnection;

  public Downloader(){
    DownloaderSettings.init();
    simpleConnection = new SimpleURLConnection();
  }

  public static void main(String[] args){
    long begin = System.currentTimeMillis();
    Downloader downloader = new Downloader();
    if(args == null || args.length == 0) return;
    if(args.length == 2){
      if(args[1].equals("proxy")){
        URLConnectionUtil.setProxy();
      }
    }
    downloader.run(args[0]);
    timeConsuming(begin, System.currentTimeMillis());
  }

  public void run(String filePath){
    try{
      BufferedReader br = new BufferedReader(new FileReader(filePath));
      String link = "";
      while((link = br.readLine()) != null && link.trim().length() > 0){
        start(link);
      }
    }catch(Exception e){
      e.printStackTrace();
    }
  }

  public void start(String link){
    String url = (link.indexOf(" ") == -1) ? link : link.substring(0, link.indexOf(" "));
    Map<String, List<String>> headers = URLConnectionUtil.getHeaders(url);
    long contentLength = URLConnectionUtil.getContentLength(headers);
    List<TaskEntity> taskList = new ArrayList<TaskEntity>();
    if(URLConnectionUtil.isSupportRange(headers)){
      File fileConfig = new File(MD5Util.md5sum(url));
      if(fileConfig.exists()){
        System.out.println("Read Task From Config: ");
        task = readTaskConfig(fileConfig);
      } else {
        task = tasks(link, contentLength);
        FileUtil.writeConfig(fileConfig, JSON.toJSONString(task));
      }
      task.setFileName(getFileName(task.getUrl()));
      taskList = task.getTaskEntity();
      if(taskList.size() == 0){
        download(url, contentLength);
        return;
      }
      int count = 0;
      for(TaskEntity taskEntity : taskList){
        if(DownloaderSettings.bbreak && count >= DownloaderSettings.bbreakcount){
          throw new RuntimeException(String.format("Block %d Is Finished", count));
        }
        keepDownlaod(taskEntity, fileConfig);
        FileUtil.writeConfig(fileConfig, JSON.toJSONString(task));
        count ++;
      }
    } else {
      task = InitFileConfig.generateConfigFile(link);
      download(url, contentLength);
    }
  }

  public static void keepDownlaod(TaskEntity taskEntity, File fileConfig){
    while(!taskEntity.getFinished()){
      try{
        download(taskEntity, fileConfig);
      }catch(Exception e){
        e.printStackTrace();
      }
    }
  }

  public static String getFileName(String url){
    if(url == null || url.length() == 0) return "NULL";
    if(url.endsWith("/")) url = url.substring(0, url.length() - 1);
    String fileName = url.substring(url.lastIndexOf("/") + 1);
    return fileName.indexOf("?") > 0 ? fileName.substring(0, fileName.lastIndexOf("?")) : fileName;
  }

  public static void download(TaskEntity taskEntity, File fileConfig){
    if(taskEntity.getRead() > (taskEntity.getEnd() - taskEntity.getBegin())
        || (taskEntity.getBegin() + taskEntity.getRead()) == taskEntity.getContentLength()){
      taskEntity.setFinished(true);
    }
    HttpURLConnection conn = null;
    InputStream is = null;
    FileOutputStream fos = null;
    RandomAccessFile randomFile = null;
    //File fileConfig = new File(MD5Util.md5sum(taskEntity.getUrl()));
    //File file = new File(getFileName(taskEntity.getUrl()));
    File file = new File(task.getFileName());
    try{
      if(!file.exists()){
        randomFile = new RandomAccessFile(file, "rw");
        randomFile.setLength(taskEntity.getContentLength());
        randomFile.close();
      }
      System.out.println(file.getAbsolutePath());
      randomFile = new RandomAccessFile(file, "rw");
      // 文件长度，字节数
      randomFile.seek(taskEntity.getBegin() + taskEntity.getRead());
      conn = simpleConnection.getConnection(taskEntity.getUrl(), DownloaderSettings.METHOD, DownloaderSettings.timeout, Long.toString(taskEntity.getBegin() + taskEntity.getRead()), Long.toString(taskEntity.getEnd()));
      URLConnectionUtil.setRequestHeader(conn, task.getCustomerHeaders());
      if(DownloaderSettings.readHeader){
        System.out.println("Request Header:");
        //URLConnectionUtil.readHeader(task.getCustomerHeaders());
        URLConnectionUtil.readHeader(conn.getRequestProperties());
        //URLConnectionUtil.readHeader(conn.getHeaderFields());
        System.out.println();
      }
      int code = conn.getResponseCode();
      String codeMessage = conn.getResponseMessage();
      System.out.println(String.format("Response Code : %d ; Response Message : %s ;", code, codeMessage));
      if(DownloaderSettings.readHeader){
        System.out.println("Response Header:");
        URLConnectionUtil.readHeader(conn.getHeaderFields());
      }
      System.out.println();
      is = conn.getInputStream();
      // buffer
      byte[] buffer = new byte[1024];
      // Total Reader
      int total = 0;
      // Current Reader
      int len = 0;
      // Unit Size Reader
      int size = 0;
      while((len = is.read(buffer, 0, buffer.length)) != -1){
        total += len;
        size += len;
        randomFile.write(buffer, 0, len);
        taskEntity.setRead(taskEntity.getRead() + len);
        if(size > DownloaderSettings.unit){
          size = size % DownloaderSettings.unit;
          FileUtil.writeConfig(fileConfig, JSON.toJSONString(task));
        }
        String message = String.format("Total Read : %d ; Content-Length : %d ; left : %d ;", (taskEntity.getBegin() + taskEntity.getRead()), taskEntity.getContentLength(), (taskEntity.getContentLength() - (taskEntity.getBegin() + taskEntity.getRead())));
        cleanConsoleLine(message.length());
        System.out.print(message);
      }
      if(taskEntity.getRead() > (taskEntity.getEnd() - taskEntity.getBegin())
          || (taskEntity.getBegin() + taskEntity.getRead()) == taskEntity.getContentLength()){
        taskEntity.setFinished(true);
      }
      //String datas = new String(baos.toByteArray());
      //System.out.println(datas);
      //System.out.println(String.format("Total Read : %d ", total));
    }catch(Exception e){
      e.printStackTrace();
    }finally{
      try{
        if(is != null) is.close();
      }catch(Exception e){
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
  }

  public static void download(String url, long contentLength){
    HttpURLConnection conn = null;
    InputStream is = null;
    FileOutputStream fos = null;
    RandomAccessFile randomFile = null;
    File file = new File(getFileName(url));
    try{
      if(!file.exists()){
        file.createNewFile();
      } else if(file.length() >= contentLength) {
        System.out.println("File Early Exists!");
        return;
      } else{
        System.out.println("File Not Support Range Restart Download");
      }
      System.out.println(file.getAbsolutePath());
      randomFile = new RandomAccessFile(file, "rw");
      conn = simpleConnection.getConnection(url, DownloaderSettings.timeout);
      URLConnectionUtil.setRequestHeader(conn, task.getCustomerHeaders());
      if(DownloaderSettings.readHeader){
        System.out.println("Request Header:");
        //URLConnectionUtil.readHeader(task.getCustomerHeaders());
        URLConnectionUtil.readHeader(conn.getRequestProperties());
        //URLConnectionUtil.readHeader(conn.getHeaderFields());
        System.out.println();
      }
      int code = conn.getResponseCode();
      String codeMessage = conn.getResponseMessage();
      System.out.println(String.format("Response Code : %d ; Response Message : %s ;", code, codeMessage));
      if(DownloaderSettings.readHeader){
        System.out.println("Response Header:");
        URLConnectionUtil.readHeader(conn.getHeaderFields());
      }
      System.out.println();
      is = conn.getInputStream();
      // buffer
      byte[] buffer = new byte[1024];
      // Total Reader
      int total = 0;
      // Current Reader
      int len = 0;
      while((len = is.read(buffer, 0, buffer.length)) != -1){
        total += len;
        randomFile.write(buffer, 0, len);
        String message = String.format("Total Read : %d ; Content-Length : %d ; left : %d ;", total, contentLength, contentLength - total);
        cleanConsoleLine(message.length());
        System.out.print(message);
      }
      //String datas = new String(baos.toByteArray());
      //System.out.println(datas);
      //System.out.println(String.format("Total Read : %d ", total));
    }catch(Exception e){
      e.printStackTrace();
    }finally{
      try{
        if(is != null) is.close();
      }catch(Exception e){
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
  }

  public static Task task;
  public static Task tasks(String link, long contentLength){
    String url = (link.indexOf(" ") == -1) ? link : link.substring(0, link.indexOf(" "));
    task = new Task();
    task.setUrl(url);
    task.setContentLength(contentLength);
    Map<String, List<String>> headers = URLConnectionUtil.getCustomerHeader(link);
    task.setCustomerHeaders(headers);
    List<TaskEntity> taskList = new ArrayList<TaskEntity>();
    // 比较总内容大小
    if(contentLength > DownloaderSettings.splitSize){
      System.out.println("Run Task Branch");
      int totalTask = (int)(contentLength / DownloaderSettings.splitSize);
      long left = 0;
      if(contentLength % DownloaderSettings.splitSize > 0){
        left = contentLength % DownloaderSettings.splitSize;
        totalTask += 1;
      }
      System.out.println("Total Task : " + totalTask);
      // 每三个任务并行执行，完成后开启下次三任务线程。
      // 单任务模板
      // 总任务记录
      long[][] tasks = new long[totalTask][2];
      long begin = 0;
      long end = 0;
      for(int i = 0; i < tasks.length; i++){
        if(i == 0){
          tasks[i][0] = begin;
          tasks[i][1] = DownloaderSettings.splitSize - 1;
          begin += DownloaderSettings.splitSize;
          end = tasks[i][1] + DownloaderSettings.splitSize;
          continue;
        }
        tasks[i][0] = begin;
        tasks[i][1] = end;
        begin += DownloaderSettings.splitSize;
        end = tasks[i][1] + DownloaderSettings.splitSize;
      }
      if(left > 0){
        tasks[tasks.length - 1][1] = tasks[tasks.length - 1][0] + left;
      }
      // run task
      for(int i = 0; i < tasks.length; i++){
        System.out.println("Task : " + (i + 1) + "; Begin :" + tasks[i][0] + "; End :" + tasks[i][1] + ";");
        TaskEntity taskEntity = new TaskEntity();
        taskEntity.setTaskId(i);
        taskEntity.setUrl(url);
        taskEntity.setBegin(tasks[i][0]);
        taskEntity.setEnd(tasks[i][1]);
        taskEntity.setContentLength(contentLength);
        taskList.add(taskEntity);
      }
    }else{
      System.out.println(String.format("File Size Small Than %d ", DownloaderSettings.splitSize));
    }
    task.setTaskEntity(taskList);
    return task;
  }

  public static Task readTaskConfig(File fileConfig){
    Task task = new Task();
    FileInputStream fis = null;
    try{
      BufferedReader br = new BufferedReader(new FileReader(fileConfig));
      String config = br.readLine();
      task = JSON.parseObject(config, Task.class);
    }catch(Exception e){
      e.printStackTrace();
    }finally{
      try{
        if(fis != null) fis.close();
      }catch(Exception e){
        e.printStackTrace();
      }
    }
    return task;
  }

  public static void timeConsuming(long begin, long end){
    long between = end - begin;
    long day = between / (24 * 60 * 60 * 1000);
    long hour = (between / (60 * 60 * 1000) - day * 24);
    long min = ((between / (60 * 1000)) - day * 24 * 60 - hour * 60);
    long s = (between / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60);
    System.out.println();
    System.out.println(String.format("耗时: %d 天 %d 时 %d 分", day, hour, min));
  }

  public static void cleanConsoleLine(int length){
    for(int i = 0; i < length; i++){
      System.out.print("\b");
    }
  }

}
