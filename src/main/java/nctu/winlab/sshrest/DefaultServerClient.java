package nctu.winlab.sshrest;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class DefaultServerClient extends SshExecClient implements ServerClient {

    public DefaultServerClient(String ip, String port, String username, String password, String model) {
        super(ip, port, username, password);
        this.model = model;
    }

    @Override
    public ObjectNode execCommand(String cmd) {
        ObjectNode res = mapper().createObjectNode();
        try {
            res.put("raw", sendCmd(cmd));
        }
        catch (Exception e) {
            res.put("error", true);
            res.put("msg", e.getMessage());
        }
        return res;
    }

    @Override
    public ObjectNode execSudoCommand(String cmd) {
        ObjectNode res = mapper().createObjectNode();
        try {
            res.put("raw", sendSudoCmd(cmd, password));
        }
        catch (Exception e) {
            res.put("error", true);
            res.put("msg", e.getMessage());
        }
        return res;
    }
}
