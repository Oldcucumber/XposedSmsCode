package com.tianma.xsmscode.feature.config;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.PreferenceFragmentCompat;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.tianma8023.xposed.smscode.R;
import java.util.concurrent.*;

public final class ConfigBackupUi {
    private final PreferenceFragmentCompat fragment;
    private final ActivityResultLauncher<String> export;
    private final ActivityResultLauncher<String[]> select;
    private boolean busy;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    public ConfigBackupUi(PreferenceFragmentCompat fragment) {
        this.fragment = fragment;
        export = fragment.registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
            if (uri != null) run(() -> { ConfigurationBackup.export(fragment.requireContext().getApplicationContext(), uri); return null; }, false);
        });
        select = fragment.registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null || busy) return;
            Context c = fragment.requireContext().getApplicationContext();
            busy = true;
            IO.execute(() -> {
                try {
                    com.google.gson.JsonObject doc = ConfigurationBackup.read(c.getContentResolver().openInputStream(uri));
                    MAIN.post(() -> {
                        busy = false; if (!fragment.isAdded()) return;
                        new MaterialDialog.Builder(fragment.requireContext()).title(R.string.config_backup_import)
                            .content(R.string.config_backup_replace).positiveText(android.R.string.ok).negativeText(android.R.string.cancel)
                            .onPositive((dialog, which) -> run(() -> { ConfigurationBackup.restore(c, doc); return null; }, true)).show();
                    });
                } catch (Exception e) { MAIN.post(() -> { busy = false; message(false); }); }
            });
        });
    }
    public void show() {
        if (busy) return;
        new MaterialDialog.Builder(fragment.requireContext()).title(R.string.config_backup_title)
            .items(R.array.config_backup_actions).itemsCallback((dialog, view, position, text) -> {
                if (position == 0) export.launch("SmsCode-settings-" + System.currentTimeMillis() + ".json");
                else select.launch(new String[]{"application/json", "text/plain", "application/octet-stream"});
            }).show();
    }
    private void run(Callable<Void> action, boolean recreate) {
        if (busy) return; busy = true; fragment.getPreferenceScreen().setEnabled(false);
        IO.execute(() -> {
            boolean success;
            try { action.call(); success = true; }
            catch (Exception e) { com.tianma.xsmscode.common.utils.XLog.e("Configuration backup failed", e); success = false; }
            final boolean ok = success;
            MAIN.post(() -> {
                busy = false; if (!fragment.isAdded()) return;
                fragment.getPreferenceScreen().setEnabled(true); message(ok);
                if (ok && recreate) {
                    try {
                        java.lang.reflect.Method reload = com.jaredrummler.cyanea.Cyanea.class.getDeclaredMethod("loadDefaults");
                        reload.setAccessible(true); reload.invoke(com.jaredrummler.cyanea.Cyanea.getInstance());
                    } catch (ReflectiveOperationException e) { com.tianma.xsmscode.common.utils.XLog.e("Theme refresh failed", e); }
                    fragment.requireActivity().recreate();
                }
            });
        });
    }
    private void message(boolean success) {
        if (fragment.isAdded()) Toast.makeText(fragment.requireContext(), success ? R.string.config_backup_success : R.string.config_backup_failed, Toast.LENGTH_LONG).show();
    }
}
