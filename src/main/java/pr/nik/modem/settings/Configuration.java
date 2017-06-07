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

  public int getThreadCount() {
    return threadCount;
  }

  public void setThreadCount(int threadCount) {
    this.threadCount = threadCount;
  }

  @Override
  public String toString() {
    return String.format("threadCount = %d; cycles = %d; socketTimeout = %d; ipFiles.size() = %d",
        threadCount, cycles, socketTimeout, ipFiles.size());
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
    this.ipFiles.add(path);
  }
}
