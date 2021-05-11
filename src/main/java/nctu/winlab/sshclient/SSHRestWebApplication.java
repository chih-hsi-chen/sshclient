package nctu.winlab.sshclient;

import java.util.Set;
import org.onlab.rest.AbstractWebApplication;

public class SSHRestWebApplication extends AbstractWebApplication {
    public Set<Class<?>> getClasses() {
        return this.getClasses(SSHRestWebResource.class);
    }
}
