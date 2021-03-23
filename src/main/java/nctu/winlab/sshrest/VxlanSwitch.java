package nctu.winlab.sshrest;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface VxlanSwitch {
    public ObjectNode setVxlanSourceInterfaceLoopback(String loopbackId);
    public ObjectNode setVxlanVlan(String vnid, String vid);
    public ObjectNode setVxlanVtep(String vnid, String ip, String mac);
    public ObjectNode setVxlanStatus(boolean flag);
    public ObjectNode showVxlan();
}
