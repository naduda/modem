package pr.nik.modem;

public class Help {
  public static final String TEXT = "You should have two files in the same directory:\n" + 
      "\t1. xxx.txt\t\t- contains list of ip-addresses.\n" +
      "\t2. xxx_cmd.txt\t- contains list of commands.\n" +
      "where xxx - any text.\n\n\n" +
      "Run: java -jar [path]/modem.jar [path]/xxx.txt [THREADS_COUNT(default = 1)] [SOCKET_TIMEOUT(default = 30 sec)]\n" +
      "===================================================================================================================\n";
}
