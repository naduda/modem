package pr.nik.modem;

import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import pr.nik.modem.settings.Configuration;

public class Main {
  private static final int SOCKET_PORT = 7777;
  private static CommonFileSingleton cf;
  private static List<String> listIp;
  private static List<String> listCommands;
  private static int cycleCounter;
  private static Configuration cfg;

  public static void main(String[] args) {
    cf = CommonFileSingleton.getInstance();

    if (args.length < 1) {
      String textFile = cf.getFileText("cfg.json");
      if (textFile.length() < 1) {
        return;
      }
      cfg = JSON.parseObject(textFile, Configuration.class);
    } else {
      if (args[0].toLowerCase().equals("make")) {
        cfg = new Configuration();
        make();
      } else if (args[0].toLowerCase().equals("help")) {
        System.out.println(Help.TEXT);
      }
      return;
    }

    for(int i = 0; i < cfg.getIpFiles().size(); i++) {
      String fileName = cfg.getIpFiles().get(i);
      File file = new File(fileName);
      if (!file.exists()) {
        System.out.println("File " + file.getAbsolutePath() + " not exist!");
        return;
      }
      String fileNameCmd = fileName.substring(0, fileName.lastIndexOf(".")) + "_cmd.txt";
      String successFile = cf.createNewFile(fileName, "Success");
      String errorFile = cf.createNewFile(fileName, "errorIp");

      readFiles(fileName, fileNameCmd);
      try {
        cf.cleanFolder(fileName.substring(0, fileName.lastIndexOf("/") + 1) + "logs");
      } catch(Exception e) {
        System.err.println(e.getMessage());
        return;
      }
      cycleCounter = 0;
      while (cycleCounter++ < cfg.getCycles() && listIp.size() > 0) {
        ExecutorService executor = Executors.newFixedThreadPool(cfg.getThreadCount());
        cf.writeToFile(successFile, "CYCLES = " + cycleCounter);
        if (cfg.getSocketTimeout() < 60_000) {
          cfg.setSocketTimeout(cfg.getSocketTimeout() + 10_000);
        }
        listIp.forEach(ip -> {
            SimpleSocket simpleSocket = new SimpleSocket(ip, SOCKET_PORT, cfg.getSocketTimeout(), successFile, errorFile);
            simpleSocket.setFileNames(fileName, "log_" + ip + "_" + cycleCounter);
            simpleSocket.setCommandsList(listCommands);
            executor.submit(simpleSocket);
          });

        executor.shutdown();
        try {
          executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
          System.err.println(e.getMessage());
        }

        if (cycleCounter == cfg.getCycles()) {
          break;
        }
        listIp = cf.getFileToListOfLines(errorFile).stream()
            .map(f -> f.split("#")[0].trim())
            .filter(f -> f.length() > 0)
            .collect(Collectors.toList());
        cf.createNewFile(fileName, "errorIp");
      }
    }
  }

  private static void make() {
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

      setIntValue(br, "threadCount");
      setIntValue(br, "cycles");
      setIntValue(br, "socketTimeout");

      System.out.println(String.format("Input files count:"));
      int filesCount = setIntValue(br);
      for (int i = 0; i < filesCount; i++) {
        String value = "";
        System.out.println(String.format("Input file path %d:", i + 1));
        while (value.length() == 0) {
          value = br.readLine().trim();
          if (value.length() > 0) {
            File ipFile = new File(value);
            if (!ipFile.exists()) {
              System.err.println("File not exist: " + ipFile.getAbsolutePath());
              value = "";
            }
          }
        }
        cfg.addIpFiles(value);
      }
    } catch(Exception e) {
      e.printStackTrace();
    }

    String jsonString = JSON.toJSONString(cfg, true);
    File f = new File("./cfg.json");
    if (f.exists()) {
      f.delete();
    }
    try {
      f.createNewFile();
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
    cf.writeToFile("cfg.json", jsonString);
  }
  
  private static void setIntValue(BufferedReader br, String fieldName) {
    System.out.println(String.format("Input %s:", fieldName));
    try {
      String value = "";
      while (value.length() == 0) {
        value = br.readLine().trim();
        try {
          int v = Integer.parseInt(value);
          switch (fieldName.toLowerCase()) {
            case "threadcount": cfg.setThreadCount(v); break;
            case "cycles": cfg.setCycles(v); break;
            case "sockettimeout": cfg.setSocketTimeout(v * 1000); break;
          }
        } catch (Exception e) {
          System.err.println("It's not a number: " + value);
          value = "";
        }
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
  }
  
  private static int setIntValue(BufferedReader br) {
    try {
      String value = "";
      while (value.length() == 0) {
        value = br.readLine().trim();
        try {
          return Integer.parseInt(value);
        } catch (Exception e) {
          System.err.println("It's not a number: " + value);
          value = "";
        }
      }
    } catch (Exception e) {
      return setIntValue(br);
    }
    return 0;
  }

  private static void readFiles(String fileIp, String fileCmd) {
    listIp = cf.getFileToListOfLines(fileIp).stream()
        .map(f -> f.split("#")[0].trim())
        .filter(f -> f.length() > 0)
        .collect(Collectors.toList());
    listCommands  = cf.getFileToListOfLines(fileCmd).stream()
        .map(f -> f.split("#")[0].trim())
        .filter(f -> f.length() > 0)
        .collect(Collectors.toList());
  }

}
