import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.RandomAccessFile;

import java.beans.XMLEncoder;
import java.beans.XMLDecoder;
import java.net.HttpURLConnection;
import java.nio.channels.FileLock;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

public class CourseLessonDownloader extends Downloader {

  WangyiClassSpider.CourseInfo courseInfo;

  ThreadPoolSettings settings;

  static ThreadPoolExecutor lessonExecutor;

  static ThreadPoolExecutor taskExecutor;

  public CourseLessonDownloader(WangyiClassSpider.CourseInfo courseInfo){
    this.courseInfo = courseInfo;
    this.settings = ThreadPoolSettings.readSettings();
    if(this.settings == null){
      this.settings = new ThreadPoolSettings();
      ThreadPoolSettings.saveSettings(this.settings);
      //System.out.println(this.settings.getCorePoolSize());
      //System.out.println(this.settings.getMaximumPoolSize());
      //System.out.println(this.settings.getKeepAliveTime());
      //System.out.println(this.settings.getBlockingQueue());
    }
    if(CourseLessonDownloader.lessonExecutor == null)
      // create ThreadPoolExecutor
      // corePoolSize
      // maximumPoolSize
      // keepAliveTime
      // blockingQueue
      lessonExecutor = new ThreadPoolExecutor(
        this.settings.getCorePoolSize(),
        this.settings.getMaximumPoolSize(),
        this.settings.getKeepAliveTime(),
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<Runnable>(this.settings.getBlockingQueue()));

    if(CourseLessonDownloader.taskExecutor == null)
      // create ThreadPoolExecutor
      // corePoolSize
      // maximumPoolSize
      // keepAliveTime
      // blockingQueue
      taskExecutor = new ThreadPoolExecutor(
          DownloaderSettings.corePoolSize,
          DownloaderSettings.maximumPoolSize,
          DownloaderSettings.keepAliveTime,
          TimeUnit.MILLISECONDS,
          new ArrayBlockingQueue<Runnable>(DownloaderSettings.blockingQueue));
  }

  public void start(){
    for(WangyiClassSpider.LessonInfo lessonInfo : this.courseInfo.getLessons()){
      File file = new File(lessonInfo.getFileName());
      if(file.exists() && verify(lessonInfo.getSize(), lessonInfo.getFileName())){
        System.out.println(String.format("Lesson %s is early exists.", lessonInfo.getFileName()));
        continue;
      }

      if(lessonInfo.getLink() == null 
          || lessonInfo.getLink().trim().equals("")) continue;

      Task task = choiceTask(lessonInfo);
      FutureTask<String> futureTask = new FutureTask<String>(new Callable<String>(){
        @Override
        public String call() throws Exception {
          String message = processTask(task, MD5Util.md5sum(task.getUrl()));
          return message;
        }
      });
      try {
        lessonExecutor.execute(futureTask);
        String message = futureTask.get();
        System.out.println(message);
      } catch(Exception e){
        e.printStackTrace();
      }
    }
    try {
      taskExecutor.shutdown();
    } catch (Exception e){
      e.printStackTrace();
    }
    try {
      lessonExecutor.shutdown();
    } catch (Exception e){
      e.printStackTrace();
    }
  }
  // generator task from LessonInfo or xml
  private static Task choiceTask(WangyiClassSpider.LessonInfo lessonInfo){
      Task task = null;
      File fileConfig = new File(MD5Util.md5sum(lessonInfo.getLink()));
      if(!fileConfig.exists() || fileConfig.length() == 0){
        task = tasks(lessonInfo);
      } else {
        System.out.println(String.format("Read Task form: %s", fileConfig.getName()));
        task = readTask(fileConfig.getAbsolutePath());
      }
      List<String> headers = new ArrayList<String>();
      headers.add("Accept-Encoding=identity;q=1, *;q=0");
      headers.add("Accept-Language=en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7,zh-TW;q=0.6");
      headers.add("User-Agent=Mozilla/5.0 (Windows NT 6.1; WOW64; rv:45.0) Gecko/20100101 Firefox/45.0");
      headers.add("Accept=*/*");
      headers.add("Referer=%s");
      headers.add("Proxy-Connection=keep-alive");
      //headers.add("If-Range=^\\^\"lhjwS3cdFqwqoUJWZRbyxNH7ZMvJ^\\^\"");;
      StringBuffer strb = new StringBuffer();
      for(String data : headers){
        strb.append(data);
        strb.append("&");
      }
      String link = lessonInfo.getLink();
      link = String.format("%s %s", link, String.format(strb.toString(), link));
      Map<String, List<String>> header = URLConnectionUtil.getCustomerHeader(link);
      task.setCustomerHeaders(header);
      return task;
  }

