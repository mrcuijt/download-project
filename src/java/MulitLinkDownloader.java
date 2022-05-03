import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import com.alibaba.fastjson.JSON;

public class MulitLinkDownloader {

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

    SimpleURLConnection simple = new SimpleURLConnection();
    if(args == null || args.length == 0) return;
    if(args.length == 2){
      if(args[1].equals("proxy")){
        URLConnectionUtil.setProxy();
      }
    }
    try{
      BufferedReader br = new BufferedReader(new FileReader(args[0]));
      String link = "";
      while((link = br.readLine()) != null && link.trim().length() > 0){
        CRunnable ctask = new CRunnable(link, simple);
        executor.execute(ctask);
      }
    }catch(Exception e){
      e.printStackTrace();
    }

    executor.shutdown();
    while(true){
       if(executor.getPoolSize() == 0 && executor.getQueue().size() == 0)
       System.exit(0);
       try {
         Thread.sleep(10000);
       } catch(Exception e){
         e.printStackTrace();
       }
    }
  }

  public static class CRunnable implements Runnable {
    private String link;
    private boolean finished;
    private SimpleURLConnection simpleConnection;
    public CRunnable(String link, SimpleURLConnection simpleConnection){
      this.link = link;
      this.simpleConnection = simpleConnection;
    }

    @Override
    public void run(){
      long begin = System.currentTimeMillis();
      while(!finished){
        try{
          download(link);
          //FileUtil.writeConfig(fileConfig, JSON.toJSONString(MulitTaskDownloader.task));
        }catch(Exception e){
          e.printStackTrace();
        }
      }
      Downloader.timeConsuming(begin, System.currentTimeMillis());
    }

    public void download(String link){
      String url = (link.indexOf(" ") == -1) ? link : link.substring(0, link.indexOf(" "));
      System.out.println(String.format("Start %s", link));
      HttpURLConnection conn = null;
      InputStream is = null;
      FileOutputStream fos = null;
      RandomAccessFile randomFile = null;
      File file = new File(Downloader.getFileName(url));
      try{
        if(!file.exists()){
          file.createNewFile();
        }
        System.out.println(file.getAbsolutePath());
        randomFile = new RandomAccessFile(file, "rw");
        // 文件长度，字节数
        //randomFile.seek(taskEntity.getBegin() + taskEntity.getRead());
        //randomFile.seek(taskEntity.getRead());
        conn = simpleConnection.getConnection(url, DownloaderSettings.METHOD, DownloaderSettings.timeout);
        URLConnectionUtil.setRequestHeader(conn, URLConnectionUtil.getCustomerHeader(link));
        if(DownloaderSettings.readHeader){
          System.out.println("Request Header:");
          URLConnectionUtil.readHeader(conn.getRequestProperties());
          System.out.println();
        }
        long contentLength = URLConnectionUtil.getContentLength(conn.getHeaderFields());
        if(contentLength != 0 && file.length() >= contentLength) {
          finished = true;
          System.out.println("File Early Exists!");
          return;
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
          if(size > DownloaderSettings.unit){
            size = size % DownloaderSettings.unit;
          }
          // String message = String.format("Total Read : %d ; Content-Length : %d ; left : %d ;", (taskEntity.getBegin() + taskEntity.getRead()), taskEntity.getContentLength(), (taskEntity.getContentLength() - (taskEntity.getBegin() + taskEntity.getRead())));
          // Downloader.cleanConsoleLine(message.length());
          // System.out.print(message);
        }
        finished = true;
        System.out.println(String.format("%s finished", url));
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
