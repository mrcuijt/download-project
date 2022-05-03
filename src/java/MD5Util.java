import java.security.*;

public class MD5Util{

 /**
  * @param param MD5 加密的字符串
  * @return String MD5Hash 值
  * @throws
  * @Description: 加密指定的字符串并返回加密后的 MD5Hash 值（全小写）
  * @author CuiJiutao
  * @date 2018/4/26 15:08
  */
  public static String md5sum(String param) {
    try {
      MessageDigest alg = MessageDigest.getInstance("MD5");
      alg.update(param.getBytes());
      byte[] digest = alg.digest();
      StringBuffer localStringBuffer = new StringBuffer();
      String str = "";
      for (int i = 0; i < digest.length; ++i) {
        str = Integer.toHexString(digest[i] & 0xFF);
        if (str.length() == 1)
          localStringBuffer.append("0");
        localStringBuffer.append(str);
      }
      return localStringBuffer.toString();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
    }
    return null;
  }
}
