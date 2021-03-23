package nctu.winlab.sshrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.logging.Logger;

public class DGS3630Client extends SshShellClient implements SwitchClient {
    private static Logger log = Logger.getLogger(DGS3630Client.class.getName());

    public DGS3630Client(String ip, String port, String username, String password, String model) {
        super(ip, port, username, password);
        this.model = model;
    }

    @Override
    public ObjectNode getController() {
        ObjectNode res = SwitchClient.createGeneralReply();
        ArrayNode controllerList = res.putArray("controllers");
        String rawoutput = "";
        try {
            String[] reply = commander.addMainCmd("show openflow configuration", new String[0]).sendCmd().recvCmd().split("[\r\n]+");
            String[] controllers = Arrays.copyOfRange(reply, 11, reply.length - 1);
            rawoutput += String.format("\u001b[32m\u001b[1m\n%s -- %s\n\u001b[0m", ip, model);
            rawoutput += String.format("%-17s%-7s%-6s%s\n", "IP", "Port", "Mode", "Role");
            for (String controller : controllers) {
                String[] infos = controller.split("[ \t]+");
                ObjectNode c = mapper.createObjectNode();
                rawoutput += String.format("%-17s%-7s%-6s%s\n", infos[0], infos[1], infos[2], infos[3]);
                c.put("ip", infos[0]);
                c.put("port", infos[1]);
                c.put("mode", infos[2]);
                c.put("role", infos[3]);
                controllerList.add((JsonNode) c);
            }
            res.put("raw", rawoutput);
        }
        catch (Exception e) {
            // empty catch block
        }
        return res;
    }

    @Override
    public ObjectNode setController(String ip, String port) {
        ObjectNode res = SwitchClient.createGeneralReply();
        try {
            port = port.isEmpty() ? port : " service-port " + port;
            String reply = commander.addCmd("configure terminal").addMainCmd("openflow controller " + ip + port, new String[0]).addCmd("exit").sendCmd().recvCmd();
            res.put("raw", reply);
        }
        catch (Exception e) {
            res.put("error", true);
            res.put("msg", e.getMessage());
        }
        return res;
    }

    @Override
    public ObjectNode unsetController(String ip) {
        ObjectNode res = SwitchClient.createGeneralReply();
        try {
            String reply = commander.addCmd("configure terminal").addMainCmd("no openflow controller " + ip, new String[0]).addCmd("exit").sendCmd().recvCmd();
            res.put("raw", reply);
        }
        catch (Exception e) {
            res.put("error", true);
            res.put("msg", e.getMessage());
        }
        return res;
    }

    @Override
    public ObjectNode getFlows() {
        ObjectNode res = SwitchClient.createGeneralReply();
        ArrayNode flowList = res.putArray("flows");
        try {
            String reply = commander.addMainCmd("show openflow flows", "a").sendCmd().recvCmd();
            res.put("raw", reply);
        }
        catch (Exception e) {
            res.put("error", true);
            res.put("msg", e.getMessage());
        }
        return res;
    }

    @Override
    public ObjectNode getGroups() {
        ObjectNode res = SwitchClient.createGeneralReply();
        try {
            String reply = commander.addMainCmd("show openflow group-desc", "a").sendCmd().recvCmd();
            res.put("raw", reply);
        }
        catch (Exception e) {
            res.put("error", true);
            res.put("msg", e.getMessage());
        }
        return res;
    }

    @Override
    public void getLogs(FileWriter writer) {
        try {
            String reply = commander.addMainCmd("show logging", "q").sendCmd().recvCmd();
            String title = String.format("\u001b[32m\u001b[1m\u001b[1m\n%s -- %s\n\u001b[0m", ip, model);
            if (writer == null) {
                log.info(title);
                log.info(reply);
            } else {
                writer.write(title, 0, title.length());
                writer.write(reply, 0, reply.length());
            }
        }
        catch (Exception e) {
            return;
        }
    }
}
