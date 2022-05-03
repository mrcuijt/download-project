import java.io.*;

public class DownloaderSettings {

  public static final String SUPPORT = "support";
  public static final String METHOD = "GET";

  public static int corePoolSize = 3;
  public static int maximumPoolSize = 3;
  public static int keepAliveTime = 2;
  public static int blockingQueue = 350;

  // 1MB
  public static int blockSize = 1 * 1024 * 1024;
  // 10MB
  public static int splitSize = 10 * 1024 * 1024;
  // 1KB
  public static int unit = 1024;
  // 5Minute
  public static int timeout = 1000 * 60 * 5;
  // Read Header
  public static boolean readHeader = true;
  public static int bbreakcount = 1;
  public static boolean bbreak = false;

  public static void init(){
    String blockSize = Integer.toString(DownloaderSettings.blockSize);
    String splitSize = Integer.toString(DownloaderSettings.splitSize);
    String timeout = Integer.toString(DownloaderSettings.timeout);
    String readHeader = Boolean.toString(DownloaderSettings.readHeader);
    String bbreakcount = Integer.toString(DownloaderSettings.bbreakcount);
    String bbreak = Boolean.toString(DownloaderSettings.bbreak);

    String corePoolSize = Integer.toString(DownloaderSettings.corePoolSize);
    String maximumPoolSize = Integer.toString(DownloaderSettings.maximumPoolSize);
    String keepAliveTime = Integer.toString(DownloaderSettings.keepAliveTime);
    String blockingQueue = Integer.toString(DownloaderSettings.blockingQueue);

    String settings = "download_settings.ini";
    BufferedReader br = null;
    try{
      if(!(new File(settings).exists())){
        StringBuffer strb = new StringBuffer();
        strb.append("blockSize=");
        strb.append(DownloaderSettings.blockSize);
        strb.append("\r\n");
        strb.append("splitSize=");
        strb.append(DownloaderSettings.splitSize);
        strb.append("\r\n");
        strb.append("timeout=");
        strb.append(DownloaderSettings.timeout / 1000 / 60);
        strb.append("\r\n");
        strb.append("readHeader=");
        strb.append(readHeader);
        strb.append("\r\n");
        strb.append("breakcount=");
        strb.append(bbreakcount);
        strb.append("\r\n");
        strb.append("break=");
        strb.append(bbreak);

        strb.append("\r\n");
        strb.append("corePoolSize=");
        strb.append(corePoolSize);
        strb.append("\r\n");
        strb.append("maximumPoolSize=");
        strb.append(maximumPoolSize);
        strb.append("\r\n");
        strb.append("keepAliveTime=");
        strb.append(keepAliveTime);
        strb.append("\r\n");
        strb.append("blockingQueue=");
        strb.append(blockingQueue);

        FileUtil.writeConfig(new File(settings), strb.toString());
      }

      br = new BufferedReader(new FileReader(settings));
      String line = br.readLine();
      blockSize = line.substring(line.lastIndexOf("=") + 1);
      line = br.readLine();
      splitSize = line.substring(line.lastIndexOf("=") + 1);
      line = br.readLine();
      timeout = line.substring(line.lastIndexOf("=") + 1);
      line = br.readLine();
      readHeader = line.substring(line.lastIndexOf("=") + 1);
      line = br.readLine();
      bbreakcount = line.substring(line.lastIndexOf("=") + 1);
      line = br.readLine();
      bbreak = line.substring(line.lastIndexOf("=") + 1);

      DownloaderSettings.blockSize = Integer.valueOf(blockSize);
      DownloaderSettings.splitSize = Integer.valueOf(splitSize);
      DownloaderSettings.timeout = Integer.valueOf(timeout) * 1000 * 60;
      DownloaderSettings.readHeader = Boolean.valueOf(readHeader);
      DownloaderSettings.bbreakcount = Integer.valueOf(bbreakcount);
      DownloaderSettings.bbreak = Boolean.valueOf(bbreak);

      line = br.readLine();
      corePoolSize = line.substring(line.lastIndexOf("=") + 1);
      line = br.readLine();
      maximumPoolSize = line.substring(line.lastIndexOf("=") + 1);
      line = br.readLine();
      keepAliveTime = line.substring(line.lastIndexOf("=") + 1);
      line = br.readLine();
      blockingQueue = line.substring(line.lastIndexOf("=") + 1);

      DownloaderSettings.corePoolSize = Integer.valueOf(corePoolSize);
      DownloaderSettings.maximumPoolSize = Integer.valueOf(maximumPoolSize);
      DownloaderSettings.keepAliveTime = Integer.valueOf(keepAliveTime);
      DownloaderSettings.blockingQueue = Integer.valueOf(blockingQueue);

      System.out.println(String.format("Download Settings: BlockSize : %s ; SplitSize : %s ; Timeout : %s ; ReadHeader : %s ; BreakCount : %s ; Break : %s ;", blockSize, splitSize, timeout, readHeader, bbreakcount, bbreak));
      System.out.println(String.format("ThreadPoolExecutor Settings: corePoolSize : %s ; maximumPoolSize : %s ; keepAliveTime : %s ; blockingQueue : %s ;", corePoolSize, maximumPoolSize, keepAliveTime, blockingQueue));
    }catch(Exception e){
      e.printStackTrace();
    }finally{
      try{
        if(br != null) br.close();
      }catch(Exception e){
        e.printStackTrace();
      }
    }
  }

  public static void main(String[] args){
    init();
  }

}
