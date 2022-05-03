import java.util.Map;
import java.util.List;

public class Task {

  private String url;

  private String fileName;

  private long contentLength;

  private Map<String, List<String>> customerHeaders;

  private List<TaskEntity> taskEntity;

  public String getUrl(){
    return this.url;
  }

  public void setUrl(String url){
    this.url = url;
  }

  public String getFileName(){
    return this.fileName;
  }

  public void setFileName(String fileName){
    this.fileName = fileName;
  }

  public long getContentLength(){
    return contentLength;
  }

  public void setContentLength(long contentLength){
    this.contentLength = contentLength;
  }

  public Map<String, List<String>> getCustomerHeaders(){
    return customerHeaders;
  }

  public void setCustomerHeaders(Map<String, List<String>> customerHeaders){
    this.customerHeaders = customerHeaders;
  }

  public List<TaskEntity> getTaskEntity(){
    return taskEntity;
  }

  public void setTaskEntity(List<TaskEntity> taskEntity){
    this.taskEntity = taskEntity;
  }

}

