/*
 * Decompiled with CFR 0.144.
 */
package nctu.winlab.sshclient;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface ServerClient {
    public ObjectNode execCommand(String cmd);
    public ObjectNode execSudoCommand(String cmd);
}
