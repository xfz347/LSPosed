package org.meowcat.edxposed.manager.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import org.meowcat.edxposed.manager.App;
import org.meowcat.edxposed.manager.R;
import org.meowcat.edxposed.manager.util.GlideApp;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> implements Filterable {

    protected Context context;
    private final ApplicationInfo.DisplayNameComparator displayNameComparator;
    private Callback callback;
    protected List<ApplicationInfo> fullList, showList;
    private final DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private List<String> checkedList;
    private final PackageManager pm;
    private final ApplicationFilter filter;
    private Comparator<ApplicationInfo> cmp;
    private final SharedPreferences preferences;

    AppAdapter(Context context) {
        this.context = context;
        preferences = App.getPreferences();
        fullList = showList = Collections.emptyList();
        checkedList = Collections.emptyList();
        filter = new ApplicationFilter();
        pm = context.getPackageManager();
        displayNameComparator = new ApplicationInfo.DisplayNameComparator(pm);
        cmp = displayNameComparator;
        refresh();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_module, parent, false);
        return new ViewHolder(v);
    }

    private void loadApps() {
        fullList = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<ApplicationInfo> rmList = new ArrayList<>();
        for (ApplicationInfo info : fullList) {
            if (this instanceof ScopeAdapter) {
                if (AppHelper.isWhiteListMode()) {
                    List<String> whiteList = AppHelper.getWhiteList();
                    if (!whiteList.contains(info.packageName)) {
                        rmList.add(info);
                        continue;
                    }
                } else {
                    List<String> blackList = AppHelper.getBlackList();
                    if (blackList.contains(info.packageName)) {
                        rmList.add(info);
                        continue;
                    }
                }
                if (info.packageName.equals(((ScopeAdapter) this).modulePackageName)) {
                    rmList.add(info);
                }
            }
            if (!preferences.getBoolean("show_modules", true)) {
                if (info.metaData != null && info.metaData.containsKey("xposedmodule") || AppHelper.forceWhiteList.contains(info.packageName)) {
                    rmList.add(info);
                }
            }
            if (!preferences.getBoolean("show_system_apps", true) && (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                rmList.add(info);
            }
        }
        if (rmList.size() > 0) {
            fullList.removeAll(rmList);
        }
        AppHelper.makeSurePath();
        checkedList = generateCheckedList();
        sortApps();
        showList = fullList;
        if (callback != null) {
            callback.onDataReady();
        }
    }

    /**
     * Called during {@link #loadApps()} in non-UI thread.
     *
     * @return list of package names which should be checked when shown
     */
    protected List<String> generateCheckedList() {
        return Collections.emptyList();
    }

    private void sortApps() {
        switch (preferences.getInt("list_sort", 0)) {
            case 7:
                cmp = Collections.reverseOrder((ApplicationInfo a, ApplicationInfo b) -> {
                    try {
                        return Long.compare(pm.getPackageInfo(a.packageName, 0).lastUpdateTime, pm.getPackageInfo(b.packageName, 0).lastUpdateTime);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        return displayNameComparator.compare(a, b);
                    }
                });
                break;
            case 6:
                cmp = (ApplicationInfo a, ApplicationInfo b) -> {
                    try {
                        return Long.compare(pm.getPackageInfo(a.packageName, 0).lastUpdateTime, pm.getPackageInfo(b.packageName, 0).lastUpdateTime);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        return displayNameComparator.compare(a, b);
                    }
                };
                break;
            case 5:
                cmp = Collections.reverseOrder((ApplicationInfo a, ApplicationInfo b) -> {
                    try {
                        return Long.compare(pm.getPackageInfo(a.packageName, 0).firstInstallTime, pm.getPackageInfo(b.packageName, 0).firstInstallTime);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        return displayNameComparator.compare(a, b);
                    }
                });
                break;
            case 4:
                cmp = (ApplicationInfo a, ApplicationInfo b) -> {
                    try {
                        return Long.compare(pm.getPackageInfo(a.packageName, 0).firstInstallTime, pm.getPackageInfo(b.packageName, 0).firstInstallTime);
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                        return displayNameComparator.compare(a, b);
                    }
                };
                break;
            case 3:
                cmp = Collections.reverseOrder((a, b) -> a.packageName.compareTo(b.packageName));
                break;
            case 2:
                cmp = (a, b) -> a.packageName.compareTo(b.packageName);
                break;
            case 1:
                cmp = Collections.reverseOrder(displayNameComparator);
                break;
            case 0:
            default:
                cmp = displayNameComparator;
                break;
        }
        fullList.sort((a, b) -> {
            boolean aChecked = checkedList.contains(a.packageName);
            boolean bChecked = checkedList.contains(b.packageName);
            if (aChecked == bChecked) {
                return cmp.compare(a, b);
            } else if (aChecked) {
                return -1;
            } else {
                return 1;
            }

        });
    }

    @SuppressLint("NonConstantResourceId")
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.item_show_system) {
            item.setChecked(!item.isChecked());
            preferences.edit().putBoolean("show_system_apps", item.isChecked()).apply();
        } else if (itemId == R.id.item_show_modules) {
            item.setChecked(!item.isChecked());
            preferences.edit().putBoolean("show_modules", item.isChecked()).apply();
        } else if (itemId == R.id.item_sort_by_name) {
            item.setChecked(true);
            preferences.edit().putInt("list_sort", 0).apply();
        } else if (itemId == R.id.item_sort_by_name_reverse) {
            item.setChecked(true);
            preferences.edit().putInt("list_sort", 1).apply();
        } else if (itemId == R.id.item_sort_by_package_name) {
            item.setChecked(true);
            preferences.edit().putInt("list_sort", 2).apply();
        } else if (itemId == R.id.item_sort_by_package_name_reverse) {
            item.setChecked(true);
            preferences.edit().putInt("list_sort", 3).apply();
        } else if (itemId == R.id.item_sort_by_install_time) {
            item.setChecked(true);
            preferences.edit().putInt("list_sort", 4).apply();
        } else if (itemId == R.id.item_sort_by_install_time_reverse) {
            item.setChecked(true);
            preferences.edit().putInt("list_sort", 5).apply();
        } else if (itemId == R.id.item_sort_by_update_time) {
            item.setChecked(true);
            preferences.edit().putInt("list_sort", 6).apply();
        } else if (itemId == R.id.item_sort_by_update_time_reverse) {
            item.setChecked(true);
            preferences.edit().putInt("list_sort", 7).apply();
        } else {
            return false;
        }
        refresh();
        return true;
    }

    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_app_list, menu);
        menu.findItem(R.id.item_show_modules).setChecked(preferences.getBoolean("show_modules", true));
        menu.findItem(R.id.item_show_system).setChecked(preferences.getBoolean("show_system_apps", true));
        switch (preferences.getInt("list_sort", 0)) {
            case 7:
                menu.findItem(R.id.item_sort_by_update_time_reverse).setChecked(true);
                break;
            case 6:
                menu.findItem(R.id.item_sort_by_update_time).setChecked(true);
                break;
            case 5:
                menu.findItem(R.id.item_sort_by_install_time_reverse).setChecked(true);
                break;
            case 4:
                menu.findItem(R.id.item_sort_by_install_time).setChecked(true);
                break;
            case 3:
                menu.findItem(R.id.item_sort_by_package_name_reverse).setChecked(true);
                break;
            case 2:
                menu.findItem(R.id.item_sort_by_package_name).setChecked(true);
                break;
            case 1:
                menu.findItem(R.id.item_sort_by_name_reverse).setChecked(true);
                break;
            case 0:
                menu.findItem(R.id.item_sort_by_name).setChecked(true);
                break;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ApplicationInfo info = showList.get(position);
        holder.appName.setText(getAppLabel(info, pm));
        try {
            PackageInfo packageInfo = pm.getPackageInfo(info.packageName, 0);
            GlideApp.with(holder.appIcon)
                    .load(packageInfo)
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            holder.appIcon.setImageDrawable(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
            holder.appVersion.setText(packageInfo.versionName);
            holder.appVersion.setSelected(true);
            String creationDate = dateformat.format(new Date(packageInfo.firstInstallTime));
            String updateDate = dateformat.format(new Date(packageInfo.lastUpdateTime));
            holder.timestamps.setText(holder.itemView.getContext().getString(R.string.install_timestamps, creationDate, updateDate));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        holder.appPackage.setText(info.packageName);

        holder.mSwitch.setOnCheckedChangeListener(null);
        holder.mSwitch.setChecked(checkedList.contains(info.packageName));
        if (this instanceof ScopeAdapter) {
            holder.mSwitch.setEnabled(((ScopeAdapter) this).enabled);
        } else {
            holder.mSwitch.setEnabled(true);
        }
        holder.mSwitch.setOnCheckedChangeListener((v, isChecked) ->
                onCheckedChange(v, isChecked, info));
        holder.itemView.setOnClickListener(v -> {
            if (callback != null) {
                callback.onItemClick(v, info);
            }
        });
    }

    @Override
    public long getItemId(int position) {
        return showList.get(position).packageName.hashCode();
    }

    @Override
    public Filter getFilter() {
        return new ApplicationFilter();
    }

    @Override
    public int getItemCount() {
        return showList.size();
    }

    public void filter(String constraint) {
        filter.filter(constraint);
    }

    public void refresh() {
        //noinspection deprecation
        AsyncTask.THREAD_POOL_EXECUTOR.execute(this::loadApps);
    }

    protected void onCheckedChange(CompoundButton buttonView, boolean isChecked, ApplicationInfo info) {
        // override this to implements your functions
    }

    public interface Callback {
        void onDataReady();

        void onItemClick(View v, ApplicationInfo info);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView appIcon;
        TextView appName;
        TextView appPackage;
        TextView appVersion;
        TextView timestamps;
        SwitchCompat mSwitch;

        ViewHolder(View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.app_icon);
            appName = itemView.findViewById(R.id.app_name);
            appPackage = itemView.findViewById(R.id.package_name);
            appVersion = itemView.findViewById(R.id.version_name);
            timestamps = itemView.findViewById(R.id.timestamps);
            mSwitch = itemView.findViewById(R.id.checkbox);
        }
    }

    class ApplicationFilter extends Filter {

        private boolean lowercaseContains(String s, CharSequence filter) {
            return !TextUtils.isEmpty(s) && s.toLowerCase().contains(filter);
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint.toString().isEmpty()) {
                showList = fullList;
            } else {
                ArrayList<ApplicationInfo> filtered = new ArrayList<>();
                String filter = constraint.toString().toLowerCase();
                for (ApplicationInfo info : fullList) {
                    if (lowercaseContains(getAppLabel(info, pm), filter)
                            || lowercaseContains(info.packageName, filter)) {
                        filtered.add(info);
                    }
                }
                showList = filtered;
            }
            return null;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }
    }

    public static String getAppLabel(ApplicationInfo info, PackageManager pm) {
        try {
            if (info.labelRes > 0) {
                Resources res = pm.getResourcesForApplication(info);
                return res.getString(info.labelRes);
            }
        } catch (Exception ignored) {
        }
        return info.loadLabel(pm).toString();
    }
}
