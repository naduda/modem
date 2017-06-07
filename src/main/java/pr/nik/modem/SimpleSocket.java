package pr.nik.modem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SimpleSocket implements Runnable {
  private static CommonFileSingleton cf;
  private Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  private String fileName;
  private String logFileName;
  private String logFile;
  private String successFile;
  private String errorFile;
  private String ipAddress;
  private int port;
  private int socketTimeout;
  private List<String> listCommands;

  public SimpleSocket(String ipAddress, int port, int timeout, String successFile, String errorFile) {
    this.errorFile = errorFile;
    this.successFile = successFile;
    this.ipAddress = ipAddress;
    this.port = port;
    this.socketTimeout = timeout;
    
    cf = CommonFileSingleton.getInstance();
  }

  public boolean init() {
    if (socket == null || socket.isClosed() || !socket.isConnected()) {
      try {
        socket = new Socket(ipAddress, port);
        socket.setSoTimeout(socketTimeout);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      } catch (IOException e) {
        return false;
      }
      if (socket == null || socket.isClosed() || !socket.isConnected()) {
        if (socket != null) {
          try {
            socket.close();
            cf.writeToFile(logFile, "Socket closed!");
          } catch (IOException e) {
            cf.writeToFile(logFile, e.getMessage());
          }
        }
      }
      if (socket != null && socket.isConnected()) {
        cf.writeToFile(logFile, String.format("Connected to %s!", ipAddress));
        return true;
      } else {
        return false;
      }
    }
    return true;
  }

  public void close() {
    if (socket != null) {
      try {
        if (!socket.isClosed()) {
          cf.writeToFile(logFile, "Socket closed!");
        }
        socket.close();
      } catch (IOException e) {
        cf.writeToFile(logFile, e.getMessage());
      }
    }

    if (out != null) {
      out.close();
    }

    if (in != null) {
      try {
        in.close();
      } catch (IOException e) {
        cf.writeToFile(logFile, e.getMessage());
      }
    }
  }

  public boolean waitForConnect(long timeout) {
    if (timeout == 0) {
      timeout = 60_000;
    }
    long start = System.currentTimeMillis();
    while (socket == null || socket.isClosed() || !socket.isConnected()) {
      if (System.currentTimeMillis() - start > timeout) {
        cf.writeToFile(logFile, "Didn't connect........................" + ipAddress);
        break;
      }
      init();
    }
    return socket != null && !socket.isClosed() && socket.isConnected();
  }

  public CommandResult executeCommand(String cmd, Map<String, String> params) {
    CommandResult result = CommandResult.ERROR;
    String response = "";
    long timeout = params.containsKey("timeout") ? Long.parseLong(params.get("timeout")) * 1000 : 0;

    if (timeout == 0) {
      if (!waitForConnect(socketTimeout * 3)) {
        return CommandResult.NOT_CONNECT;
      }
    }

    out.println(cmd);
    cf.writeToFile(logFile, "=> " + cmd);
    int dataBuffer;
    StringBuilder respBuilder = new StringBuilder();
    try {
      while ((dataBuffer = in.read()) != -1) {
        if (dataBuffer == 10) break;
        respBuilder.append((char)dataBuffer);
      }
      response = respBuilder.toString();
      cf.writeToFile(logFile, response);
    } catch (IOException e) {
      cf.writeToFile(logFile, e.getMessage());
    }

    if (params.containsKey("reconnect")) {
      close();
      result = CommandResult.SUCCESS;
    }

    if (params.containsKey("wait")) {
      close();
      try {
        TimeUnit.SECONDS.sleep(Long.parseLong(params.get("wait").toString()));
      } catch (InterruptedException e) {
        cf.writeToFile(logFile, e.getMessage());
      }
      result = CommandResult.SUCCESS;
    }

    if (params.containsKey("contains")) {
      if (response.toLowerCase().contains(params.get("contains").toString())) {
        result = CommandResult.SUCCESS;
      }
    }

    if (result == CommandResult.ERROR && params.containsKey("timeout")) {
      close();
      result = waitForConnect(timeout) ? CommandResult.SUCCESS : CommandResult.ERROR;
    } else if (params.containsKey("exitifcontains")) {
      boolean isSuccess = true;
      String[] conditions = params.get("exitifcontains").toString().split("&&");
      for (String condition : conditions) {
        if (!isSuccess) break;
        isSuccess = response.toLowerCase().contains(condition.trim());
      }
      if (isSuccess) {
        result = CommandResult.SUCCESS_AND_BREAK;
      }
    }

    return result;
  }

  public void setCommandsList(List<String> list) {
    this.listCommands = list;
  }
  
  public void setFileNames(String fileName, String logFileName) {
    this.fileName = fileName;
    this.logFileName = logFileName;
  }

  @Override
  public void run() {
    logFile = cf.createNewFile(fileName, logFileName);
    boolean success = true;
    for (int i = 0; i < listCommands.size(); i++) {
      String cmdString = listCommands.get(i);
      String cmd = cmdString.split("\\|")[0];
      Map<String, String> params = cf.getCmdParameters(cmdString);
      CommandResult response = executeCommand(cmd, params);
      success = response == CommandResult.SUCCESS || response == CommandResult.SUCCESS_AND_BREAK;

      if (response == CommandResult.SUCCESS_AND_BREAK || response == CommandResult.NOT_CONNECT) {
        break;
      }
    }

    cf.writeToFile(success ? successFile : errorFile, ipAddress);
    close();
  }
}
