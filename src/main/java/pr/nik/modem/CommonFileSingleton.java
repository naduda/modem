package pr.nik.modem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommonFileSingleton {
  private static volatile CommonFileSingleton instance;

  private CommonFileSingleton() {}

  public static CommonFileSingleton getInstance()
  {
    if (instance == null)
    {
      synchronized(CommonFileSingleton.class) {
        if (instance == null) instance = new CommonFileSingleton();
      }
    }
    return instance;
  }

  public synchronized String createNewFile(String fileName, String name) {
    String filePath = "";
    if (name.startsWith("log")) {
      String parrent = new File(fileName).getParent();
      String dirLogsPath = parrent + (parrent.endsWith(File.separator) ? "" : File.separator)+ "logs";
      File dirLogs = new File(dirLogsPath);
      if (!dirLogs.exists()) {
        dirLogs.mkdir();
      }
      filePath = dirLogsPath + File.separator + name + ".txt";
    } else {
      filePath = fileName.substring(0, fileName.indexOf(".")) + "_" + name + ".txt";
    }

    File f = new File(filePath);
    if (f.exists()) {
      f.delete();
    }
    try {
      f.createNewFile();
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
    return filePath;
  }
  
  public synchronized List<String> getFileToListOfLines(String fileName) {
    try (Stream<String> stream = Files.lines(Paths.get(fileName))) {
      return stream.collect(Collectors.toList());
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public synchronized void writeToFile(String fileName, String log) {
    DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");
    if (!fileName.toLowerCase().contains("error")) {
      log = df.format(new Date(System.currentTimeMillis())) + ": " + log;
    }
    System.out.println(log);
    try(FileWriter fw = new FileWriter(fileName, true);
        BufferedWriter bw = new BufferedWriter(fw);
        PrintWriter out = new PrintWriter(bw))
    {
        out.println(log);
    } catch (IOException e) {
        System.err.println(e.getMessage());
    }
  }

  public synchronized Map<String, String> getCmdParameters(String cmdString) {
    Map<String, String> params = new HashMap<>();
    params = new HashMap<>();
    if (cmdString.split("\\|").length > 1) {
      String[] pars = cmdString.split("\\|")[1].split(";");
      for (String p : pars) {
        if (p.indexOf("=") < 0) {
          continue;
        }
        params.put(p.split("=")[0].toLowerCase(), p.split("=")[1].toLowerCase());
      }
    }
    return params;
  }
}
