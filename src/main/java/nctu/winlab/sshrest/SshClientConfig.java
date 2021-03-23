package nctu.winlab.sshrest;

import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;

public class SshClientConfig extends Config<ApplicationId> {
    private static final String CLIENT_INFOS = "clientInfos";
    public HashMap<String, SshClient> clients;
    public HashMap<Integer, String> idToname;
    private int[] maxWidth = new int[]{0, 0, 0, 0, 0};
    private static final String NAME = "name";
    private static final String IP = "ip";
    private static final String PORT = "port";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String MODEL = "model";

    public boolean isValid() {
        return hasField(CLIENT_INFOS);
    }
    public JsonNode clientInfo() {
        return this.node.get(CLIENT_INFOS);
    }

    public void parseConfig() {
        clients = new HashMap<>();
        idToname = new HashMap<>();
        int index = 0;
        for (JsonNode jsonNode : node.get(CLIENT_INFOS)) {
            String name = jsonNode.path(NAME).asText("device-" + index);
            String ip = jsonNode.path(IP).asText();
            String port = jsonNode.path(PORT).asText("22");
            String username = jsonNode.path(USERNAME).asText();
            String password = jsonNode.path(PASSWORD).asText();
            String model = jsonNode.path(MODEL).asText();
            maxWidth[0] = maxWidth[0] > name.length() ? maxWidth[0] : name.length();
            maxWidth[1] = maxWidth[1] > ip.length() ? maxWidth[1] : ip.length();
            maxWidth[2] = maxWidth[2] > port.length() ? maxWidth[2] : port.length();
            maxWidth[3] = maxWidth[3] > username.length() ? maxWidth[3] : username.length();
            maxWidth[4] = maxWidth[4] > model.length() ? maxWidth[4] : model.length();
            SshClient client = null;
            switch (SshClientManager.DeviceModel.valueOf(model)) {
                case DXS_5000: {
                    client = new DXS5000Client(ip, port, username, password, model);
                    break;
                }
                case DGS_3630: {
                    client = new DGS3630Client(ip, port, username, password, model);
                    break;
                }
                case DIR_835: {
                    client = new Dir835Client(ip, port, username, password, model);
                    break;
                }
                case SERVER: {
                    client = new DefaultServerClient(ip, port, username, password, model);
                    break;
                }
            }
            if (client == null) continue;
            clients.put(name, client);
            idToname.put(index++, name);
        }
    }

    public HashMap<String, SshClient> getClients() {
        return clients;
    }

    public HashMap<Integer, String> getIdMap() {
        return idToname;
    }

    public int[] getMaxWidth() {
        return maxWidth;
    }
}
