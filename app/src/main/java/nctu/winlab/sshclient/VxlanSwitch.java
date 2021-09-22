package nctu.winlab.sshclient;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface VxlanSwitch {
    public ObjectNode setVxlanSourceInterfaceLoopback(String loopbackId);
    public ObjectNode setVxlanVlan(String vnid, String vid);
    public ObjectNode setVxlanVtep(String vnid, String ip, String mac);
    public ObjectNode setVxlanStatus(boolean flag);
    public ObjectNode setVxlanTenantSystemLocal(String vni, String mac, String port, boolean add);
    public ObjectNode setVxlanTenantSystemRemote(String vni, String mac, String remoteIp, boolean add);
    public ObjectNode showVxlanTenantSystemLocal();
    public ObjectNode showVxlanTenantSystemRemove();
    public ObjectNode showVxlan();
    public ObjectNode testConnection();
}
