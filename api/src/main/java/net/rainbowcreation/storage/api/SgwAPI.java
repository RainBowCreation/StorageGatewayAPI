package net.rainbowcreation.storage.api;

public final class SgwAPI {
    private static volatile StorageGateway GATEWAY;
    private SgwAPI() {}
    public static void publish(StorageGateway g) { GATEWAY = g; }
    public static StorageGateway get() { return GATEWAY; }
}
