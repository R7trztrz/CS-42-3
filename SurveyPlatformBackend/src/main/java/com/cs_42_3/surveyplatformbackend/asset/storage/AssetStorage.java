package com.cs_42_3.surveyplatformbackend.asset.storage;

/**
 * Keeps filesystem details replaceable by an object-storage implementation.
 *
 * @author Simon Tian
 */
public interface AssetStorage {
    void write(String key, byte[] bytes);
    byte[] read(String key);
    boolean exists(String key);
    void delete(String key);
}
