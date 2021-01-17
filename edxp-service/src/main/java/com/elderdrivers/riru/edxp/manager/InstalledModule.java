package com.elderdrivers.riru.edxp.manager;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import static com.elderdrivers.riru.edxp.util.MetaDataReader.extractIntPart;

public class InstalledModule {
    //private static final int FLAG_FORWARD_LOCK = 1 << 29;
    public final String packageName;
    public final String versionName;
    public final long versionCode;
    public final int minVersion;
    public final long installTime;
    public final long updateTime;
    final boolean isFramework;
    public ApplicationInfo app;
    public PackageInfo pkg;
    private String appName; // loaded lazyily
    private String description; // loaded lazyily

    public InstalledModule(PackageInfo pkg, boolean isFramework) {
        this.app = pkg.applicationInfo;
        this.pkg = pkg;
        this.packageName = pkg.packageName;
        this.isFramework = isFramework;
        this.versionName = pkg.versionName;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            this.versionCode = pkg.versionCode;
        } else {
            this.versionCode = pkg.getLongVersionCode();
        }
        this.installTime = pkg.firstInstallTime;
        this.updateTime = pkg.lastUpdateTime;

        if (isFramework) {
            this.minVersion = 0;
            this.description = "";
        } else {
            Object minVersionRaw = app.metaData.get("xposedminversion");
            if (minVersionRaw instanceof Integer) {
                this.minVersion = (Integer) minVersionRaw;
            } else if (minVersionRaw instanceof String) {
                this.minVersion = extractIntPart((String) minVersionRaw);
            } else {
                this.minVersion = 0;
            }

        }
    }

    public boolean isInstalledOnExternalStorage() {
        return (app.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) != 0;
    }

    public void loadAppName(PackageManager pm) {
        appName = app.loadLabel(pm).toString();
    }

    public String getAppName() {
        return appName;
    }

    public void loadDescription(PackageManager pm) {
        Object descriptionRaw = app.metaData.get("xposeddescription");
        String descriptionTmp = null;
        if (descriptionRaw instanceof String) {
            descriptionTmp = ((String) descriptionRaw).trim();
        } else if (descriptionRaw instanceof Integer) {
            try {
                int resId = (Integer) descriptionRaw;
                if (resId != 0)
                    descriptionTmp = pm.getResourcesForApplication(app).getString(resId).trim();
            } catch (Exception ignored) {
            }
        }
        description = (descriptionTmp != null) ? descriptionTmp : "";
    }

    public String getDescription() {
        return description;
    }
}