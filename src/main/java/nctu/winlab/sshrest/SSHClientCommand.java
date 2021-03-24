package nctu.winlab.sshrest;
/*
 * Copyright 2019-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;

import static nctu.winlab.sshrest.SshClientService.ALL_CLIENTS_OPERATION_INDEX;

import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static nctu.winlab.sshrest.SSHConstants.ANSI_BOLD;
import static nctu.winlab.sshrest.SSHConstants.ANSI_GREEN;
import static nctu.winlab.sshrest.SSHConstants.ANSI_RESET;

/**
 * SSH client CLI
 */
@Service
@Command(scope = "onos", name = "sshctl",
         description = "SSH client CLI")
public class SSHClientCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "instruction",
            description = "(get|set|unset|exec|vxlan)",
            required = true, multiValued = false)
    private String instruction;
    
    @Argument(index = 1, name = "contents",
            description = "instruction specific contents",
            required = true, multiValued = true)
    private String[] contents;

    @Option(name = "-v", aliases = "--vtep",
            description = "VTEP IP address",
            required = false, multiValued = false)
    private String vtepIP = null;

    @Option(name = "-l", aliases = "--loopback",
            description = "Loopback ID",
            required = false, multiValued = false)
    private String loopbackId = null;

    @Option(name = "--vnid",
            description = "VNID",
            required = false, multiValued = false)
    private String vnid = null;

    @Option(name = "--vid",
            description = "VLAN ID",
            required = false, multiValued = false)
    private String vid = null;

    @Option(name = "--tenant",
            description = "Tenant MAC address",
            required = false, multiValued = false)
    private String mac = null;

    @Option(name = "-i", aliases = "--idex",
            description = "Indicate (by index) machine to be operated on.",
            required = false, multiValued = false)
    private String targetId = null;

    @Option(name = "-n", aliases = "--name",
            description = "Indicate (by index) machine to be operated on.",
            required = false, multiValued = false)
    private String targetName = null;

    private final SshClientService service = get(SshClientService.class);
    private String target = null;
    
    @Override
    protected void doExecute() {
        determineTarget();
        switch (instruction) {
            case "get":
                getHandler();
                break;
            case "set":
                setHandler(false);
                break;
            case "unset":
                setHandler(true);
                break;
            case "exec":
                execHandler();
                break;
            case "vxlan":
                vxlanHandler();
                break;
            default:
                System.out.printf("Invalid instruction: %s\n", instruction);
        }
    }

    private void determineTarget() {
        if (targetId != null)
            target = targetId;
        else if (targetName != null)
            target = targetName;
        else
            target = ALL_CLIENTS_OPERATION_INDEX;
    }

    private void getHandler() {
        switch (contents[0]) {
            case "device":
                printDeviceArray(service.getDevices());
                break;
            case "controller":
                output(service.getController(target));
                break;
            case "flow":
                output(service.getFlows(target));
                break;
            case "group":
                output(service.getGroups(target));
                break;
            case "log":
                String filename = contents.length > 1 ? contents[1] : null;
                service.getLogs(target, filename);
                break;
            default:
                System.out.printf("Invalid resource: %s\n", contents[0]);
        }
    }

    private void setHandler(boolean unsetFlag) {
        switch (contents[0]) {
            case "controller":
                String ip = contents.length > 1 ? contents[1] : null;
                if (ip == null) {
                    System.out.println("Controller IP should be assigned");
                    break;
                }
                String port = contents.length > 2 ? contents[2] : "";
                if (unsetFlag) {
                    output(service.unsetController(target, ip));
                } else {
                    output(service.setController(target, ip, port));
                }
                break;
            case "ssid":
                if (contents.length < 3) {
                    System.out.println("Usage:");
                    System.out.println("  sshctl set controller <ip> [port]");
                    System.out.println("  sshctl set ssid <ifname> <ssid>");
                    break;
                }
                service.setSsid(target, contents[1], contents[2]);
                break;
            default:
                System.out.printf("Invalid resource: %s\n", contents[0]);
        }
    }

    private void execHandler() {
        service.execCommand(target, contents[0]);
    }

    private void vxlanHandler() {
        if (contents[0].equals("set")) {
            if (contents.length < 2) {
                System.out.println("Usage:");
                System.out.println("  sshctl [OPTIONS] vxlan set { loopback | vtep | vlan }");
                return;
            }
            if (contents[1].equals("loopback"))
                output(service.setVxlanSourceInterfaceLoopback(target, loopbackId));
            else if (contents[1].equals("vtep"))
                output(service.setVxlanVtep(target, vnid, vtepIP, mac));
            else if (contents[1].equals("vlan"))
                output(service.setVxlanVlan(target, vnid, vid));
            else {
                System.out.println("Usage:");
                System.out.println("  sshctl [OPTIONS] vxlan set { loopback | vtep | vlan }");
            }
        } else if (contents[0].equals("enable") || contents[0].equals("disable")) {
            output(service.setVxlanStatus(target, contents[0].startsWith("e")));
        } else if (contents[0].equals("show")) {
            output(service.showVxlan(target));
        } else {
            System.out.printf("Invalid VXLAN command: %s\n", contents[1]);
        }
    }

    private void printDeviceArray(ArrayNode devices) {
        int INTERVAL = 4;
        int[] width = service.getWidth();
        String fmt = "";

        for (int i = 0; i < width.length; i++) {
            fmt += "%-" + String.valueOf(width[i] + INTERVAL * 2) + "s";
        }
        fmt += "\n";

        System.out.printf("Index  " + fmt, "Name", "IP", "Port", "Username", "Model");
        System.out.printf("-".repeat(7 + Arrays.stream(width).sum() + INTERVAL * 10) + "\n");

        for (JsonNode device : devices) {
            System.out.printf("%-7d" + fmt, device.get("index").asInt(),
                                   device.get("name").asText(),
                                   device.get("ip").asText(),
                                   device.get("port").asText(),
                                   device.get("username").asText(),
                                   device.get("model").asText());
        }
    }

    private void output(ObjectNode res) {
        if (res.path("error").asBoolean(false)) {
            System.out.printf("Error: %s", res.path("msg").asText("unknown error"));
        } else {
            ArrayNode devices = (ArrayNode) res.get("devices");
            for (JsonNode device : devices) {
                if (device instanceof ObjectNode) {
                    String raw = device.get("raw").asText("").trim();
                    System.out.printf(ANSI_GREEN + ANSI_BOLD + "[%s]\n" + ANSI_RESET, device.get("name").asText());
                    System.out.printf("%s\n", raw.equals("") ? "success" : raw);
                }
            }
        }
    }
}
