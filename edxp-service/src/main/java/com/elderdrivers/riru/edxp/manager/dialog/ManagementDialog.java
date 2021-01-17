package com.elderdrivers.riru.edxp.manager.dialog;

import android.app.ActivityThread;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toolbar;

import com.elderdrivers.riru.edxp.HandlerUtil;
import com.elderdrivers.riru.edxp.manager.InstalledModule;
import com.elderdrivers.riru.edxp.manager.ManagerProcess;
import com.elderdrivers.riru.edxp.util.Utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class ManagementDialog {

    private static final IBinder TOKEN = new Binder();

    private static class RemoveWindowRunnable implements Runnable {

        private WindowManager windowManager;
        private View view;
        private BroadcastReceiver receiver;

        @Override
        public void run() {
            try {
                windowManager.removeView(view);
            } catch (Throwable e) {
                Utils.logW("removeView", e);
            }

            try {
                view.getContext().unregisterReceiver(receiver);
            } catch (Throwable e) {
                Utils.logW("unregisterReceiver", e);
            }
        }
    }

    public static void show() {
        new Handler(Looper.getMainLooper()).post(ManagementDialog::showInternal);
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    private static void showInternal() {
        Context application = ActivityThread.currentActivityThread().getApplication();
        if (application == null) {
            return;
        }

        boolean isNight = (application.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_YES) != 0;
        Context context = new ContextThemeWrapper(application, isNight ? android.R.style.Theme_Material : android.R.style.Theme_Material_Light);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        float density = context.getResources().getDisplayMetrics().density;

        final RemoveWindowRunnable removeWindowRunnable = new RemoveWindowRunnable();
        removeWindowRunnable.windowManager = wm;
        removeWindowRunnable.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                new Handler(Looper.getMainLooper()).post(removeWindowRunnable);
            }
        };

        FrameLayout windowRoot = new FrameLayout(context) {
            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {
                if (event.getKeyCode() != KeyEvent.KEYCODE_BACK) {
                    return super.dispatchKeyEvent(event);
                }

                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                    getKeyDispatcherState().startTracking(event, this);
                    return true;
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    getKeyDispatcherState().handleUpEvent(event);

                    if (event.isTracking() && !event.isCanceled()) {
                        HandlerUtil.getMainHandler().post(removeWindowRunnable);
                        return true;
                    }
                }
                return super.dispatchKeyEvent(event);
            }
        };
        removeWindowRunnable.view = windowRoot;

        TypedArray a = context.obtainStyledAttributes(new int[]{android.R.attr.colorBackground, android.R.attr.colorPrimary});
        int colorBackground = a.getColor(0, 0);
        int colorPrimary = a.getColor(1, 0);

        a.recycle();

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setBackgroundColor(colorBackground);
        windowRoot.addView(linearLayout);
        Toolbar toolbar = new Toolbar(context);
        toolbar.setBackgroundColor(colorPrimary);
        toolbar.setElevation(2 * density);
        toolbar.setTitle("LSPosed Manager");
        linearLayout.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView textView = new TextView(context);
        textView.setText(ManagerProcess.CONFIG_PATH);
        linearLayout.addView(textView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ListView listView = new ListView(context);
        listView.setFastScrollEnabled(true);
        Adapter adapter = new Adapter();
        listView.setAdapter(adapter);
        linearLayout.addView(listView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        HandlerUtil.getWorkerHandler().post(() -> {
            List<InstalledModule> data = new ArrayList<>();
            try {
                PackageManager pm = context.getPackageManager();

                for (PackageInfo pkg : pm.getInstalledPackages(PackageManager.GET_META_DATA)) {
                    ApplicationInfo app = pkg.applicationInfo;
                    if (!app.enabled)
                        continue;
                    InstalledModule installed = null;
                    if (app.metaData != null && app.metaData.containsKey("xposedmodule")) {
                        installed = new InstalledModule(pkg, false);
                        installed.loadAppName(pm);
                        installed.loadDescription(pm);
                        data.add(installed);
                    }
                }

            } catch (Throwable e) {
                Utils.logE("getInstalledPackages", e);
            }

            linearLayout.post(() -> {
                if (!data.isEmpty()) {
                    adapter.updateData(data);
                }
            });

        });

        // TODO: Everything

        windowRoot.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                v.removeOnAttachStateChangeListener(this);
                v.requestApplyInsets();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {

            }
        });

        windowRoot.setSystemUiVisibility(windowRoot.getSystemUiVisibility()
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        windowRoot.setOnApplyWindowInsetsListener((v, insets) -> {
            int left = insets.getSystemWindowInsetLeft();
            int top = insets.getSystemWindowInsetTop();
            int right = insets.getSystemWindowInsetRight();
            int bottom = insets.getSystemWindowInsetBottom();

            toolbar.setPadding(
                    toolbar.getPaddingLeft(),
                    top,
                    toolbar.getPaddingRight(),
                    toolbar.getPaddingBottom()
            );

            listView.setPadding(
                    listView.getPaddingLeft(),
                    listView.getPaddingTop(),
                    listView.getPaddingRight(),
                    bottom
            );

            return insets;
        });

        WindowManager.LayoutParams attr = new WindowManager.LayoutParams();
        attr.width = ViewGroup.LayoutParams.MATCH_PARENT;
        attr.height = ViewGroup.LayoutParams.MATCH_PARENT;
        attr.dimAmount = 0;
        attr.flags = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        attr.type = WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG;
        attr.token = TOKEN;
        attr.gravity = Gravity.CENTER;
        attr.windowAnimations = android.R.style.Animation_Dialog;
        attr.format = PixelFormat.TRANSLUCENT;
        try {
            Field privateFlags = WindowManager.LayoutParams.class.getDeclaredField("privateFlags");
            Field SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS_field = WindowManager.LayoutParams.class.getDeclaredField("SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS");
            int SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS = SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS_field.getInt(attr);
            int flags = privateFlags.getInt(attr);
            privateFlags.setInt(attr, flags | SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        try {
            wm.addView(windowRoot, attr);
        } catch (Exception e) {
            Utils.logW("addView", e);
            return;
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);

        try {
            context.registerReceiver(removeWindowRunnable.receiver, intentFilter);
            Utils.logI("registerReceiver android.intent.action.CLOSE_SYSTEM_DIALOGS");
        } catch (Exception e) {
            Utils.logI("registerReceiver android.intent.action.CLOSE_SYSTEM_DIALOGS", e);
        }
    }

    private static class Adapter extends BaseAdapter {

        private final List<InstalledModule> data;

        private Adapter() {
            this.data = new ArrayList<>();
        }

        public void updateData(List<InstalledModule> newData) {
            data.clear();
            data.addAll(newData);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public InstalledModule getItem(int position) {
            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new ModuleItemView(parent.getContext());
            }
            ModuleItemView title = (ModuleItemView) convertView;
            title.bind(getItem(position).getAppName(), true);
            return convertView;
        }
    }

    private static class ModuleItemView extends RelativeLayout {
        TextView title;
        Switch checkbox;

        public ModuleItemView(Context context) {
            super(context);
            title = new TextView(context);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.addRule(ALIGN_PARENT_LEFT);
            addView(title, lp);
            checkbox = new Switch(context);
            lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.addRule(ALIGN_PARENT_RIGHT);
            addView(checkbox, lp);
        }

        public void bind(String name, boolean enable){
            title.setText(name);
            checkbox.setChecked(enable);
        }
    }
}
