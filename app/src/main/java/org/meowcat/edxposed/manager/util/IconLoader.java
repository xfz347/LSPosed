package org.meowcat.edxposed.manager.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

import org.meowcat.edxposed.manager.R;

import me.zhanghai.android.appiconloader.glide.AppIconModelLoader;

@GlideModule
public class IconLoader extends AppGlideModule {
    @Override
    public void registerComponents(Context context, @NonNull Glide glide, Registry registry) {
        int iconSize = context.getResources().getDimensionPixelSize(R.dimen.app_icon_size);
        registry.prepend(PackageInfo.class, Bitmap.class, new AppIconModelLoader.Factory(iconSize,
                false, context));
    }
}

