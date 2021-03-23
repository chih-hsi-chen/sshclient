package nctu.winlab.sshrest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.FileWriter;

public interface SwitchClient {
    public ObjectMapper mapper = new ObjectMapper();

    public ObjectNode getController();
    public ObjectNode setController(String ip, String port);
    public ObjectNode unsetController(String ip);
    public ObjectNode getFlows();
    public ObjectNode getGroups();
    public void getLogs(FileWriter writer);

    public static ObjectNode createGeneralReply() {
        ObjectNode reply = mapper.createObjectNode();
        reply.put("error", false);
        reply.put("msg", "");
        reply.put("raw", "");
        return reply;
    }
}
