import java.io.*;
import java.nio.channels.FileChannel;

public class FileUtil {

  public static void writeConfig(File file, String config){
    FileOutputStream fos = null;
    try{
        if(file.exists()) backup(file);
        fos = new FileOutputStream(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bytes = config.getBytes("UTF-8");
        baos.write(bytes, 0, bytes.length);
        baos.writeTo(fos);
        baos.flush();
    }catch(Exception e){
      e.printStackTrace();
    }finally{
      try{
        if(fos != null) fos.close();
      }catch(Exception e){
        e.printStackTrace();
      }
    }
  }

  public static void backup(File file) {
    FileInputStream fis = null;
    FileOutputStream fos = null;
    try{
        fis = new FileInputStream(file);
        fos = new FileOutputStream(new File(file.getName() + ".bak"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while((length = fis.read(buffer, 0, buffer.length)) != -1){
          baos.write(buffer, 0, length);
        }
        fos.write(baos.toByteArray());
        fos.flush();
    }catch(Exception e){
      e.printStackTrace();
    }finally{
      try{
        if(fos != null) fos.close();
        if(fis != null) fis.close();
      }catch(Exception e){
        e.printStackTrace();
      }
    }
  }

  public static void mergeFiles(File[] files, File target){
    if(files == null || files.length == 0) return;
    if(files.length == 1) files[0].renameTo(target);
    try{
      FileChannel fileChannel = new FileOutputStream(target, true).getChannel();
      for(File file : files){
        FileChannel channel = new FileInputStream(file).getChannel();
        fileChannel.transferFrom(channel, fileChannel.size(), channel.size());
        channel.close();
        System.out.println(String.format("%s merge to %s finished", file.getName(), target.getName()));
      }
      fileChannel.close();
      System.out.println("Merge File Finished");
      System.out.println(target.getAbsolutePath());
    } catch(Exception e){
      e.printStackTrace();
    }
  }

  public static void splitFile(File file, long blockSize){
    if(file == null || !file.exists()) return;
    long size = file.length();
    if(size < blockSize) return;
    // 比较总内容大小
    int totalBlock = (int)(size / blockSize);
    long left = 0;
    if(size % blockSize > 0){
      left = size % blockSize;
      totalBlock += 1;
    }
    long[][] tasks = new long[totalBlock][2];
    long begin = 0;
    long end = 0;
    for(int i = 0; i < tasks.length; i++){
      if(i == 0){
        tasks[i][0] = begin;
        tasks[i][1] = blockSize;
        begin += blockSize;
        end = tasks[i][1] + blockSize;
        continue;
      }
      tasks[i][0] = begin;
      tasks[i][1] = end;
      begin += blockSize;
      end = tasks[i][1] + blockSize;
    }
    if(left > 0){
      tasks[tasks.length - 1][1] = tasks[tasks.length - 1][0] + left;
    }
    try {
          FileChannel fileChannel = new FileInputStream(file).getChannel();
      for(int i = 0; i < tasks.length; i++){
        try{
          File target = new File(file.getName() + i);
          FileChannel channel = new FileOutputStream(target).getChannel();
          fileChannel.transferTo(tasks[i][0], tasks[i][1] - tasks[i][0], channel);
          channel.close();
          System.out.println(String.format("%d split to %d finished", tasks[i][0], tasks[i][1]));
          System.out.println(String.format("%s split to %s finished", file.getName(), target.getName()));
        } catch(Exception e){
          e.printStackTrace();
        }
      }
          fileChannel.close();
      System.out.println("Split File Finished");
    } catch(Exception e){
      e.printStackTrace();
    }
  }

  public static long fileLength(String filePath){
    if(filePath == null) return 0;
    File file = new File(filePath);
    if(!file.exists() || !file.isFile()) return 0;
    return file.length();
  }

}
