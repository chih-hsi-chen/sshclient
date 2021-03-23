package nctu.winlab.sshrest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.FileWriter;

public interface SwitchClient {
    public ObjectNode getController();
    public void setController(String ip, String port);
    public void unsetController(String ip);
    public ObjectNode getFlows();
    public ObjectNode getGroups();
    public void getLogs(FileWriter writer);
    public void setVxlanSourceInterfaceLoopback(String loopbackId);
    public void setVxlanVlan(String vnid, String vid);
    public void setVxlanVtep(String vnid, String ip, String mac);
}