  private static String processTask(Task task, String fileConfig){
    String message = "";
    List<TaskEntity> taskList = task.getTaskEntity();
    Collection<Future<?>> futures = new LinkedList<Future<?>>();
    try {
      for(TaskEntity taskEntity : taskList){
        System.out.println("task entity :" + taskEntity.getFinished());
        if(taskEntity.getFinished()) continue;
        CRunnable ctask = new CRunnable(taskEntity, task, new File(fileConfig), Downloader.simpleConnection);
        //CourseLessonDownloader.taskExecutor.execute(ctask);
        Future<?> future = CourseLessonDownloader.taskExecutor.submit(ctask);
        futures.add(future);
      }
      for(Future<?> future : futures){
        Object object = future.get();
        System.out.println(String.format("Future : %s", message));
      }
      message = String.format("%s finished.", task.getFileName());
      mergeMulitTaskFile(task);
    } catch(Exception e){
      e.printStackTrace();
    }
    return message;
  }

  public static void mergeMulitTaskFile(Task task){
    long begin = System.currentTimeMillis();
    if(task.getTaskEntity() == null || task.getTaskEntity().size() == 0){
      System.out.println("There is no file to merge");
      return;
    }
    File[] files = new File[task.getTaskEntity().size()];
    int index = 0;
    for(TaskEntity taskEntity : task.getTaskEntity()){
      files[index] = new File(task.getFileName() + taskEntity.getTaskId());
      index++;
    }
    FileUtil.mergeFiles(files, new File(task.getFileName()));
    if(verify(task.getContentLength(), task.getFileName())){
      deleteMergeFile(task);
    }
    Downloader.timeConsuming(begin, System.currentTimeMillis());
  }

  public static boolean verify(long contentLength, String fileName){
    boolean verifyResult = false;
    File file = new File(fileName);
    if(contentLength > 0 && file.exists()){
      if(contentLength == file.length()){
        verifyResult = true;
      }
    }
    return verifyResult;
  }

  public static void deleteMergeFile(Task task){
    if(task == null || task.getTaskEntity().size() == 0) return;
    for(TaskEntity taskEntity : task.getTaskEntity()){
      File file = new File(task.getFileName() + taskEntity.getTaskId());
      if(file.exists()) file.delete();
    }
  }

  public static Task tasks(WangyiClassSpider.LessonInfo lessonInfo){
    String link = lessonInfo.getLink();
    //link = String.format("%s Referer=%s&Accept-Encoding=identity", link, link);
    long contentLength = lessonInfo.getSize();
    String url = (link.indexOf(" ") == -1) ? link : link.substring(0, link.indexOf(" "));
    Task task = new Task();
    task.setFileName(lessonInfo.getFileName());
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
      String message = "Task:%d; Begin:%s; End:%s";
      // run task
      for(int i = 0; i < tasks.length; i++){

        System.out.println(String.format(message, 
                                         (i + 1), 
                                         String.valueOf(tasks[i][0]),
                                         String.valueOf(tasks[i][1])));

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

  public static void backup(String fileName){
    File file = new File(fileName);
    if(!file.exists()) return;
    FileLock lock = null;
    FileChannel fileChannel = null;
    FileChannel backupChannel = null;
    try {
      fileChannel = new FileInputStream(file).getChannel();
      //lock = fileChannel.lock();
      backupChannel = new FileOutputStream(String.format("%s.bak", fileName)).getChannel();
      fileChannel.transferTo(0, file.length(), backupChannel);
    } catch(Exception e){
      e.printStackTrace();
    } finally {
      try {
        if(fileChannel != null) fileChannel.close();
      } catch(Exception e){
        e.printStackTrace();
      }
      try {
        if(backupChannel != null) backupChannel.close();
      } catch(Exception e){
        e.printStackTrace();
      }
      try {
        if(lock != null) lock.release();
      } catch(Exception e){
        e.printStackTrace();
      }
    }
  }

  public static void saveTask(String fileName, Task task){
    XMLEncoder xmlEncoder = null;
    try {
      synchronized(task){
        backup(fileName);
        xmlEncoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(fileName)));
        xmlEncoder.writeObject(task);
      }
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

  public static Task readTask(String fileName){
    XMLDecoder xmlDecoder = null;
    Task task = null;
    try {
      xmlDecoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(fileName)));
      task = (Task)xmlDecoder.readObject();
    } catch(Exception e){
      e.printStackTrace();
    } finally {
      try {
        if(xmlDecoder != null) xmlDecoder.close();
      } catch(Exception e){
        e.printStackTrace();
      }
    }
    return task;
  }

  public static class CRunnable implements Runnable {
    //private String fileName;
    private TaskEntity taskEntity;
    private Task task;
    private File fileConfig;
    private SimpleURLConnection simpleConnection;
    public CRunnable(TaskEntity taskEntity,
                     Task task, 
                     File fileConfig, 
                     SimpleURLConnection simpleConnection){
      //this.fileName = fileName;
      this.taskEntity = taskEntity;
      this.task = task;
      this.fileConfig = fileConfig;
      this.simpleConnection = simpleConnection;
    }

