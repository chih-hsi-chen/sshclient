package nctu.winlab.sshrest;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.io.BufferedReader;
import java.util.Formatter;

public abstract class SshClient {
    public String ip;
    public String port;
    public String username;
    public String password;
    public String model;
    protected Session session;
    protected Channel channel;
    protected BufferedReader reader;
    protected final JSch jsch = new JSch();
    protected static final int TIMEOUT = 3000;
    protected static final int RECV_BUF_SIZE = 1024;
    protected static final String ANSI_RESET = "\u001b[0m";
    protected static final String ANSI_GREEN = "\u001b[32m";
    protected static final String ANSI_RED = "\u001b[31m";
    protected static final String ANSI_BOLD = "\u001b[1m";

    protected SshClient(String ip, String port, String username, String password) {
        this.ip = ip;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    protected String formatString(String format, Object ... args) {
        StringBuffer buffer = new StringBuffer();
        Formatter formatter = new Formatter(buffer);
        String output = formatter.format(format, args).toString();
        formatter.close();
        return output;
    }
}
