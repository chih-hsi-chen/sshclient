package nctu.winlab.sshrest;

import java.util.logging.Logger;

public class DefaultServerClient extends SshExecClient implements ServerClient {
    private static Logger log = Logger.getLogger(DefaultServerClient.class.getName());

    public DefaultServerClient(String ip, String port, String username, String password, String model) {
        super(ip, port, username, password);
        this.model = model;
    }

    @Override
    public void execCommand(String cmd) {
        try {
            sendCmd(cmd);
            log.info(recvCmd());
        }
        catch (Exception e) {
            return;
        }
    }

    @Override
    public void execSudoCommand(String cmd) {
        try {
            sendSudoCmd(cmd, password);
            log.info(recvCmd());
        }
        catch (Exception e) {
            return;
        }
    }
}
