import java.io.*;
import java.util.*;
import com.alibaba.fastjson.JSON;

public class InitFileConfig {

  public static void main(String[] args){
    long begin = System.currentTimeMillis();
    if(args == null || args.length == 0) return;
    if(args.length == 2){
      if(args[1].equals("proxy")){
        URLConnectionUtil.setProxy();
      }
    }
    run(args[0]);
    Downloader.timeConsuming(begin, System.currentTimeMillis());
  }

  public static void run(String filePath){
    try{
      BufferedReader br = new BufferedReader(new FileReader(filePath));
      String link = "";
      while((link = br.readLine()) != null && link.trim().length() > 0){
        //Map<String, List<String>> headers = getCustomerHeader(url);
        //URLConnectionUtil.readHeader(headers);
        generateConfigFile(link);
      }
    }catch(Exception e){
      e.printStackTrace();
    }
  }

  public static Map<String, List<String>> getCustomerHeader(String url){
    if(url.indexOf(" ") == -1) return null;
    Map<String, List<String>> headers = new HashMap<String, List<String>>();
    String params = url.substring(url.indexOf(" ") + 1);
    if(params.indexOf("=") == -1) return null;
    String[] temps = params.split("&");
    try {
      for(String temp : temps){
        if(temp.indexOf("=") == -1) continue;
        String key = temp.substring(0, temp.indexOf("="));
        String value = temp.substring(temp.indexOf("=") + 1);
        List<String> valueList = new ArrayList<String>();
        valueList.add(value);
        headers.put(key, valueList);
      }
    } catch(Exception e){
      e.printStackTrace();
    }
    return headers;
  }

  public static Task generateConfigFile(String link){
    String url = (link.indexOf(" ") == -1) ? link : link.substring(0, link.indexOf(" "));
    long contentLength = URLConnectionUtil.getContentLength(URLConnectionUtil.getHeaders(url));
    File fileConfig = new File(MD5Util.md5sum(url));
    Task task = new Downloader().tasks(link, contentLength);
    task.setFileName(Downloader.getFileName(url));
    FileUtil.writeConfig(fileConfig, JSON.toJSONString(task));
    return task;
  }

}
