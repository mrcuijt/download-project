import java.util.*;
import java.net.HttpURLConnection;

public class ProxyURLConnection implements URLConnection {

  public SimpleURLConnection simpleURLConnection;

  public Map<String, List<String>> customerHeader;

  public ProxyURLConnection(SimpleURLConnection simpleURLConnection){
    this(simpleURLConnection, null);
  }

  public ProxyURLConnection(SimpleURLConnection simpleURLConnection,  Map<String, List<String>> headers){
    this.simpleURLConnection = simpleURLConnection;
    this.customerHeader = headers;
  }

  public HttpURLConnection getConnection(String url, String method, int timeout, String rangeBegin, String rangeEnd){
    HttpURLConnection conn = this.simpleURLConnection.getConnection(url, method, timeout, rangeBegin, rangeEnd);
    setRequestHeader(conn, this.customerHeader);
    return conn;
  }

  public void setRequestHeader(HttpURLConnection conn, Map<String,List<String>> headers){
    if(conn == null) return;
    if(headers == null || headers.keySet().size() == 0) return;
    Set<String> keySet = headers.keySet();
    for(String header : keySet){
      List<String> values = headers.get(header);
      if(values == null) continue;
      else if(values.size() == 1){
        conn.setRequestProperty(header, values.get(0));
      }else{
        StringBuilder strb = new StringBuilder();
        for(String value : values){
          strb.append(value);
          strb.append(";");
        }
        conn.setRequestProperty(header, strb.toString());
      }
    }
  }
}
