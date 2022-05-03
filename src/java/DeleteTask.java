import java.io.File;

public class DeleteTask {

  public static void main(String[] args){
    long begin = System.currentTimeMillis();
    if(args == null || args.length == 0) return;
    File fileConfig = new File(args[0]);
    Task task = Downloader.readTaskConfig(fileConfig);
    if(task.getTaskEntity() == null || task.getTaskEntity().size() == 0){
      System.out.println("There is no file to delete.");
      return;
    }
    for(TaskEntity taskEntity : task.getTaskEntity()){
      File file = new File(task.getFileName() + taskEntity.getTaskId());
      if(file.isFile() && file.exists()) file.delete();
    }
    Downloader.timeConsuming(begin, System.currentTimeMillis());
  }

}
