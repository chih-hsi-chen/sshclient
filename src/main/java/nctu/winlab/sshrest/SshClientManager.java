package nctu.winlab.sshrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
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

    private ObjectMapper mapper = new ObjectMapper();
    private HashMap<String, SshClient> clients;
    private HashMap<Integer, String> idToname;
    private int[] width;
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
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.removeListener(cfgListener);
        factories.forEach((cfgService)::unregisterConfigFactory);
        log.info("Stopped");
    }

    @Override
    public ArrayNode getDevices() {
        ArrayNode node = mapper.createArrayNode();
        int index = 0;
        for (String name : clients.keySet()) {
            SshClient client = clients.get(name);
            ObjectNode device = mapper.createObjectNode();
            device.put("index", index++);
            device.put("name", name);
            device.put("ip", client.ip);
            device.put("port", client.port);
            device.put("username", client.username);
            device.put("model", client.model);
            node.add((JsonNode) device);
        }
        return node;
    }

    @Override
    public ObjectNode getController(String deviceID) {
        String deviceName = convert2name(deviceID);
        ObjectNode reply = createGeneralReply();
        SshClient client;
        ArrayNode devices = reply.putArray("devices");

        if (deviceName.equals("ALL")) {
            for (String cname : clients.keySet()) {
                if (isSwitchClient(client = clients.get(cname)))
                    addDeviceReply(cname, ((SwitchClient) client).getController(), devices);
            }
        } else if (isSwitchClient(client = clients.get(deviceName))) {
            addDeviceReply(deviceName, ((SwitchClient) client).getController(), devices);
        } else {
            reply.put("error", true);
            reply.put("msg", "Remote machine should be switch");
        }
        return reply;
    }

    @Override
    public ObjectNode setController(String deviceID, String ip, String port) {
        String deviceName = convert2name(deviceID);
        ObjectNode reply = createGeneralReply();
        ArrayNode devices = reply.putArray("devices");
        SshClient client;

        if (deviceName.equals("ALL")) {
            for (String cname : clients.keySet()) {
                if (isSwitchClient(client = clients.get(cname)))
                    addDeviceReply(cname, ((SwitchClient) client).setController(ip, port), devices);
            }
        } else if (isSwitchClient(client = clients.get(deviceName))) {
            addDeviceReply(deviceName, ((SwitchClient) client).setController(ip, port), devices);
        } else {
            reply.put("error", true);
            reply.put("msg", "Remote machine should be switch");
        }
        return reply;
    }

    @Override
    public ObjectNode unsetController(String deviceID, String ip) {
        String deviceName = convert2name(deviceID);
        ObjectNode reply = mapper.createObjectNode();
        ArrayNode devices = reply.putArray("devices");
        SshClient client;

        if (deviceName.equals("ALL")) {
            for (String cname : clients.keySet()) {
                client = clients.get(cname);
                if (isSwitchClient(client = clients.get(cname)))
                    addDeviceReply(cname, ((SwitchClient) client).unsetController(ip), devices);
            }
        } else if (isSwitchClient(client = clients.get(deviceName))) {
            addDeviceReply(deviceName, ((SwitchClient) client).unsetController(ip), devices);
        } else {
            reply.put("error", true);
            reply.put("msg", "Remote machine should be switch");
        }
        return reply;
    }

    @Override
    public ObjectNode getFlows(String deviceID) {
        String deviceName = convert2name(deviceID);
        ObjectNode reply = createGeneralReply();
        ArrayNode devices = reply.putArray("devices");
        SshClient client;

        if (deviceName.equals("ALL")) {
            for (String cname : clients.keySet()) {
                if (isSwitchClient(client = clients.get(cname)))
                    addDeviceReply(cname, ((SwitchClient) client).getFlows(), devices);
            }
        } else if (isSwitchClient(client = clients.get(deviceName))) {
            addDeviceReply(deviceName, ((SwitchClient) client).getFlows(), devices);
        } else {
            reply.put("error", true);
            reply.put("msg", "Remote machine should be switch");
        }
        return reply;
    }

    @Override
    public ObjectNode getGroups(String deviceID) {
        String deviceName = convert2name(deviceID);
        ObjectNode reply = createGeneralReply();
        ArrayNode devices = reply.putArray("devices");
        SshClient client;

        if (deviceName.equals("ALL")) {
            for (String cname : clients.keySet()) {
                if (isSwitchClient(client = clients.get(cname)))
                    addDeviceReply(cname, ((SwitchClient) client).getGroups(), devices);
            }
        } else if (isSwitchClient(client = clients.get(deviceName))) {
            addDeviceReply(deviceName, ((SwitchClient) client).getGroups(), devices);
        } else {
            reply.put("error", true);
            reply.put("msg", "Remote machine should be switch");
        }
        return reply;
    }

    @Override
    public void getLogs(String deviceID, String filename) {
        String deviceName = convert2name(deviceID);
        SshClient client;
        try {
            FileWriter writer = filename != null ? new FileWriter(filename) : null;
            
            if (deviceName.equals("ALL")) {
                clients.values().forEach(c -> {
                    if (isSwitchClient(c)) {
                        ((SwitchClient) c).getLogs(writer);
                    }
                });
            } else if (isSwitchClient(client = clients.get(deviceName))) {
                ((SwitchClient) client).getLogs(writer);
            } else {
                log.info("Remote machine should be switch");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public ObjectNode execCommand(String deviceID, String cmd) {
        String deviceName = convert2name(deviceID);
        ObjectNode reply = createGeneralReply();
        ArrayNode devices = reply.putArray("devices");
        SshClient client;

        if (deviceName.equals("ALL")) {
            for (String cname : clients.keySet()) {
                if (isServerClient(client = clients.get(cname)))
                    addDeviceReply(cname, ((ServerClient) client).execCommand(cmd), devices);
            }
        } else if (isServerClient(client = clients.get(deviceName))) {
            addDeviceReply(deviceName, ((ServerClient) client).execCommand(cmd), devices);
        } else {
            log.info("Remote machine should be server");
        }
        return reply;
    }

    @Override
    public ObjectNode execSudoCommand(String deviceID, String cmd) {
        String deviceName = convert2name(deviceID);
        ObjectNode reply = createGeneralReply();
        ArrayNode devices = reply.putArray("devices");
        SshClient client;

        if (deviceName.equals("ALL")) {
            for (String cname : clients.keySet()) {
                if (isServerClient(client = clients.get(cname)))
                    addDeviceReply(cname, ((ServerClient) client).execSudoCommand(cmd), devices);
            }
        } else if (isServerClient(client = clients.get(deviceName))) {
            addDeviceReply(deviceName, ((ServerClient) client).execSudoCommand(cmd), devices);
        } else {
            log.info("Remote machine should be server");
        }
        return reply;
    }

    @Override
    public void setSsid(String deviceID, String ifname, String ssid) {
        String deviceName = convert2name(deviceID);
        SshClient client;

        if (deviceName.equals("ALL")) {
            clients.values().forEach(c -> {
                if (isApClient(c)) {
                    ((ApClient) c).setSsid(ifname, ssid);
                }
            });
        } else if (isApClient(client = clients.get(deviceName))) {
            ((ApClient) client).setSsid(ifname, ssid);
        } else {
            log.info("Remote machine should be AP");
        }
    }

    @Override
    public ObjectNode setVxlanSourceInterfaceLoopback(String deviceID, String loopbackId) {
        String deviceName = convert2name(deviceID);
        ObjectNode reply = createGeneralReply();
        ArrayNode devices = reply.putArray("devices");
        SshClient client;

        if (deviceName.equals("ALL")) {
            for (String cname : clients.keySet()) {
                if (isVxlanSupported(client = clients.get(cname)))
                    addDeviceReply(cname, ((VxlanSwitch) client).setVxlanSourceInterfaceLoopback(loopbackId), devices);
            }
        } else if (isVxlanSupported(client = clients.get(deviceName))) {
            addDeviceReply(deviceName, ((VxlanSwitch) client).setVxlanSourceInterfaceLoopback(loopbackId), devices);
        } else {
            reply.put("error", true);
            reply.put("msg", "Remote machine should be VXLAN supported switch");
        }
        return reply;
    }

    @Override
    public ObjectNode setVxlanVlan(String deviceID, String vnid, String vid) {
        String deviceName = convert2name(deviceID);
        ObjectNode reply = createGeneralReply();
        ArrayNode devices = reply.putArray("devices");
        SshClient client;

        if (deviceName.equals("ALL")) {
            for (String cname : clients.keySet()) {
                if (isVxlanSupported(client = clients.get(cname)))
                    addDeviceReply(cname, ((VxlanSwitch) client).setVxlanVlan(vnid, vid), devices);
            }
        } else if (isVxlanSupported(client = clients.get(deviceName))) {
            addDeviceReply(deviceName, ((VxlanSwitch) client).setVxlanVlan(vnid, vid), devices);
        } else {
            reply.put("error", true);
            reply.put("msg", "Remote machine should be VXLAN supported switch");
        }
        return reply;
    }

    @Override
    public ObjectNode setVxlanVtep(String deviceID, String vnid, String ip, String mac) {
        String deviceName = convert2name(deviceID);
        ObjectNode reply = createGeneralReply();
        ArrayNode devices = reply.putArray("devices");
        SshClient client;

        if (deviceName.equals("ALL")) {
            for (String cname : clients.keySet()) {
                if (isVxlanSupported(client = clients.get(cname)))
                    addDeviceReply(cname, ((VxlanSwitch) client).setVxlanVtep(vnid, ip, mac), devices);
            }
        } else if (isVxlanSupported(client = clients.get(deviceName))) {
            addDeviceReply(deviceName, ((VxlanSwitch) client).setVxlanVtep(vnid, ip, mac), devices);
        } else {
            reply.put("error", true);
            reply.put("msg", "Remote machine should be VXLAN supported switch");
        }
        return reply;
    }

    @Override
    public ObjectNode setVxlanStatus(String deviceID, boolean flag) {
        String deviceName = convert2name(deviceID);
        ObjectNode reply = createGeneralReply();
        ArrayNode devices = reply.putArray("devices");
        SshClient client;

        log.info("stauts: {}", flag);

        if (deviceName.equals("ALL")) {
            for (String cname : clients.keySet()) {
                if (isVxlanSupported(client = clients.get(cname)))
                    addDeviceReply(cname, ((VxlanSwitch) client).setVxlanStatus(flag), devices);
            }
        } else if (isVxlanSupported(client = clients.get(deviceName))) {
            addDeviceReply(deviceName, ((VxlanSwitch) client).setVxlanStatus(flag), devices);
        } else {
            reply.put("error", true);
            reply.put("msg", "Remote machine should be VXLAN supported switch");
        }
        return reply;
    }

    @Override
    public ObjectNode showVxlan(String deviceID) {
        String deviceName = convert2name(deviceID);
        ObjectNode reply = createGeneralReply();
        ArrayNode devices = reply.putArray("devices");
        SshClient client;

        if (deviceName.equals("ALL")) {
            for (String cname : clients.keySet()) {
                if (isVxlanSupported(client = clients.get(cname)))
                    addDeviceReply(cname, ((VxlanSwitch) client).showVxlan(), devices);
            }
        } else if (isVxlanSupported(client = clients.get(deviceName))) {
            addDeviceReply(deviceName, ((VxlanSwitch) client).showVxlan(), devices);
        } else {
            reply.put("error", true);
            reply.put("msg", "Remote machine should be VXLAN supported switch");
        }
        return reply;
    }

    @Override
    public int[] getWidth() {
        return width;
    }

    private String convert2name(String deviceID) {
        try {
            Integer index = Integer.parseInt(deviceID);
            return idToname.get(index);
        } catch (NumberFormatException e) {
            // Input is a string
            return deviceID;
        }
    }

    private ObjectNode createGeneralReply() {
        ObjectNode reply = mapper.createObjectNode();
        reply.put("error", false);
        reply.put("msg", "");
        return reply;
    }

    private void addDeviceReply(String deviceID, ObjectNode reply, ArrayNode arr) {
        ObjectNode device = mapper.createObjectNode();
        device.put("name", deviceID);
        merge(device, reply);
        arr.add(device);
    }

    private void merge(JsonNode main, JsonNode update) {
        Iterator<String> fieldnames = update.fieldNames();
        while (fieldnames.hasNext()) {
            String fn = fieldnames.next();
            JsonNode value = main.get(fn);
            if (value != null && value.isObject()) {
                merge(value, update.get(fn));
            } else {
                if (main instanceof ObjectNode) {
                    ((ObjectNode) main).set(fn, update.get(fn));
                }
            }
        }
    }

    private boolean isSwitchClient(SshClient client) {
        return client instanceof SwitchClient;
    }

    private boolean isServerClient(SshClient client) {
        return client instanceof ServerClient;
    }

    private boolean isApClient(SshClient client) {
        return client instanceof ApClient;
    }

    private boolean isVxlanSupported(SshClient client) {
        return client instanceof VxlanSwitch;
    }

    private class SshClientConfigListener implements NetworkConfigListener {
        private SshClientConfigListener() {
        }

        public void event(NetworkConfigEvent event) {
            if ((event.type() == CONFIG_ADDED || event.type() == CONFIG_UPDATED) 
                && event.configClass().equals(SshClientConfig.class)) {
                SshClientConfig config = cfgService.getConfig(appId, SshClientConfig.class);
                if (config != null) {
                    config.parseConfig();
                    clients = config.getClients();
                    idToname = config.getIdMap();
                    width = config.getMaxWidth();
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
