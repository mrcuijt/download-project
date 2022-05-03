import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import com.alibaba.fastjson.JSON;

public class UpdateFileConfig {

  public static void main(String[] args){
    if(args == null || args.length == 0) return;
    File file = new File(args[0]);
    if(!file.exists()) return;
    run(file);
  }

  public static void run(File file){
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(file));
      String line = "";
      while((line = br.readLine()) != null){
        if(line.indexOf(" ") == -1) continue;
        String fileName = line.substring(0, line.indexOf(" "));
        File fileConfig = new File(fileName);
        if(!fileConfig.exists()) continue;
        updateFileConfig(fileConfig, line.substring(line.indexOf(" ") + 1));
      }
    } catch(Exception e) {
      e.printStackTrace();
    } finally {
      try {
        if(br != null) br.close();
      } catch (Exception e){
        e.printStackTrace();
      }
    }
  }

  public static void updateFileConfig(File fileConfig, String link) {
    Task task = Downloader.readTaskConfig(fileConfig);
    task.setCustomerHeaders(URLConnectionUtil.getCustomerHeader(link));
    if(link.indexOf(" ") != -1) {
      String url = link.substring(0, link.indexOf(" "));
      task.setUrl(url);
      for(TaskEntity taskEntity : task.getTaskEntity()){
        taskEntity.setUrl(url);
      }
    }
    FileUtil.writeConfig(fileConfig, JSON.toJSONString(task));
  }
}
