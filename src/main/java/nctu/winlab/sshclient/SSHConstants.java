package nctu.winlab.sshclient;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SSHConstants {
    public static String ANSI_RESET = "\u001B[0m";
    public static String ANSI_RED = "\u001B[31m";
    public static String ANSI_GREEN = "\u001B[32m";
    public static String ANSI_BOLD = "\033[1m";
    public static ObjectMapper mapper = new ObjectMapper();
}
