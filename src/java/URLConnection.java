import java.net.HttpURLConnection;

public interface URLConnection {
  HttpURLConnection getConnection(String url, String method, int timeout, String rangeBegin, String rangeEnd);
}
