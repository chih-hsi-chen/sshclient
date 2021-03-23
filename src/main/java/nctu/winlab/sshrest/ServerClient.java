/*
 * Decompiled with CFR 0.144.
 */
package nctu.winlab.sshrest;

public interface ServerClient {
    public void execCommand(String cmd);
    public void execSudoCommand(String cmd);
}
