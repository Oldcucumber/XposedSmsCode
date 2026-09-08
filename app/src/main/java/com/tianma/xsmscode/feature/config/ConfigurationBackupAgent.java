package com.tianma.xsmscode.feature.config;

import android.app.backup.*;
import android.os.ParcelFileDescriptor;
import java.io.*;

public final class ConfigurationBackupAgent extends BackupAgent {
    @Override public void onBackup(ParcelFileDescriptor oldState, BackupDataOutput data, ParcelFileDescriptor newState) { }
    @Override public void onRestore(BackupDataInput data, int version, ParcelFileDescriptor newState) { }
    @Override public void onFullBackup(FullBackupDataOutput data) throws IOException {
        synchronized (ConfigStore.class) {
            ConfigStore.writeAtomic(ConfigStore.snapshotFile(this), ConfigStore.capture(this).toString());
            fullBackupFile(ConfigStore.snapshotFile(this), data);
        }
    }
    @Override public void onRestoreFinished() {
        File file = ConfigStore.snapshotFile(this);
        if (file.isFile()) try { ConfigurationBackup.restore(this, ConfigurationBackup.read(new FileInputStream(file))); }
        catch (IOException e) { com.tianma.xsmscode.common.utils.XLog.e("System configuration restore failed", e); }
    }
}
