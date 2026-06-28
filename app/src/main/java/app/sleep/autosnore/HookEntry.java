package app.sleep.autosnore;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.view.View;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class HookEntry implements IXposedHookLoadPackage {
    private static final String TAG = "[AutoSnoreOnSleep] ";
    private static final String TARGET_PACKAGE = "com.heytap.health";
    private static final String SLEEP_RECEIVER =
            "com.heytap.health.sleep.receiver.SleepStateReceiver";
    private static final String SLEEP_HISTORY_ACTIVITY =
            "com.heytap.health.sleep.SleepHistoryActivity";
    private static final String SNORE_MONITOR_CARD =
            "com.heytap.health.sleep.day.card.SnoreMonitorCard";
    private static final String RAW_SLEEP_PROCESSOR =
            "com.heytap.device.data.sporthealth.receive.t";
    private static final String SLEEP_MODE_MANAGER =
            "com.heytap.device.sleep.SleepModeManager";
    private static final String SLEEP_MODEL_SETTINGS =
            "com.heytap.databaseengine.model.SleepModelSettings";
    private static final String AUDIO_RECORD_SERVICE =
            "com.heytap.health.sleep.audio.service.AudioRecordService2";
    private static final String ACTION_FALL_ASLEEP =
            "action_broadcast_device_fall_asleep";
    private static final String EXTRA_FOREGROUND_START =
            "app.sleep.autosnore.EXTRA_FOREGROUND_START";
    private static final long DUPLICATE_GUARD_MS = 10_000L;
    private static final long FOREGROUND_RETRY_MS = 500L;
    private static final long MAIN_PROCESS_RESTART_DELAY_MS = 700L;
    private static final long RETURN_HOME_MS = 1_500L;
    private static final long PENDING_TIMEOUT_MS = 60_000L;
    private static final AtomicLong LAST_START = new AtomicLong(0L);
    private static final AtomicLong LAST_TRANSPORT_TRIGGER = new AtomicLong(0L);
    private static volatile long pendingUntil;
    private static volatile Context processContext;
    private static volatile WeakReference<View> startButtonRef = new WeakReference<>(null);
    private static volatile WeakReference<Object> snoreCardRef = new WeakReference<>(null);

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) {
        if ("android".equals(loadPackageParam.packageName)
                && "android".equals(loadPackageParam.processName)) {
            hookColorOsLockScreenInterceptor(loadPackageParam.classLoader);
            return;
        }
        if (!TARGET_PACKAGE.equals(loadPackageParam.packageName)) {
            return;
        }

        XposedBridge.log(TAG + "Module loaded in process " + loadPackageParam.processName);
        hookApplicationContext(loadPackageParam.classLoader);
        if ((TARGET_PACKAGE + ":transport").equals(loadPackageParam.processName)) {
            hookRawSleepSignal(loadPackageParam.classLoader);
            hookWatchSleepModeSignal(loadPackageParam.classLoader);
        } else if (TARGET_PACKAGE.equals(loadPackageParam.processName)) {
            hookSleepSignal(loadPackageParam.classLoader);
            hookSnoreCard(loadPackageParam.classLoader);
            hookSleepHistoryCreate(loadPackageParam.classLoader);
            hookSleepHistoryResume(loadPackageParam.classLoader);
            hookAudioService(loadPackageParam.classLoader);
        }
    }

    private static void hookColorOsLockScreenInterceptor(ClassLoader classLoader) {
        try {
            Class<?> activityRecord = Class.forName(
                    "com.android.server.wm.ActivityRecord", false, classLoader);
            Class<?> keyguardController = Class.forName(
                    "com.android.server.wm.KeyguardController", false, classLoader);

            XposedHelpers.findAndHookMethod(
                    "com.android.server.wm.OplusInterceptLockScreenWindow",
                    classLoader,
                    "keyguardFlagCheck",
                    activityRecord,
                    keyguardController,
                    lockScreenBypassHook("keyguardFlagCheck"));
            XposedHelpers.findAndHookMethod(
                    "com.android.server.wm.OplusInterceptLockScreenWindow",
                    classLoader,
                    "execInterceptWindow",
                    Context.class,
                    activityRecord,
                    boolean.class,
                    lockScreenBypassHook("execInterceptWindow"));
            XposedBridge.log(TAG
                    + "ColorOS lock-screen interceptor hooks installed");
        } catch (Throwable throwable) {
            XposedBridge.log(TAG
                    + "ColorOS lock-screen interceptor hook installation failed");
            XposedBridge.log(throwable);
        }
    }

    private static XC_MethodHook lockScreenBypassHook(String method) {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                Object record = param.args.length > 1
                        && param.args[1] != null
                        && param.args[1].getClass().getName().endsWith("ActivityRecord")
                        ? param.args[1]
                        : param.args[0];
                if (!isHealthSleepActivity(record)) {
                    return;
                }
                param.setResult(false);
                XposedBridge.log(TAG
                        + "ColorOS lock-screen interception bypassed: " + method);
            }
        };
    }

    private static boolean isHealthSleepActivity(Object activityRecord) {
        if (activityRecord == null) {
            return false;
        }
        try {
            Field field = findField(
                    activityRecord.getClass(), "mActivityComponent");
            Object component = field.get(activityRecord);
            String name = String.valueOf(component);
            return name.contains(TARGET_PACKAGE)
                    && name.contains("SleepHistoryActivity");
        } catch (Throwable ignored) {
            String record = String.valueOf(activityRecord);
            return record.contains(TARGET_PACKAGE)
                    && record.contains("SleepHistoryActivity");
        }
    }

    private static Field findField(Class<?> type, String name)
            throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    private static void hookApplicationContext(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    "android.app.Application",
                    classLoader,
                    "attach",
                    Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Context context = (Context) param.args[0];
                            processContext = context == null
                                    ? null
                                    : context.getApplicationContext();
                            if (processContext == null) {
                                processContext = context;
                            }
                        }
                    });
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + "Application context hook installation failed");
            XposedBridge.log(throwable);
        }
    }

    private static void hookRawSleepSignal(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    RAW_SLEEP_PROCESSOR,
                    classLoader,
                    "a",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            XposedBridge.log(TAG
                                    + "Raw watch sleep message reached transport process: state=1");
                        }
                    });
            XposedBridge.log(TAG + "Raw transport sleep hook installed");
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + "Raw transport sleep hook installation failed");
            XposedBridge.log(throwable);
        }
    }

    private static void hookWatchSleepModeSignal(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    SLEEP_MODE_MANAGER,
                    classLoader,
                    "t",
                    SLEEP_MODEL_SETTINGS,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Object settings = param.args[0];
                            boolean startNow = Boolean.TRUE.equals(
                                    XposedHelpers.callMethod(settings, "isStartNow"));
                            boolean syncSleepMode = Boolean.TRUE.equals(
                                    XposedHelpers.callMethod(settings, "isSyncSleepMode"));
                            Object timestamp = XposedHelpers.callMethod(settings, "getTimestamp");
                            XposedBridge.log(TAG
                                    + "Watch sleep-mode setting received: startNow="
                                    + startNow + ", syncSleepMode=" + syncSleepMode
                                    + ", timestamp=" + timestamp);
                            if (startNow) {
                                triggerMainProcessFromTransport("SleepModelSetting.startNow");
                            }
                        }
                    });
            XposedBridge.log(TAG + "Watch sleep-mode setting hook installed");

            XposedHelpers.findAndHookMethod(
                    SLEEP_MODE_MANAGER,
                    classLoader,
                    "r",
                    boolean.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            boolean enabled = (Boolean) param.args[0];
                            int updateTime = (Integer) param.args[1];
                            XposedBridge.log(TAG
                                    + "Phone DND linkage reached: enabled="
                                    + enabled + ", updateTime=" + updateTime);
                            if (enabled) {
                                triggerMainProcessFromTransport("phone DND linkage");
                            }
                        }
                    });
            XposedBridge.log(TAG + "Phone DND linkage hook installed");
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + "Watch sleep-mode hook installation failed");
            XposedBridge.log(throwable);
        }
    }

    private static void triggerMainProcessFromTransport(String source) {
        long now = SystemClock.elapsedRealtime();
        long previous = LAST_TRANSPORT_TRIGGER.get();
        if (now - previous < DUPLICATE_GUARD_MS
                || !LAST_TRANSPORT_TRIGGER.compareAndSet(previous, now)) {
            XposedBridge.log(TAG + "Duplicate transport trigger ignored: " + source);
            return;
        }

        Context context = processContext;
        if (context == null) {
            XposedBridge.log(TAG + "Cannot forward sleep-mode trigger: context unavailable");
            return;
        }

        beginBackgroundStart(context, source);
    }

    private static void hookSleepSignal(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    SLEEP_RECEIVER,
                    classLoader,
                    "onReceive",
                    Context.class,
                    Intent.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Context context = (Context) param.args[0];
                            Intent signal = (Intent) param.args[1];
                            if (!isValidSleepSignal(context, signal)) {
                                return;
                            }
                            beginBackgroundStart(
                                    context, "legacy fall-asleep broadcast");
                        }
                    });
            XposedBridge.log(TAG + "Sleep signal hook installed");
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + "Sleep signal hook installation failed");
            XposedBridge.log(throwable);
        }
    }

    private static void hookSnoreCard(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    SNORE_MONITOR_CARD,
                    classLoader,
                    "p",
                    Context.class,
                    View.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            snoreCardRef = new WeakReference<>(param.thisObject);
                            View cardView = (View) param.args[1];
                            int id = cardView.getResources().getIdentifier(
                                    "switch_snore_detect_start", "id", TARGET_PACKAGE);
                            View startButton = id == 0 ? null : cardView.findViewById(id);
                            if (startButton == null) {
                                XposedBridge.log(TAG
                                        + "Native start button was not found in SnoreMonitorCard");
                                return;
                            }
                            startButtonRef = new WeakReference<>(startButton);
                            XposedBridge.log(TAG + "Native snore start button captured");
                            scheduleNativeButtonClick(600L);
                        }
                    });
            XposedBridge.log(TAG + "SnoreMonitorCard hook installed");
            XposedHelpers.findAndHookMethod(
                    SNORE_MONITOR_CARD,
                    classLoader,
                    "Z",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            XposedBridge.log(TAG
                                    + "Native checkStartRecord business entry reached");
                        }
                    });
            XposedBridge.log(TAG + "Native checkStartRecord trace hook installed");
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + "SnoreMonitorCard hook installation failed");
            XposedBridge.log(throwable);
        }
    }

    private static void hookAudioService(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    "com.heytap.health.sleep.audio.service.AudioRecordService2",
                    classLoader,
                    "onStartCommand",
                    Intent.class,
                    int.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Intent intent = (Intent) param.args[0];
                            int command = intent == null
                                    ? -100
                                    : intent.getIntExtra("AUDIO_RECORD_KEY", -100);
                            XposedBridge.log(TAG
                                    + "AudioRecordService2 onStartCommand command=" + command);
                        }
                    });
            XposedBridge.log(TAG + "AudioRecordService2 trace hook installed");
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + "AudioRecordService2 trace hook installation failed");
            XposedBridge.log(throwable);
        }
    }

    private static void hookSleepHistoryResume(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    SLEEP_HISTORY_ACTIVITY,
                    classLoader,
                    "onResume",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Activity activity = (Activity) param.thisObject;
                            Intent intent = activity.getIntent();
                            if (intent != null
                                    && intent.getBooleanExtra(
                                            EXTRA_FOREGROUND_START, false)) {
                                intent.removeExtra(EXTRA_FOREGROUND_START);
                                XposedBridge.log(TAG
                                        + "Health foreground start flow resumed");
                                startSnoreService(activity, "foreground resumed");
                                Handler handler = new Handler(Looper.getMainLooper());
                                handler.postDelayed(
                                        () -> startSnoreService(
                                                activity, "foreground retry"),
                                        FOREGROUND_RETRY_MS);
                                handler.postDelayed(
                                        () -> returnToHome(activity, false),
                                        RETURN_HOME_MS);
                                handler.postDelayed(
                                        () -> returnToHome(activity, false),
                                        RETURN_HOME_MS + 1500L);
                                handler.postDelayed(
                                        () -> returnToHome(activity, true),
                                        RETURN_HOME_MS + 3500L);
                            }
                            scheduleNativeButtonClick(600L);
                        }
                    });
            XposedBridge.log(TAG + "SleepHistoryActivity resume hook installed");
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + "SleepHistoryActivity resume hook installation failed");
            XposedBridge.log(throwable);
        }
    }

    private static void hookSleepHistoryCreate(ClassLoader classLoader) {
        try {
            XposedHelpers.findAndHookMethod(
                    SLEEP_HISTORY_ACTIVITY,
                    classLoader,
                    "onCreate",
                    Bundle.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Activity activity = (Activity) param.thisObject;
                            Intent intent = activity.getIntent();
                            if (intent == null
                                    || !intent.getBooleanExtra(
                                            EXTRA_FOREGROUND_START, false)) {
                                return;
                            }
                            activity.setShowWhenLocked(true);
                            activity.setTurnScreenOn(true);
                            XposedBridge.log(TAG
                                    + "Lock-screen foreground bridge enabled");
                        }
                    });
            XposedBridge.log(TAG + "SleepHistoryActivity create hook installed");
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + "SleepHistoryActivity create hook installation failed");
            XposedBridge.log(throwable);
        }
    }

    private static void beginBackgroundStart(Context context, String source) {
        long now = SystemClock.elapsedRealtime();
        long previous = LAST_START.get();
        if (now - previous < DUPLICATE_GUARD_MS
                || !LAST_START.compareAndSet(previous, now)) {
            XposedBridge.log(TAG + "Duplicate sleep signal ignored: " + source);
            return;
        }

        XposedBridge.log(TAG + "Sleep trigger accepted from " + source
                + "; restarting Health foreground start flow");
        killHealthMainProcess(context);
        new Handler(Looper.getMainLooper()).postDelayed(
                () -> launchHealthForeground(context),
                MAIN_PROCESS_RESTART_DELAY_MS);
    }

    private static void killHealthMainProcess(Context context) {
        try {
            ActivityManager manager =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager == null || manager.getRunningAppProcesses() == null) {
                return;
            }
            for (ActivityManager.RunningAppProcessInfo process
                    : manager.getRunningAppProcesses()) {
                if (TARGET_PACKAGE.equals(process.processName)
                        && process.pid != Process.myPid()) {
                    XposedBridge.log(TAG
                            + "Killing Health main process pid=" + process.pid);
                    Process.killProcess(process.pid);
                    return;
                }
            }
            XposedBridge.log(TAG + "Health main process was not running");
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + "Unable to kill Health main process");
            XposedBridge.log(throwable);
        }
    }

    private static void launchHealthForeground(Context sourceContext) {
        Context context = sourceContext.getApplicationContext();
        if (context == null) {
            context = sourceContext;
        }
        Intent activity = new Intent();
        activity.setClassName(TARGET_PACKAGE, SLEEP_HISTORY_ACTIVITY);
        activity.putExtra("tab", "0");
        activity.putExtra(EXTRA_FOREGROUND_START, true);
        activity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            context.startActivity(activity);
            XposedBridge.log(TAG
                    + "Health foreground launch dispatched");
        } catch (Throwable throwable) {
            XposedBridge.log(TAG
                    + "Health foreground launch failed");
            XposedBridge.log(throwable);
        }
    }

    private static void startSnoreService(Context sourceContext, String phase) {
        Context context = sourceContext.getApplicationContext();
        if (context == null) {
            context = sourceContext;
        }
        Intent service = createAudioServiceIntent(0);
        try {
            context.startService(service);
            XposedBridge.log(TAG
                    + "Native background snore start command dispatched: " + phase);
        } catch (Throwable throwable) {
            XposedBridge.log(TAG
                    + "Native background snore start command failed: " + phase);
            XposedBridge.log(throwable);
        }
    }

    private static Intent createAudioServiceIntent(int command) {
        Intent service = new Intent();
        service.setClassName(TARGET_PACKAGE, AUDIO_RECORD_SERVICE);
        service.putExtra("AUDIO_RECORD_KEY", command);
        if (command == 0) {
            service.putExtra("KEY_START_ALARM", true);
            service.putExtra("KEY_FROM_WEB_VIEW", false);
        }
        return service;
    }

    private static void returnToHome(Activity activity, boolean finalAttempt) {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            activity.startActivity(home);
            activity.moveTaskToBack(true);
            if (finalAttempt && !activity.isFinishing()) {
                activity.finish();
            }
            XposedBridge.log(TAG + "Recording start dispatched; home enforced"
                    + (finalAttempt ? " (final)" : ""));
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + "Unable to return to home");
            XposedBridge.log(throwable);
        }
    }

    private static boolean isValidSleepSignal(Context context, Intent signal) {
        if (context == null
                || signal == null
                || !ACTION_FALL_ASLEEP.equals(signal.getAction())) {
            return false;
        }
        String target = signal.getPackage();
        if (target != null && !TARGET_PACKAGE.equals(target)) {
            XposedBridge.log(TAG + "Ignored sleep signal for package " + target);
            return false;
        }
        return true;
    }

    private static void openSleepHistory(Context receiverContext) {
        Context context = receiverContext.getApplicationContext();
        if (context == null) {
            context = receiverContext;
        }
        Intent activity = new Intent();
        activity.setClassName(TARGET_PACKAGE, SLEEP_HISTORY_ACTIVITY);
        activity.putExtra("tab", "0");
        activity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            context.startActivity(activity);
        } catch (Throwable throwable) {
            pendingUntil = 0L;
            XposedBridge.log(TAG + "Unable to open SleepHistoryActivity");
            XposedBridge.log(throwable);
        }
    }

    private static void scheduleNativeButtonClick(long delayMs) {
        if (!hasPendingStart()) {
            return;
        }
        View startButton = startButtonRef.get();
        if (startButton == null) {
            XposedBridge.log(TAG + "Waiting for SnoreMonitorCard to create the native button");
            return;
        }
        startButton.postDelayed(HookEntry::clickNativeStartButton, delayMs);
    }

    private static void clickNativeStartButton() {
        if (!hasPendingStart()) {
            return;
        }
        View startButton = startButtonRef.get();
        if (startButton == null) {
            XposedBridge.log(TAG + "Native start button is not ready yet");
            return;
        }
        Object snoreCard = snoreCardRef.get();
        if (snoreCard == null) {
            XposedBridge.log(TAG + "Native SnoreMonitorCard instance is unavailable");
            return;
        }
        try {
            XposedHelpers.callMethod(snoreCard, "Z");
            pendingUntil = 0L;
            XposedBridge.log(TAG
                    + "Native checkStartRecord invoked; original checks and prompt will run");
        } catch (Throwable throwable) {
            XposedBridge.log(TAG + "Native checkStartRecord invocation failed");
            XposedBridge.log(throwable);
        }
    }

    private static boolean hasPendingStart() {
        long until = pendingUntil;
        if (until == 0L) {
            return false;
        }
        if (SystemClock.elapsedRealtime() <= until) {
            return true;
        }
        pendingUntil = 0L;
        XposedBridge.log(TAG + "Pending native start flow expired");
        return false;
    }
}
