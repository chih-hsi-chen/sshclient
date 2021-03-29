package nctu.winlab.sshrest;

import com.jcraft.jsch.ChannelExec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import static nctu.winlab.sshrest.SSHConstants.ANSI_BOLD;
import static nctu.winlab.sshrest.SSHConstants.ANSI_RED;
import static nctu.winlab.sshrest.SSHConstants.ANSI_RESET;

public class SshExecClient extends SshClient {
    public SshExecClient(String ip, String port, String username, String password) {
        super(ip, port, username, password);
    }

    public void connectToServer() throws Exception {
        try {
            if (session == null || !session.isConnected()) {
                session = jsch.getSession(username, ip, Integer.parseInt(port));
                session.setPassword(password);
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect(3000);
            }
        }
        catch (Exception e) {
            System.err.printf(ANSI_RED + ANSI_BOLD + "\nFailed to connect to %s:%s\n" + ANSI_RESET, ip, port);
            throw e;
        }
    }

    public String sendCmd(String cmd) throws Exception {
        String ret = "";
        try {
            connectToServer();
            channel = session.openChannel("exec");
            ((ChannelExec)channel).setCommand(cmd);
            
            InputStream in = channel.getInputStream();
            // reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect();

            ret = recvOutput(in);
        }
        catch (Exception out) {
            ret = out.getMessage();
        }
        return ret;
    }
    
    public String sendSudoCmd(String cmd, String passwd) throws Exception {
        String ret = "";
        try {
            connectToServer();
            channel = session.openChannel("exec");
            ((ChannelExec)channel).setCommand("echo " + passwd + " | sudo -S " + cmd);
            
            InputStream in = channel.getInputStream();
            // reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect();

            ret = recvOutput(in);
        }
        catch (Exception out) {
            ret = out.getMessage();
        }
        return ret;
    }

    // public String recvCmd() {
    //     char[] buf = new char[1024];
    //     String reply = "";
    //     try {
    //         int nbytes;
    //         while ((nbytes = reader.read(buf, 0, 1024)) > -1) {
    //             reply = reply + String.valueOf(buf, 0, nbytes);
    //         }
    //     }
    //     catch (Exception e) {
    //         e.printStackTrace();
    //     }
    //     return reply;
    // }

    private String recvOutput(InputStream in) throws IOException {
        byte[] buffer = new byte[1024];
        StringBuilder strBuilder = new StringBuilder();

        while (true){
            while (in.available() > 0) {
                int i = in.read(buffer, 0, 1024);
                if (i < 0)
                    break;
                strBuilder.append(new String(buffer, 0, i));
            }

            if(channel.isClosed())
                break;
            try {
                Thread.sleep(100);
            } catch (Exception ex){
                // empty exception block
            }
        }
        return strBuilder.toString();
    }
}
