# AutoSnoreOnSleep

一个用于 OPPO 健康的 LSPosed 模块。当 OPPO Watch 开启睡眠模式并联动手机免打扰时，自动在后台启动“手机监测鼾声”。

## 支持版本

- 目标包名：`com.heytap.health`
- 已验证版本：`6.4.6_cb99e90_260626`
- 模块版本：`1.8.0`
- 系统要求：LSPosed；Android 12 及以上

其他 OPPO 健康版本未经验证。目标应用更新或混淆名称变化后，模块可能失效。

## 工作方式

模块监听 OPPO 健康 `:transport` 进程中的真实睡眠模式链路：

`SleepModelSetting.isStartNow=true` → `SleepModeManager` → 手机免打扰联动

收到信号后，模块使用 OPPO 健康原生 `AudioRecordService2` 的启动命令在后台开始鼾声监测：

- 不打开 OPPO 健康页面
- 不依赖 Activity、按钮或可见 View
- 锁屏时仍可执行
- 即使 OPPO 健康主进程已被系统清理，也会先在后台预热主进程，再延迟启动监测
- 启动命令会进行一次幂等重试，避免冷启动初始化尚未完成
- 保留应用原生录音服务、前台通知、停止及数据保存逻辑
- 对重复信号进行去重

旧版的 `SleepStateChangeNotify state=1` 入睡广播仍作为兼容入口保留。

## 使用方法

1. 安装模块。
2. 在 LSPosed 中启用模块，作用域仅勾选“OPPO 健康”。
3. 强制停止并重新打开 OPPO 健康，或重启手机。
4. 确认 OPPO 健康已经获得麦克风权限。
5. 在手表上开启睡眠模式。

模块没有桌面图标。

## 日志

日志以 `[AutoSnoreOnSleep]` 开头。正常启动时可看到：

```text
Watch sleep-mode setting received: startNow=true
Phone DND linkage reached: enabled=true
Native background snore start command dispatched
AudioRecordService2 onStartCommand command=0
```

## 构建

项目使用 Android Gradle Plugin 8.7.3、Gradle 8.10.2、JDK 17 或更高版本以及 Android SDK 35。

```powershell
gradle --offline --no-daemon clean :app:assembleRelease
```

## 免责声明

本项目仅供个人研究与自动化使用，与 OPPO、欢太或 LSPosed 项目无关。
