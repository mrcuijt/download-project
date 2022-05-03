import java.io.File;

public class MergeMulitTaskDownloader {

  public static void main(String[] args){
    long begin = System.currentTimeMillis();
    if(args == null || args.length == 0) return;
    File fileConfig = new File(args[0]);
    Task task = Downloader.readTaskConfig(fileConfig);
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
    Downloader.timeConsuming(begin, System.currentTimeMillis());
  }

}
