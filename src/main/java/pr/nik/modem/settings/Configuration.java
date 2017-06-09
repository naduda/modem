package pr.nik.modem.settings;

import java.util.ArrayList;
import java.util.List;

public class Configuration {
  private int threadCount;
  private int cycles;
  private int socketTimeout;
  private List<String> ipFiles;

  public Configuration() {
    ipFiles = new ArrayList<>();
    threadCount = 1;
    cycles = 1;
    socketTimeout = 30_000;
  }

  @Override
  public String toString() {
    return String.format("threadCount = %d; cycles = %d; socketTimeout = %d; ipFiles.size() = %d",
        threadCount, cycles, socketTimeout, ipFiles.size());
  }

  public String toJSONString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n\t\"cycles\":");
    sb.append(cycles);
    sb.append(",\n\t\"ipFiles\":[\n");
    for(int i = 0; i < ipFiles.size(); i++) {
      sb.append("\t\t\"");
      sb.append(ipFiles.get(i));
      sb.append("\"");
      if (i < ipFiles.size() - 1) {
        sb.append(",");
      }
      sb.append("\n");
    }
    sb.append("\t],\n\t\"socketTimeout\":");
    sb.append(socketTimeout);
    sb.append(",\n\t\"threadCount\":");
    sb.append(threadCount);
    sb.append("\n}");
    return sb.toString();
  }

  public void parseObject(String value) {
    value = value.replaceAll("\n", "");
    while (value.indexOf(" ") > 0) {
      value = value.replaceAll(" ", "");
    }
    while (value.indexOf("\t") > 0) {
      value = value.replaceAll("\t", "");
    }
    value = value.substring(1, value.length() - 1);
    String[] arr = value.split(",");
    for (String v : arr) {
      if (v.startsWith("\"cycles")) {
        setCycles(Integer.parseInt(v.substring(v.indexOf(":") + 1)));
      } else if (v.startsWith("\"ipFiles")) {
        addIpFiles(v.substring(v.indexOf("[") + 2, v.length() - 1));
      } else if (v.indexOf(":") < 0) {
        addIpFiles(v.contains("]") ? v.substring(1, v.length() - 2) : v.substring(1, v.length() - 1));
      } else if (v.contains("\"socketTimeout")) {
        setSocketTimeout(Integer.parseInt(v.substring(v.indexOf(":") + 1)));
      } else if (v.contains("\"threadCount")) {
        setThreadCount(Integer.parseInt(v.substring(v.indexOf(":") + 1)));
      }
    }
  }

  public int getThreadCount() {
    return threadCount;
  }

  public void setThreadCount(int threadCount) {
    this.threadCount = threadCount;
  }

  public int getCycles() {
    return cycles;
  }

  public void setCycles(int cycles) {
    this.cycles = cycles;
  }

  public int getSocketTimeout() {
    return socketTimeout;
  }

  public void setSocketTimeout(int socketTimeout) {
    this.socketTimeout = socketTimeout;
  }

  public List<String> getIpFiles() {
    return ipFiles;
  }

  public void addIpFiles(String path) {
    if (path.endsWith("\"")) {
      path = path.substring(0, path.length() - 1);
    }
    this.ipFiles.add(path);
  }
}
