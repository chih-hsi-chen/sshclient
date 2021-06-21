package nctu.winlab.sshclient;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.FileWriter;

public interface SwitchClient {
    public ObjectNode getController();
    public ObjectNode setController(String ip, String port);
    public ObjectNode unsetController(String ip);
    public ObjectNode getFlows();
    public ObjectNode getGroups();
    public void getLogs(FileWriter writer);
}
