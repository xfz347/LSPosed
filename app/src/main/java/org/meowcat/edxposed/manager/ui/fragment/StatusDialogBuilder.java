package org.meowcat.edxposed.manager.ui.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.meowcat.edxposed.manager.App;
import org.meowcat.edxposed.manager.BuildConfig;
import org.meowcat.edxposed.manager.Constants;
import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.databinding.StatusInstallerBinding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;

@SuppressLint("StaticFieldLeak")
public class StatusDialogBuilder extends MaterialAlertDialogBuilder {

    public StatusDialogBuilder(@NonNull Context context) {
        super(context);
        StatusInstallerBinding binding = StatusInstallerBinding.inflate(LayoutInflater.from(context), null, false);

        String installedXposedVersion = Constants.getXposedVersion();
        String mAppVer = String.format("%s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
        binding.manager.setText(mAppVer);

        if (installedXposedVersion != null) {
            binding.api.setText(Constants.getXposedApiVersion() + ".0");
            binding.framework.setText(installedXposedVersion + " (" + Constants.getXposedVariant() + ")");
        }

        binding.androidVersion.setText(context.getString(R.string.android_sdk, getAndroidVersion(), Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
        binding.manufacturer.setText(getUIFramework());
        binding.cpu.setText(getCompleteArch());

        determineVerifiedBootState(binding);
        setView(binding.getRoot());
    }

    private static String getCompleteArch() {
        String info = "";

        try {
            FileReader fr = new FileReader("/proc/cpuinfo");
            BufferedReader br = new BufferedReader(fr);
            String text;
            while ((text = br.readLine()) != null) {
                if (!text.startsWith("processor")) break;
            }
            br.close();
            String[] array = text != null ? text.split(":\\s+", 2) : new String[0];
            if (array.length >= 2) {
                info += array[1] + " ";
            }
        } catch (IOException ignored) {
        }

        info += Build.SUPPORTED_ABIS[0];
        return info + " (" + getArch() + ")";
    }

    @SuppressWarnings("deprecation")
    private static String getArch() {
        if (Build.CPU_ABI.equals("arm64-v8a")) {
            return "arm64";
        } else if (Build.CPU_ABI.equals("x86_64")) {
            return "x86_64";
        } else if (Build.CPU_ABI.equals("mips64")) {
            return "mips64";
        } else if (Build.CPU_ABI.startsWith("x86") || Build.CPU_ABI2.startsWith("x86")) {
            return "x86";
        } else if (Build.CPU_ABI.startsWith("mips")) {
            return "mips";
        } else if (Build.CPU_ABI.startsWith("armeabi-v5") || Build.CPU_ABI.startsWith("armeabi-v6")) {
            return "armv5";
        } else {
            return "arm";
        }
    }

    private void determineVerifiedBootState(StatusInstallerBinding binding) {
        try {
            @SuppressLint("PrivateApi") Class<?> c = Class.forName("android.os.SystemProperties");
            Method m = c.getDeclaredMethod("get", String.class, String.class);
            m.setAccessible(true);

            String propSystemVerified = (String) m.invoke(null, "partition.system.verified", "0");
            String propState = (String) m.invoke(null, "ro.boot.verifiedbootstate", "");
            File fileDmVerityModule = new File("/sys/module/dm_verity");

            boolean verified = false;
            if (propSystemVerified != null) {
                verified = !propSystemVerified.equals("0");
            }
            boolean detected = false;
            if (propState != null) {
                detected = !propState.isEmpty() || fileDmVerityModule.exists();
            }

            if (verified) {
                binding.dmverity.setText(R.string.verified_boot_active);
                binding.dmverity.setTextColor(ContextCompat.getColor(getContext(), R.color.warning));
            } else if (detected) {
                binding.dmverity.setText(R.string.verified_boot_deactivated);
            } else {
                binding.dmverity.setText(R.string.verified_boot_none);
                binding.dmverity.setTextColor(ContextCompat.getColor(getContext(), R.color.warning));
            }
        } catch (Exception e) {
            Log.e(App.TAG, "Could not detect Verified Boot state", e);
        }
    }

    private String getAndroidVersion() {
        switch (Build.VERSION.SDK_INT) {
            case 26:
            case 27:
                return "Oreo";
            case 28:
                return "Pie";
            case 29:
                return "Q";
            case 30:
                return "R";
        }
        return "Unknown";
    }

    private String getUIFramework() {
        String manufacturer = Character.toUpperCase(Build.MANUFACTURER.charAt(0)) + Build.MANUFACTURER.substring(1);
        if (!Build.BRAND.equals(Build.MANUFACTURER)) {
            manufacturer += " " + Character.toUpperCase(Build.BRAND.charAt(0)) + Build.BRAND.substring(1);
        }
        manufacturer += " " + Build.MODEL + " ";
        if (new File("/system/framework/twframework.jar").exists() || new File("/system/framework/samsung-services.jar").exists()) {
            manufacturer += "(TouchWiz)";
        } else if (new File("/system/framework/framework-miui-res.apk").exists() || new File("/system/app/miui/miui.apk").exists() || new File("/system/app/miuisystem/miuisystem.apk").exists()) {
            manufacturer += "(Mi UI)";
        } else if (new File("/system/priv-app/oneplus-framework-res/oneplus-framework-res.apk").exists()) {
            manufacturer += "(Oxygen/Hydrogen OS)";
        } else if (new File("/system/framework/com.samsung.device.jar").exists() || new File("/system/framework/sec_platform_library.jar").exists()) {
            manufacturer += "(One UI)";
        }
        return manufacturer;
    }
}