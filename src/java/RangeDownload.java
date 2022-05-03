import java.io.*;
import java.util.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class RangeDownload {

  private static SimpleURLConnection simple = new SimpleURLConnection();

  public static void main(String[] args){
    String dictory = "" + System.currentTimeMillis();
    String filePath = "Noname13.txt";
    try{
      if(args != null && args.length == 1 && args[0].equals("proxy"))
        URLConnectionUtil.setProxy();
      download(filePath,dictory);
    }catch(Exception e){
      e.printStackTrace();
    }
  }

  public static void download(String filePath, String dictory){
    try {
      BufferedReader br = new BufferedReader(new FileReader(filePath));
      String link = "";
      while((link = br.readLine()) != null && link.trim().length() > 0){
        try {
          reDownloadAppend(link);
        } catch (Exception e){
          reLoad(link);
        }
      }
    } catch (Exception e){
      e.printStackTrace();
    }
  }

  public static boolean fin = false;
  public static void reDownloadAppend(String link) throws Exception{
    String url = (link.indexOf(" ") == -1) ? link : link.substring(0, link.indexOf(" "));
    //Map<String, List<String>> headers = URLConnectionUtil.getCustomerHeader(url);
    Map<String, List<String>> headers = new HashMap<String, List<String>>();
    List<String> referheader = new ArrayList<String>();
    String params = (link.indexOf("") == -1) ? "" : link.substring(link.indexOf(" ") + 1);
    referheader.add(params);
    headers.put("Referer", referheader);
    List<String> rangeheader = new ArrayList<String>();
    rangeheader.add("bytes=" + getRange(url) + "-");
    headers.put("Range", rangeheader);
    URLConnectionUtil.readHeader(headers);
    HttpURLConnection conn = simple.getConnection(url, "GET", 1000 * 60 * 10);
    URLConnectionUtil.setRequestHeader(conn, headers);
    InputStream is = conn.getInputStream();
    File file = new File(getFileName(url));
    RandomAccessFile randomFile = null;  
    if(file.exists()){
      try{
        randomFile = new RandomAccessFile(file, "rw");
        // 文件长度，字节数
        randomFile.seek(randomFile.length());
        byte[] buffer = new byte[1024];
        long total = randomFile.length();
        int len = 0;
        while((len = is.read(buffer,0,buffer.length)) != -1){
          total += len;
          randomFile.write(buffer, 0, len);
          System.out.println("total read : " + total);
        }
        fin = true;
        randomFile.close();
      }catch(Exception e){
        System.out.println(e.toString());
        throw e;
      }finally{
        if(is != null){
          is.close();
        }
      }
    } else {
      
    }
  }

  public static void reLoad(String link){
    while(!fin){
      try{
        reDownloadAppend(link);
      }catch(Exception e){
        e.printStackTrace();
        //System.out.println(e.toString());
      }
    }
  }

  public static String getFileName(String url){
    String fileName = MD5Util.md5sum(url) + ".mp4";
    return fileName;
    //String fileName = url.substring(url.lastIndexOf("/") + 1);
    //return fileName.indexOf("?") > 0 ? fileName.substring(0, fileName.lastIndexOf("?")) : fileName;
  }

  public static long getRange(String url){
    File file = new File(getFileName(url));
    if(!file.exists()){
      try{
        file.createNewFile();
      }catch(IOException e){
        System.out.println(e.toString());
      }
    }
    return file.length();
  }
}
