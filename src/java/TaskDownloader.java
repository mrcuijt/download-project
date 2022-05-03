import java.io.File;
import java.util.List;
import com.alibaba.fastjson.JSON;

public class TaskDownloader extends Downloader {


  public TaskDownloader(){
    //DownloaderSettings.init();
    //simpleConnection = new SimpleURLConnection();
  }

  public static void main(String[] args){
    long begin = System.currentTimeMillis();
    Downloader downloader = new TaskDownloader();
    if(args == null || args.length == 0) return;
    if(args.length == 2){
      if(args[1].equals("proxy")){
        URLConnectionUtil.setProxy();
      }
    }
    File fileConfig = new File(args[0]);
    task = readTaskConfig(fileConfig);
    run(task, fileConfig);
    timeConsuming(begin, System.currentTimeMillis());
  }

  public static void run(Task task, File fileConfig){
    List<TaskEntity> taskList = task.getTaskEntity();
    if(taskList.size() == 0){
      download(task.getUrl(), task.getContentLength());
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
  }

}
