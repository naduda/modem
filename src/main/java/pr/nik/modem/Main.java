package pr.nik.modem;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
  public static final CommonFile cf = new CommonFile();
  private static List<String> listIp;
  private static List<String> listCommands;
  public static String successFile;
  public static String errorIp;
  private static int THREADS_COUNT = 1;
  public static int SOCKET_TIMEOUT = 30_000;
  private static int runningCount = 0;

  private SocketTools st;

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println(Help.TEXT);
      return;
    }
    
    if (args.length > 1) {
      THREADS_COUNT = Integer.parseInt(args[1]);
    }
    
    if (args.length > 2) {
      SOCKET_TIMEOUT = Integer.parseInt(args[2]) * 1000;
    }
    
    System.err.println(THREADS_COUNT + " === " + SOCKET_TIMEOUT + ": " + new Date());

    String fileName = args[0];
    String fileNameCmd = fileName.substring(0, fileName.indexOf(".")) + "_cmd.txt";
    successFile = cf.createNewFile(fileName, "Success");
    errorIp = cf.createNewFile(fileName, "errorIp");

    readFiles(fileName, fileNameCmd);
    listIp.stream()
      .map(f -> f.split("#")[0].trim())
      .filter(f -> f.length() > 0)
      .forEach(ip -> {
        changeRoundCount(1);
        new Thread(() -> {
          Main instance = new Main();
          String logFile = cf.createNewFile(fileName, "log_" + ip);
          instance.st = new SocketTools(logFile);
          instance.executeListOfCommands(ip, successFile, errorIp);
          instance.st.close();
          changeRoundCount(-1);
        }).start();

        while (runningCount == THREADS_COUNT) {
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            System.err.println(e.getMessage());
          }
        }
      });
    System.err.println(THREADS_COUNT + " === " + SOCKET_TIMEOUT + ": " + new Date());
  }
  
  private static synchronized void changeRoundCount(int value) {
    runningCount += value;
//    System.err.println(runningCount);
  }

  private static void readFiles(String fileIp, String fileCmd) {
    listIp = cf.getFileToListOfLines(fileIp);
    listCommands  = cf.getFileToListOfLines(fileCmd);
  }

  private void executeListOfCommands(String ip, String successFile, String errorIp) {
    List<String> cmdList = listCommands.stream()
        .map(f -> f.split("#")[0].trim())
        .filter(f -> f.length() > 0).collect(Collectors.toList());
    
    boolean success = true;
    for (int i = 0; i < cmdList.size(); i++) {
      String cmdString = cmdList.get(i);
      String cmd = cmdString.split("\\|")[0];
      Map<String, String> params = cf.getCmdParameters(cmdString);
      String response = st.executeCommand(cmd, ip, params);
      if (response.equals("-1")) {
        success = false;
        break;
      }
      if (response.equals("-2")) {
        break;
      }
    }
    
    if (success) {
      cf.writeToFile(successFile, ip);
    } else {
      cf.writeToFile(errorIp, ip);
    }
  }

}
