package pr.nik.modem;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class Main {
  private static final int SOCKET_PORT = 7777;
  private static int SOCKET_TIMEOUT = 30_000;
  private static CommonFileSingleton cf;
  private static List<String> listIp;
  private static List<String> listCommands;
  private static int THREADS_COUNT = 1;

  public static void main(String[] args) {
    cf = CommonFileSingleton.getInstance();

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

    String fileName = args[0];
    String fileNameCmd = fileName.substring(0, fileName.indexOf(".")) + "_cmd.txt";
    String successFile = cf.createNewFile(fileName, "Success");
    String errorIp = cf.createNewFile(fileName, "errorIp");

    ExecutorService executor = Executors.newFixedThreadPool(THREADS_COUNT);
    readFiles(fileName, fileNameCmd);
    listIp.forEach(ip -> {
        String logFile = cf.createNewFile(fileName, "log_" + ip);
        SimpleSocket simpleSocket = new SimpleSocket(ip, SOCKET_PORT, SOCKET_TIMEOUT, logFile, successFile, errorIp);
        simpleSocket.setCommandsList(listCommands);
        executor.submit(simpleSocket);
      });
    executor.shutdown();
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
