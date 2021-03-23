/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  com.jcraft.jsch.Channel
 *  com.jcraft.jsch.JSch
 *  com.jcraft.jsch.Session
 */
package nctu.winlab.sshrest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;

public class SshShellClient extends SshClient {
    private PrintWriter writer;
    protected final Commander commander = new Commander();

    public SshShellClient(String ip, String port, String username, String password) {
        super(ip, port, username, password);
    }

    public void connectToServer() throws Exception {
        try {
            if (session == null || !session.isConnected()) {
                session = jsch.getSession(username, ip, Integer.parseInt(port));
                session.setPassword(password);
                session.setConfig("StrictHostKeyChecking", "no");
                session.connect(3000);
                channel = session.openChannel("shell");
            }
            if (!channel.isConnected()) {
                writer = new PrintWriter(channel.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
                channel.connect(3000);
            }
        }
        catch (Exception e) {
            System.err.printf("\u001b[31m\u001b[1m\nFailed to connect to %s:%s\n\u001b[0m", ip, port);
            throw e;
        }
    }

    protected class Commander {
        private ArrayList<String> cmds = new ArrayList<String>();
        private String mainCmd;
        private static final String CMD_END_MARK = "# CMD_END #";
        private static final int RECV_BUF_SIZE = 1024;

        protected Commander() {
        }

        public Commander addCmd(String ... cmd) {
            for (String c : cmd) {
                cmds.add(c + "\n");
            }
            return this;
        }

        public Commander addMainCmd(String cmd, String ... ctrls) {
            cmds.add(cmd + "\n");
            for (String ctrl : ctrls) {
                cmds.add(ctrl);
            }
            cmds.add("# CMD_END #\n");
            mainCmd = cmd;
            return this;
        }

        public Commander sendCmd() throws Exception {
            connectToServer();
            for (String cmd : cmds) {
                writer.print(cmd);
                writer.flush();
            }
            cmds.clear();
            return this;
        }

        public String recvCmd() {
            int nbytes;
            char[] buf = new char[RECV_BUF_SIZE];
            String reply = "";

            try {
                while ((nbytes = reader.read(buf, 0, RECV_BUF_SIZE)) > -1) {
                    reply += String.valueOf(buf, 0, nbytes);
                    if (reply.contains(CMD_END_MARK)) {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            reply = reply.substring(reply.indexOf(mainCmd) + mainCmd.length());
            reply = reply.substring(reply.indexOf("\n") + 1);
            reply = reply.substring(0, reply.lastIndexOf(CMD_END_MARK));
            reply = reply.substring(0, reply.lastIndexOf("\n")+ 1);
            return reply;
        }
    }

}
