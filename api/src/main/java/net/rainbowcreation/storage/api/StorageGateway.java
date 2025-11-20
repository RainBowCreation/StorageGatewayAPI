package net.rainbowcreation.storage.api;

public interface StorageGateway {
  StorageClient open(String dbName, String secret) throws SecurityException;
}
