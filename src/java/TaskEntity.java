public class TaskEntity {
  private int taskId;
  private String url;
  private long begin;
  private long end;
  private long read;
  private long contentLength;
  private boolean finished;

  public int getTaskId(){
    return taskId;
  }

  public void setTaskId(int taskId){
    this.taskId = taskId;
  }

  public String getUrl(){
    return url;
  }

  public void setUrl(String url){
    this.url = url;
  }

  public long getBegin(){
    return begin;
  }

  public void setBegin(long begin){
    this.begin = begin;
  }

  public long getEnd(){
    return end;
  }

  public void setEnd(long end){
    this.end = end;
  }

  public long getRead(){
    return read;
  }

  public void setRead(long read){
    this.read = read;
  }

  public long getContentLength(){
    return contentLength;
  }

  public void setContentLength(long contentLength){
    this.contentLength = contentLength;
  }

  public boolean getFinished(){
    return finished;
  }

  public void setFinished(boolean finished){
    this.finished = finished;
  }

}
