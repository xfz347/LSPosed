package org.meowcat.edxposed.manager.adapters;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.widget.CompoundButton;

import org.meowcat.edxposed.manager.App;
import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.util.ModuleUtil;
import org.meowcat.edxposed.manager.util.ToastUtil;

import java.util.Collection;
import java.util.List;


public class BlackListAdapter extends AppAdapter {

    private final boolean isWhiteListMode;
    private List<String> checkedList;

    public BlackListAdapter(Context context, boolean isWhiteListMode) {
        super(context);
        this.isWhiteListMode = isWhiteListMode;
    }

    @Override
    public List<String> generateCheckedList() {
        if (App.getPreferences().getBoolean("hook_modules", true)) {
            Collection<ModuleUtil.InstalledModule> installedModules = ModuleUtil.getInstance().getModules().values();
            for (ModuleUtil.InstalledModule info : installedModules) {
                AppHelper.forceWhiteList.add(info.packageName);
            }
        }
        AppHelper.makeSurePath();
        if (isWhiteListMode) {
            checkedList = AppHelper.getWhiteList();
        } else {
            checkedList = AppHelper.getBlackList();
        }
        return checkedList;
    }

    @Override
    protected void onCheckedChange(CompoundButton view, boolean isChecked, ApplicationInfo info) {
        boolean success = isChecked ?
                AppHelper.addPackageName(isWhiteListMode, info.packageName) :
                AppHelper.removePackageName(isWhiteListMode, info.packageName);
        if (success) {
            if (isChecked) {
                checkedList.add(info.packageName);
            } else {
                checkedList.remove(info.packageName);
            }
        } else {
            ToastUtil.showShortToast(context, R.string.add_package_failed);
            view.setChecked(!isChecked);
        }
    }
}
