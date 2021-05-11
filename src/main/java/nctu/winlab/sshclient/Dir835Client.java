package nctu.winlab.sshclient;

import java.util.logging.Logger;

public class Dir835Client extends SshExecClient implements ApClient {
    private static Logger log = Logger.getLogger(Dir835Client.class.getName());

    public Dir835Client(String ip, String port, String username, String password, String model) {
        super(ip, port, username, password);
        this.model = model;
    }

    @Override
    public void setSsid(String ifname, String ssid) {
        try {
            log.info(sendCmd("uci set wireless." + ifname + ".ssid=" + ssid));
            log.info(sendCmd("uci commit wireless"));
            log.info(sendCmd("wifi"));
        }
        catch (Exception e) {
            return;
        }
    }
}
