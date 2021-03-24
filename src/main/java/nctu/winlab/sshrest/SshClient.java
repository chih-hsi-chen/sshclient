package nctu.winlab.sshrest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.io.BufferedReader;

import static nctu.winlab.sshrest.SSHConstants.mapper;

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

    protected ObjectMapper mapper() {
        return mapper;
    }

    protected ObjectNode createGeneralReply() {
        ObjectNode reply = mapper.createObjectNode();
        reply.put("error", false);
        reply.put("msg", "");
        reply.put("raw", "");
        return reply;
    }
}
