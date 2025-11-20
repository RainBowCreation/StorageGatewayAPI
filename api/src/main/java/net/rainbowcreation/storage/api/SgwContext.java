package net.rainbowcreation.storage.api;

public final class SgwContext {
    private final StorageClient client;
    private final String baseNs; // e.g., "101"
    public SgwContext(StorageClient client, String baseNs){ this.client=client; this.baseNs=baseNs; }
    public StorageClient client(){ return client; }
    public String fullNs(String logical){ return baseNs + ":" + logical; }
}
