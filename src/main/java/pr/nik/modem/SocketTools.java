package pr.nik.modem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

public class SocketTools {
  public Socket socket;
  public PrintWriter out;
  public BufferedReader in;
  private String logFile;
  
  public SocketTools(String logFile) {
    this.logFile = logFile;
  }
  
  public boolean init(String ip, String logFile) {
    if (socket == null || socket.isClosed() || !socket.isConnected()) {
      try {
        socket = new Socket(ip, 7777);
        socket.setSoTimeout(Main.SOCKET_TIMEOUT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      } catch (IOException e) {
        return false;
      }
      if (socket == null || socket.isClosed() || !socket.isConnected()) {
        if (socket != null) {
          try {
            socket.close();
            Main.cf.writeToFile(logFile, "Socket closed!");
          } catch (IOException e) {
            Main.cf.writeToFile(logFile, e.getMessage());
          }
        }
      }
      if (socket != null && socket.isConnected()) {
        Main.cf.writeToFile(logFile, String.format("Connected to %s!", ip));
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
          Main.cf.writeToFile(logFile, "Socket closed!");
        }
        socket.close();
      } catch (IOException e) {
        Main.cf.writeToFile(logFile, e.getMessage());
      }
    }
    
    if (out != null) {
      out.close();
    }
    
    if (in != null) {
      try {
        in.close();
      } catch (IOException e) {
        Main.cf.writeToFile(logFile, e.getMessage());
      }
    }
  }
  
  public boolean waitForConnect(String ip, long timeout, String logFile) {
    if (timeout == 0) {
      timeout = 60_000;
    }
    long start = System.currentTimeMillis();
    while (socket == null || socket.isClosed() || !socket.isConnected()) {
      if (System.currentTimeMillis() - start > timeout) {
        Main.cf.writeToFile(logFile, "Didn't connect........................" + ip);
        break;
      }
      if (init(ip, logFile)) {
        break;
      }
    }
    return socket != null && !socket.isClosed() && socket.isConnected();
  }

  public String executeCommand(String cmd, String ip, Map<String, String> params) {
    String success = "-1";
    long timeout = params.containsKey("timeout") ? Long.parseLong(params.get("timeout")) * 1000 : 0;

    if (timeout == 0) {
      if (!waitForConnect(ip, Main.SOCKET_TIMEOUT * 3, logFile)) {
        return success;
      }
    }

    out.println(cmd);
    Main.cf.writeToFile(logFile, "=> " + cmd);
    int dataBuffer;
    StringBuilder respBuilder = new StringBuilder();
    try {
      while ((dataBuffer = in.read()) != -1) {
        if (dataBuffer == 10) break;
        respBuilder.append((char)dataBuffer);
      }
      Main.cf.writeToFile(logFile, respBuilder.toString());
      success = respBuilder.toString();
      if (params.containsKey("contains")) {
        if (success.toLowerCase().contains(params.get("contains").toString())) {
          success = "0";
        }
      }
    } catch (IOException e) {
      Main.cf.writeToFile(logFile, e.getMessage());
    }
    if (params.containsKey("reconnect")) {
      close();
      success = "0";
    }

    if (params.containsKey("wait")) {
      close();
      try {
        Thread.sleep(Long.parseLong(params.get("wait").toString()) * 1000);
      } catch (InterruptedException e) {
        Main.cf.writeToFile(logFile, e.getMessage());
      }
      success = "0";
    }

    if (success.equals("-1") && params.containsKey("timeout")) {
      close();
      success = waitForConnect(ip, timeout, logFile) ? "0" : "-1";
    } else if (params.containsKey("exitifcontains")) {
      boolean isSuccess = true;
      String[] conditions = params.get("exitifcontains").toString().split("&&");
      for (String condition : conditions) {
        if (!isSuccess) break;
        isSuccess = success.toLowerCase().contains(condition.trim());
      }
      if (isSuccess) {
        return "-2";
      }
    }

    return success;
  }
}