    @Override
    public void run(){
      long begin = System.currentTimeMillis();
      while(!taskEntity.getFinished()){
        try{
          download(taskEntity, fileConfig);
          CourseLessonDownloader.saveTask(fileConfig.getAbsolutePath(), this.task);
        }catch(Exception e){
          e.printStackTrace();
        }
      }
      Downloader.timeConsuming(begin, System.currentTimeMillis());
    }

    public void download(TaskEntity taskEntity, File fileConfig){
      System.out.println(String.format("%d TaskEntity start", taskEntity.getTaskId()));
      if(taskEntity.getRead() > (taskEntity.getEnd() - taskEntity.getBegin())
        || (taskEntity.getBegin() + taskEntity.getRead()) == taskEntity.getContentLength()){
        taskEntity.setFinished(true);
        System.out.println(String.format("%d TaskEntity finished", taskEntity.getTaskId()));
      }
      HttpURLConnection conn = null;
      InputStream is = null;
      FileOutputStream fos = null;
      RandomAccessFile randomFile = null;
      File file = new File(task.getFileName() + taskEntity.getTaskId());
      try{
        if(!file.exists()){
          file.createNewFile();
        }
        System.out.println(file.getAbsolutePath());
        randomFile = new RandomAccessFile(file, "rw");
        // 文件长度，字节数
        //randomFile.seek(taskEntity.getBegin() + taskEntity.getRead());
        randomFile.seek(taskEntity.getRead());
        conn = simpleConnection.getConnection(taskEntity.getUrl(), DownloaderSettings.METHOD, DownloaderSettings.timeout, Long.toString(taskEntity.getBegin() + taskEntity.getRead()), Long.toString(taskEntity.getEnd()));
        URLConnectionUtil.setRequestHeader(conn, task.getCustomerHeaders());
        if(DownloaderSettings.readHeader){
          System.out.println("Request Header:");
          URLConnectionUtil.readHeader(conn.getRequestProperties());
          System.out.println();
        }
        int code = conn.getResponseCode();
        if(code == 416) {
          taskEntity.setFinished(true);
          System.out.println(String.format("%d TaskEntity finished", taskEntity.getTaskId()));
          return;
        }
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
          if(size > 5242880){
          //if(size > DownloaderSettings.unit){
             size = 0;
             //size = size % DownloaderSettings.unit;
             CourseLessonDownloader.saveTask(fileConfig.getAbsolutePath(), this.task);
            //FileUtil.writeConfig(fileConfig, JSON.toJSONString(task));
          }
          // String message = String.format("Total Read : %d ; Content-Length : %d ; left : %d ;", (taskEntity.getBegin() + taskEntity.getRead()), taskEntity.getContentLength(), (taskEntity.getContentLength() - (taskEntity.getBegin() + taskEntity.getRead())));
          // Downloader.cleanConsoleLine(message.length());
          // System.out.print(message);
        }
        if(taskEntity.getRead() > (taskEntity.getEnd() - taskEntity.getBegin())
          || (taskEntity.getBegin() + taskEntity.getRead()) == taskEntity.getContentLength()){
          taskEntity.setFinished(true);
          System.out.println(String.format("%d TaskEntity finished", taskEntity.getTaskId()));
        }
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
  }

  public static class ThreadPoolSettings {
    private int corePoolSize = 1;
    private int maximumPoolSize = 1;
    private int keepAliveTime = 2;
    private int blockingQueue = 350;
    public static final String SETTINGS = "thread_pool_settings.xml";

    public static void saveSettings(ThreadPoolSettings settings){
      XMLEncoder xmlEncoder = null;
      try {
        xmlEncoder = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(SETTINGS)));
        xmlEncoder.writeObject(settings);
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
  
    public static ThreadPoolSettings readSettings(){
      XMLDecoder xmlDecoder = null;
      ThreadPoolSettings settings = null;
      try {
        xmlDecoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(SETTINGS)));
        settings = (ThreadPoolSettings)xmlDecoder.readObject();
      } catch(Exception e){
        e.printStackTrace();
      } finally {
        try {
          if(xmlDecoder != null) xmlDecoder.close();
        } catch(Exception e){
          e.printStackTrace();
        }
      }
      return settings;
    }

      public int getCorePoolSize(){ return this.corePoolSize; }
      public void setCorePoolSize(int corePoolSize){ this.corePoolSize = corePoolSize; }

      public int getMaximumPoolSize(){ return this.maximumPoolSize; }
      public void setMaximumPoolSize(int maximumPoolSize){ this.maximumPoolSize = maximumPoolSize; }

      public int getKeepAliveTime(){ return this.keepAliveTime; }
      public void setKeepAliveTime(int keepAliveTime){ this.keepAliveTime = keepAliveTime; }

      public int getBlockingQueue(){ return this.blockingQueue; }
      public void setBlockingQueue(int blockingQueue){ this.blockingQueue = blockingQueue; }
  }  

}
