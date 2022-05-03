import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import com.alibaba.fastjson.JSON;

public class MulitTaskDownloader {

  public static Task task;
  public MulitTaskDownloader(Task task){
    MulitTaskDownloader.task = task;
  }

  public static void main(String[] args){
    //ExecutorService executor = Executors.newCachedThreadPool();

    DownloaderSettings.init();

    // create ThreadPoolExecutor
    // corePoolSize
    // maximumPoolSize
    // keepAliveTime
    // blockingQueue
    ThreadPoolExecutor executor =  
      new ThreadPoolExecutor(
        DownloaderSettings.corePoolSize,
        DownloaderSettings.maximumPoolSize,
        DownloaderSettings.keepAliveTime,
        TimeUnit.MILLISECONDS,
        new ArrayBlockingQueue<Runnable>(DownloaderSettings.blockingQueue));

    // 显示声明线程池拒绝策略
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());

    SimpleURLConnection simple = new SimpleURLConnection();
    MulitTaskDownloader downloader = null;
    if(args == null || args.length == 0) return;
    if(args.length == 2){
      if(args[1].equals("proxy")){
        URLConnectionUtil.setProxy();
      }
    }
    File fileConfig = new File(args[0]);
    Task task = Downloader.readTaskConfig(fileConfig);
    downloader = new MulitTaskDownloader(task);
    List<TaskEntity> taskList = task.getTaskEntity();
    try {
      for(TaskEntity taskEntity : taskList){
        if(taskEntity.getFinished()) continue;
        CRunnable ctask = new CRunnable(taskEntity, fileConfig, simple);
        executor.execute(ctask);
      }
    } catch(Exception e){
      e.printStackTrace();
    }
    executor.shutdown();
    while(true){
       if(executor.getPoolSize() == 0 && executor.getQueue().size() == 0){
           System.out.println(
             String.format("线程池中线程数目：%d，队列中等待执行的任务数目：%d，已执行完毕的任务数目：%d", 
               executor.getPoolSize(), 
               executor.getQueue().size(), 
               executor.getCompletedTaskCount()));
           System.exit(0);
       }
       try {
         // 查看线程池状态
         System.out.println(
             String.format("线程池中线程数目：%d，队列中等待执行的任务数目：%d，已执行完毕的任务数目：%d", 
                 executor.getPoolSize(), 
                 executor.getQueue().size(), 
                 executor.getCompletedTaskCount()));
         Thread.sleep(10000);
       } catch(Exception e){
         e.printStackTrace();
       }
    }
  }

  public static class CRunnable implements Runnable {
    private TaskEntity taskEntity;
    private File fileConfig;
    private SimpleURLConnection simpleConnection;
    public CRunnable(TaskEntity taskEntity, File fileConfig, SimpleURLConnection simpleConnection){
      this.taskEntity = taskEntity;
      this.fileConfig = fileConfig;
      this.simpleConnection = simpleConnection;
    }

    @Override
    public void run(){
      long begin = System.currentTimeMillis();
      while(!taskEntity.getFinished()){
        try{
          download(taskEntity, fileConfig);
          FileUtil.writeConfig(fileConfig, JSON.toJSONString(MulitTaskDownloader.task));
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

}
