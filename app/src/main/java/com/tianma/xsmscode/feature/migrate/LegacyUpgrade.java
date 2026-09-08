package com.tianma.xsmscode.feature.migrate;

import android.content.Context;
import com.tianma.xsmscode.feature.config.*;
import com.tianma.xsmscode.common.utils.XLog;
import java.io.*;
import java.nio.file.Files;

/** Retained-data upgrades only. Never clears existing preferences or databases. */
public final class LegacyUpgrade {
    public static void run(Context context) throws IOException {
        ConfigurationBackup.recover(context);
        if (context.getSharedPreferences("apiupdate_migration", Context.MODE_PRIVATE).getBoolean("retained_v1", false)) return;
        File external = context.getExternalFilesDir(null);
        if (external != null && external.isDirectory()) {
            File[] files = external.listFiles();
            if (files != null) for (File source : files) {
                String name = source.getName();
                if (!source.isFile() || !(name.equals("code_rule_template") || name.equals("code_rules")
                        || name.equals("blocked_apps") || name.startsWith("CodeRecord_"))) continue;
                File destination = new File(context.getFilesDir(), name);
                if (!destination.exists()) {
                    // Keep source files until the whole import has succeeded.
                    File temporary = new File(context.getNoBackupFilesDir(), "legacy-copy.tmp");
                    Files.copy(source.toPath(), temporary.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    Files.move(temporary.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        if (!com.tianma.xsmscode.ui.record.CodeRecordRestoreManager.importToDatabase(context))
            throw new IOException("Pending legacy records could not be restored");
        context.getSharedPreferences("apiupdate_migration", Context.MODE_PRIVATE).edit().putBoolean("retained_v1", true).commit();
    }
}
