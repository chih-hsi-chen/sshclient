package nctu.winlab.sshrest;

import com.fasterxml.jackson.databind.JsonNode;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;

public class SshClientConfig extends Config<ApplicationId> {
    private static final String CLIENT_INFOS = "clientInfos";

    public boolean isValid() {
        return hasField(CLIENT_INFOS);
    }
    public JsonNode clientInfo() {
        return this.node.get(CLIENT_INFOS);
    }
}
