package nctu.winlab.sshrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;

/**
 * Implementation of SSH client service
 */
@Component(immediate=true, service={SshClientService.class})
public class SshClientManager implements SshClientService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    // private SshClientPrinter printer = new SshClientPrinter();
    private final SshClientConfigListener cfgListener = new SshClientConfigListener();
    private final Set<ConfigFactory<ApplicationId, SshClientConfig>> factories = ImmutableSet.of(new ConfigFactory<ApplicationId, SshClientConfig>(
        APP_SUBJECT_FACTORY, SshClientConfig.class, "SshClientConfig") {
            public SshClientConfig createConfig() {
                return new SshClientConfig();
            }
        }
    );
    private ClientConfig clientConfig;
    private ApplicationId appId;
    @Reference(cardinality=ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry cfgService;
    @Reference(cardinality=ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("nctu.winlab.sshrest");
        cfgService.addListener(cfgListener);
        factories.forEach((cfgService)::registerConfigFactory);
        clientConfig = new ClientConfig();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.removeListener(cfgListener);
        factories.forEach((cfgService)::unregisterConfigFactory);
        log.info("Stopped");
    }

    @Override
    public void printDevices() {
        int INTERVAL = 2;
        int[] width = clientConfig.getMaxWidth();
        String fmt = "";

        for (int i = 0; i < width.length; i++) {
            fmt.concat("%-" + String.valueOf(width[i] + INTERVAL) + "s");
        }
        fmt += "\n";
        
        log.info(String.format("Index" + fmt, "Name", "IP", "Port", "Username", "Model"));
        log.info("-".repeat(7 + Arrays.stream(width).sum() + INTERVAL * 4));
        ArrayList<SshClient> sshclients = new ArrayList<SshClient>(clientConfig.clients.values());
        for (int i = 0; i < sshclients.size(); ++i) {
            SshClient client = sshclients.get(i);
            log.info(String.format("%-7d" + fmt, i, client.ip, client.port, client.username, client.model));
        }
        for (Integer index : clientConfig.idToname.keySet()) {
            String name = clientConfig.idToname.get(index);
            SshClient client = clientConfig.clients.get(name);
            log.info("%-7d" + fmt, index, name, client.ip, client.port, client.username, client.model);
        }
    }

    @Override
    public ArrayNode getDevices() {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode node = mapper.createArrayNode();
        int index = 0;
        for (String name : clientConfig.clients.keySet()) {
            SshClient client = clientConfig.clients.get(name);
            ObjectNode device = mapper.createObjectNode();
            device.put("index", index++);
            device.put("name", name);
            device.put("ip", client.ip);
            device.put("port", client.port);
            device.put("username", client.username);
            device.put("password", client.password);
            device.put("model", client.model);
            node.add((JsonNode) device);
        }
        return node;
    }

    @Override
    public ObjectNode getController(String deviceID) {
        String deviceName = convert2name(deviceID);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode reply = mapper.createObjectNode();
        reply.put("error", false);
        reply.put("msg", "");
        ArrayNode devices = reply.putArray("devices");
        if (deviceName.equals("ALL")) {
            for (String cname : clientConfig.clients.keySet()) {
                SshClient client = clientConfig.clients.get(cname);
                if (!(client instanceof SwitchClient)) continue;
                ObjectNode info = ((SwitchClient) client).getController();
                info.put("name", cname);
                devices.add((JsonNode)info);
            }
        } else if (isSwitchClient(deviceName)) {
            ObjectNode info = ((SwitchClient) clientConfig.clients.get(deviceName)).getController();
            info.put("name", deviceName);
            devices.add((JsonNode)info);
        } else {
            log.info("Remote machine should be switch");
            reply.put("error", true);
            reply.put("msg", "Remote machine should be switch");
        }
        return reply;
    }

    @Override
    public void setController(String deviceID, String ip, String port) {
        String deviceName = convert2name(deviceID);
        if (deviceName.equals("ALL")) {
            clientConfig.clients.values().forEach(c -> {
                if (c instanceof SwitchClient) {
                    ((SwitchClient) c).setController(ip, port);
                }
            });
        } else if (isSwitchClient(deviceName)) {
            ((SwitchClient)clientConfig.clients.get(deviceName)).setController(ip, port);
        } else {
            log.info("Remote machine should be switch");
        }
    }

    @Override
    public void unsetController(String deviceID, String ip) {
        String deviceName = convert2name(deviceID);
        if (deviceName.equals("ALL")) {
            clientConfig.clients.values().forEach(c -> {
                if (c instanceof SwitchClient) {
                    ((SwitchClient) c).unsetController(ip);
                }
            });
        } else if (isSwitchClient(deviceName)) {
            ((SwitchClient)clientConfig.clients.get(deviceName)).unsetController(ip);
        } else {
            log.info("Remote machine should be switch");
        }
    }

    @Override
    public ObjectNode getFlows(String deviceID) {
        String deviceName = convert2name(deviceID);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode reply = mapper.createObjectNode();
        reply.put("error", false);
        reply.put("msg", "");
        ArrayNode devices = reply.putArray("devices");
        if (deviceName.equals("ALL")) {
            for (String cname : clientConfig.clients.keySet()) {
                SshClient client = clientConfig.clients.get(cname);
                if (!(client instanceof SwitchClient)) continue;
                ObjectNode info = ((SwitchClient) client).getFlows();
                info.put("name", cname);
                devices.add((JsonNode)info);
            }
        } else if (isSwitchClient(deviceName)) {
            ObjectNode info = ((SwitchClient)clientConfig.clients.get(deviceName)).getFlows();
            info.put("name", deviceName);
            devices.add((JsonNode)info);
        } else {
            log.info("Remote machine should be switch");
            reply.put("error", true);
            reply.put("msg", "Remote machine should be switch");
        }
        return reply;
    }

    @Override
    public ObjectNode getGroups(String deviceID) {
        String deviceName = convert2name(deviceID);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode reply = mapper.createObjectNode();
        reply.put("error", false);
        reply.put("msg", "");
        ArrayNode devices = reply.putArray("devices");
        if (deviceName.equals("ALL")) {
            for (String cname : clientConfig.clients.keySet()) {
                SshClient client = clientConfig.clients.get(cname);
                if (!(client instanceof SwitchClient)) continue;
                ObjectNode info = ((SwitchClient) client).getGroups();
                info.put("name", cname);
                devices.add((JsonNode)info);
            }
        } else if (isSwitchClient(deviceName)) {
            ObjectNode info = ((SwitchClient) clientConfig.clients.get(deviceName)).getGroups();
            info.put("name", deviceName);
            devices.add((JsonNode)info);
        } else {
            log.info("Remote machine should be switch");
            reply.put("error", true);
            reply.put("msg", "Remote machine should be switch");
        }
        return reply;
    }

    @Override
    public void getLogs(String deviceID, String filename) {
        String deviceName = convert2name(deviceID);
        try {
            try (FileWriter writer = filename != null ? new FileWriter(filename) : null){
                if (deviceName.equals("ALL")) {
                    clientConfig.clients.values().forEach(c -> {
                        if (c instanceof SwitchClient) {
                            ((SwitchClient)((Object)c)).getLogs(writer);
                        }
                    });
                } else if (isSwitchClient(deviceName)) {
                    ((SwitchClient)clientConfig.clients.get(deviceName)).getLogs(writer);
                } else {
                    log.info("Remote machine should be switch");
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void execCommand(String deviceID, String cmd) {
        String deviceName = convert2name(deviceID);
        if (deviceName.equals("ALL")) {
            clientConfig.clients.values().forEach(c -> {
                if (c instanceof ServerClient) {
                    ((ServerClient) c).execCommand(cmd);
                }
            });
        } else if (isServerClient(deviceName)) {
            ((ServerClient)clientConfig.clients.get(deviceName)).execCommand(cmd);
        } else {
            log.info("Remote machine should be server");
        }
    }

    @Override
    public void execSudoCommand(String deviceID, String cmd) {
        String deviceName = convert2name(deviceID);
        if (isServerClient(deviceName)) {
            ((ServerClient)clientConfig.clients.get(deviceName)).execSudoCommand(cmd);
        } else {
            log.info("Remote machine should be server");
        }
    }

    @Override
    public void setSsid(String deviceID, String ifname, String ssid) {
        String deviceName = convert2name(deviceID);
        if (deviceName.equals("ALL")) {
            clientConfig.clients.values().forEach(c -> {
                if (c instanceof ApClient) {
                    ((ApClient) c).setSsid(ifname, ssid);
                }
            });
        } else if (isApClient(deviceName)) {
            ((ApClient)clientConfig.clients.get(deviceName)).setSsid(ifname, ssid);
        } else {
            log.info("Remote machine should be AP");
        }
    }

    @Override
    public void setVxlanSourceInterfaceLoopback(String deviceID, String loopbackId) {
        String deviceName = convert2name(deviceID);
        if (deviceName.equals("ALL")) {
            clientConfig.clients.values().forEach(c -> {
                if (c instanceof DXS5000Client) {
                    ((DXS5000Client) c).setVxlanSourceInterfaceLoopback(loopbackId);
                }
            });
        } else if (isDxs5000Client(deviceName)) {
            ((DXS5000Client) clientConfig.clients.get(deviceName)).setVxlanSourceInterfaceLoopback(loopbackId);
        } else {
            log.info("Remote machine should be DXS-5000");
        }
    }

    @Override
    public void setVxlanVlan(String deviceID, String vnid, String vid) {
        String deviceName = convert2name(deviceID);
        if (deviceName.equals("ALL")) {
            clientConfig.clients.values().forEach(c -> {
                if (c instanceof DXS5000Client) {
                    ((DXS5000Client) c).setVxlanVlan(vnid, vid);
                }
            });
        } else if (isDxs5000Client(deviceName)) {
            ((DXS5000Client)clientConfig.clients.get(deviceName)).setVxlanVlan(vnid, vid);
        } else {
            log.info("Remote machine should be DXS-5000");
        }
    }

    @Override
    public void setVxlanVtep(String deviceID, String vnid, String ip, String mac) {
        String deviceName = convert2name(deviceID);
        if (deviceName.equals("ALL")) {
            clientConfig.clients.values().forEach(c -> {
                if (c instanceof DXS5000Client) {
                    ((DXS5000Client)c).setVxlanVtep(vnid, ip, mac);
                }
            });
        } else if (isDxs5000Client(deviceName)) {
            ((DXS5000Client)clientConfig.clients.get(deviceName)).setVxlanVtep(vnid, ip, mac);
        } else {
            log.info("Remote machine should be DXS-5000");
        }
    }

    private String convert2name(String deviceID) {
        try {
            Integer index = Integer.parseInt(deviceID);
            return clientConfig.idToname.get(index);
        } catch (NumberFormatException e) {
            // Input is a string
            return deviceID;
        }
    }

    private boolean isSwitchClient(String deviceName) {
        return clientConfig.clients.get(deviceName) instanceof SwitchClient;
    }

    private boolean isServerClient(String deviceName) {
        return clientConfig.clients.get(deviceName) instanceof ServerClient;
    }

    private boolean isApClient(String deviceName) {
        return clientConfig.clients.get(deviceName) instanceof ApClient;
    }

    private boolean isDxs5000Client(String deviceName) {
        return clientConfig.clients.get(deviceName) instanceof DXS5000Client;
    }

    // private String formatString(String format, Object ... args) {
    //     StringBuffer buffer = new StringBuffer();
    //     Formatter formatter = new Formatter(buffer);
    //     String output = formatter.format(format, args).toString();
    //     formatter.close();
    //     return output;
    // }

    // private class SshClientPrinter {
    //     private static final int INTERVAL = 2;
    //     private final String[] FIELDS = new String[]{"IP", "Port", "Username", "Model"};
    //     private int[] width = Arrays.stream(FIELDS).mapToInt(String::length).toArray();

    //     private SshClientPrinter() {
    //     }

    //     public void updateMaxWidth(ClientConfig config) {
    //         int[] maxWidth = config.getMaxWidth();
    //         for (int i = 0; i < maxWidth.length; ++i) {
    //             width[i] = maxWidth[i] > width[i] ? maxWidth[i] : width[i];
    //         }
    //     }

    //     public void printDevices(HashMap<String, SshClient> clients) {
    //         String fmt = "%-" + String.valueOf(width[0] + INTERVAL) + "s" +
    //                 "%-" + String.valueOf(width[1] + INTERVAL) + "s" +
    //                 "%-" + String.valueOf(width[2] + INTERVAL) + "s" +
    //                 "%-" + String.valueOf(width[3] + INTERVAL) + "s" + "\n";
    //         log.info(formatString("Index  " + fmt, new Object[]{"IP", "Port", "Username", "Model"}));
    //         log.info("-".repeat(7 + Arrays.stream(width).sum() + INTERVAL * 4));
    //         ArrayList<SshClient> sshclients = new ArrayList<SshClient>(clients.values());
    //         for (int i = 0; i < sshclients.size(); ++i) {
    //             SshClient client = sshclients.get(i);
    //             log.info(formatString("%-7d" + fmt, new Object[]{i, client.ip, client.port, client.username, client.model}));
    //         }
    //     }
    // }

    private class SshClientConfigListener implements NetworkConfigListener {
        private SshClientConfigListener() {
        }

        public void event(NetworkConfigEvent event) {
            if ((event.type() == CONFIG_ADDED || event.type() == CONFIG_UPDATED) 
                && event.configClass().equals(SshClientConfig.class)) {
                SshClientConfig config = cfgService.getConfig(appId, SshClientConfig.class);
                if (config != null) {
                    clientConfig = new ClientConfig((ObjectNode)config.clientInfo());
                    // printer.updateMaxWidth(clientConfig);
                    log.info("Config file uploaded successfully");
                }
            }
        }
    }

    public static enum DeviceModel {
        DGS_3630,
        DXS_5000,
        SERVER,
        DIR_835;
    }
}
