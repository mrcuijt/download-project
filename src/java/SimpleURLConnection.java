import java.io.*;
import java.util.*;
import java.security.*;
import java.security.cert.*;
import java.net.*;
import javax.net.ssl.*;

public class SimpleURLConnection implements URLConnection  {

  private static String method = "GET";

  public HttpURLConnection getConnection(String url){
    return getConnection(url, method);
  }

  public HttpURLConnection getConnection(String url, String method){
    return getConnection(url, method, DownloaderSettings.timeout);
  }

  public HttpURLConnection getConnection(String url, int timeout){
    return getConnection(url, method, timeout);
  }

  public HttpURLConnection getConnection(String url, String method, int timeout){
    return getConnection(url, method, timeout, null, null);
  }

  public HttpURLConnection getConnection(String url, String method, int timeout, String rangeBegin, String rangeEnd){
    try {
      // 打开连接
      HttpURLConnection conn = null;
      if(url.startsWith("https")){
        conn = (HttpsURLConnection) new URL(url).openConnection();
        ((HttpsURLConnection)conn).setHostnameVerifier(URLConnectionUtil.getTrustAnyHostnameVerifier());
        ((HttpsURLConnection)conn).setSSLSocketFactory(URLConnectionUtil.getSSLSocketFactory());
      } else conn = (HttpURLConnection) new URL(url).openConnection();
      // 设置一些参数
      conn.setDoOutput(true);
      conn.setDoInput(true);
      conn.setUseCaches(false);
      conn.setReadTimeout(timeout);
      conn.setRequestMethod(method);
      // 设置 HTTP 请求头
      conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:45.0) Gecko/20100101 Firefox/45.0");
      if(rangeBegin != null && rangeEnd != null) conn.setRequestProperty("Range", "bytes=" + rangeBegin + "-" + rangeEnd);
      return conn;
    }catch(Exception e){
      e.printStackTrace();
    }
    return null;
  }

}
