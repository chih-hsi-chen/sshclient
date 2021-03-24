package nctu.winlab.sshrest;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.io.BufferedReader;

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

    protected SshClient(String ip, String port, String username, String password) {
        this.ip = ip;
        this.port = port;
        this.username = username;
        this.password = password;
    }
}
