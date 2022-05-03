import java.io.File;

public class SplitDownloaderFile {

  public static void main(String[] args){
    long begin = System.currentTimeMillis();
    if(args == null || args.length == 0) return;
    DownloaderSettings.init();
    File fileConfig = new File(args[0]);
    Task task = Downloader.readTaskConfig(fileConfig);
    FileUtil.splitFile(new File(task.getFileName()), DownloaderSettings.splitSize);
    Downloader.timeConsuming(begin, System.currentTimeMillis());
  }

}
