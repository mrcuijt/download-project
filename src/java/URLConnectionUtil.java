import java.io.*;
import java.util.*;
import java.security.cert.*;
import java.net.*;
import javax.net.ssl.*;

public class URLConnectionUtil {

  public static void setProxy(){
    String settings = "proxy_settings.ini";
    String proxyHost = "127.0.0.1";
    String proxyPort = "1080";

    try{
      if(!new File(settings).exists()){
        StringBuilder strb = new StringBuilder();
        strb.append("proxyHost=");
        strb.append(proxyHost);
        strb.append("\r\n");
        strb.append("proxyPort=");
        strb.append(proxyPort);
        FileUtil.writeConfig(new File(settings), strb.toString());
      }
      BufferedReader br = new BufferedReader(new FileReader(settings));
      String line = br.readLine();
      proxyHost = line.substring(line.lastIndexOf("=") + 1);
      line = br.readLine();
      proxyPort = line.substring(line.lastIndexOf("=") + 1);
    }catch(Exception e){
      System.out.println(e.toString());
    }
    System.out.println(String.format("use proxy %s:%s",proxyHost ,proxyPort));
    System.setProperty("http.proxyHost", proxyHost);
    System.setProperty("http.proxyPort", proxyPort);

    // 对https也开启代理
    System.setProperty("https.proxyHost", proxyHost);
    System.setProperty("https.proxyPort", proxyPort);
  }

  public static void clearProxy(){
    System.clearProperty("http.proxyHost");
    System.clearProperty("https.proxyHost");
  }

  public static void setRequestHeader(HttpURLConnection conn, Map<String,List<String>> headers){
    if(conn == null) return;
    if(headers == null || headers.keySet().size() == 0) return;
    Set<String> keySet = headers.keySet();
    for(String header : keySet){
      List<String> values = headers.get(header);
      if(values == null || values.size() == 0) continue;
      conn.setRequestProperty(header, values.get(0));
      //if(values.size() == 1){
      //  conn.setRequestProperty(header, values.get(0));
      //} else {
      //  StringBuilder strb = new StringBuilder();
      //  for(String value : values){
      //    strb.append(value);
      //    strb.append(";");
      //  }
      //  conn.setRequestProperty(header, strb.toString());
      //}
    }
  }

  public static Map<String, List<String>> getCustomerHeader(String link){
    if(link.indexOf(" ") == -1) return null;
    Map<String, List<String>> headers = new HashMap<String, List<String>>();
    String params = link.substring(link.indexOf(" ") + 1);
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

  public static Map<String, List<String>> getHeaders(String link){
    String method = "GET";
    String url = (link.indexOf(" ") == -1) ? link : link.substring(0, link.indexOf(" "));
    Map<String, List<String>> customerHeaders = getCustomerHeader(link);
    HttpURLConnection conn = new SimpleURLConnection().getConnection(url, method);
    setRequestHeader(conn, customerHeaders);
    Map<String, List<String>> headers = getHeaders(conn);
    conn.disconnect();
    return headers;
  }

  public static Map<String, List<String>> getHeaders(HttpURLConnection conn){
    String[] acceptRanges = new String[]{"Accept-Ranges", "bytes"};
    Map<String, List<String>> headers = conn.getHeaderFields();
    List<String> accepts = headers.get(acceptRanges[0]);
    if(accepts != null && accepts.size() > 0 && accepts.get(0).equals(acceptRanges[1])){
      Map<String,List<String>> temp = new HashMap<String, List<String>>();
      Set<String> keySet = headers.keySet();
      for(String key : keySet){
        List<String> header = headers.get(key);
        temp.put(key, header);
      }
      List<String> list = new ArrayList<String>();
      list.add("true");
      temp.put(DownloaderSettings.SUPPORT, list);
      headers = temp;
    }
    return headers;
  }

  public static void readHeader(Map<String,List<String>> headers){
    if(headers == null || headers.keySet().size() ==0) return;
    Set<String> keySet = headers.keySet();
    for(String key : keySet){
      StringBuilder strb = new StringBuilder();
      List<String> header = headers.get(key);
      for(String temp : header){
        strb.append(temp);
      }
      System.out.println(String.format("%s : %s", key ,strb.toString()));
    }
  }

  public static long getContentLength(Map<String, List<String>> headers){
    long length = 0;
    String contentLength = "Content-Length";
    List<String> list = headers.get(contentLength);
    if(list != null && list.size() > 0){
      try{
        length = Long.valueOf(list.get(0));
       }catch(Exception e){
         e.printStackTrace();
      }
    }
    return length;
  }

  public static boolean isSupportRange(Map<String, List<String>> headers){
    return headers.containsKey(DownloaderSettings.SUPPORT);
  }

  public static SSLSocketFactory getSSLSocketFactory(){
    try {
      // 创建SSLContext
      SSLContext sslContext = SSLContext.getInstance("SSL");
      TrustManager[] tm = { new SSLVerify() };
      // 初始化
      sslContext.init(null, tm, new java.security.SecureRandom());
      // 获取 SSLSocketFactory 对象
      SSLSocketFactory ssf = sslContext.getSocketFactory();
      return ssf;
    }catch(Exception e){
      e.printStackTrace();
    }
    return null;
  }

  public static class SSLVerify implements X509TrustManager {

    public void checkClientTrusted(X509Certificate[] chain, String authType)
              throws CertificateException {
    }

    public void checkServerTrusted(X509Certificate[] chain, String authType)
              throws CertificateException {
    }

    public X509Certificate[] getAcceptedIssuers() {
      return null;
    }

  }

  public static TrustAnyHostnameVerifier getTrustAnyHostnameVerifier(){
    return new TrustAnyHostnameVerifier();
  }

  /**
   * 校验https网址是否安全
   *
   * @author solexit06
   *
   */
  public static class TrustAnyHostnameVerifier implements HostnameVerifier {
    public boolean verify(String hostname, SSLSession session) {
      // 直接返回true:默认所有https请求都是安全的
      return true;
    }
  }

}
