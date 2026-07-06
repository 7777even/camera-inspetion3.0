package com.enviro.brain.entity;

/**
 * 支持 sync_version 增量同步的实体接口
 * 所有需要被鹊桥增量同步的业务实体实现此接口
 */
public interface HasSyncVersion {
    Long getSyncVersion();
}
