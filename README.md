# AutoSnoreOnSleep

### OPPO Watch 入睡后自动启动手机鼾声监测

Android · LSPosed · OPPO 健康 · 睡眠自动化

[![Release](https://img.shields.io/github/v/release/V0idream/AutoSnoreOnSleep)](https://github.com/V0idream/AutoSnoreOnSleep/releases/latest)
[![Android](https://img.shields.io/badge/Android-12%2B-3DDC84?logo=android&logoColor=white)](#兼容性)
[![License](https://img.shields.io/github/license/V0idream/AutoSnoreOnSleep)](LICENSE)

---

## 📖 项目简介

这是一个用于 OPPO 健康的 LSPosed 模块。当 OPPO Watch 开启睡眠模式并联动手机免打扰时，模块自动启动“手机监测鼾声”。

即使健康主进程已被清理或手机处于锁屏状态，模块也会重新启动健康、取得麦克风前台资格、执行原生录音命令，然后返回桌面。

## ✅ 兼容性

- 已验证 OPPO 健康：`6.4.6_cb99e90_260626`
- 目标包名：`com.heytap.health`
- 模块版本：`2.2.0`
- 系统要求：Android 12 及以上、LSPosed
- LSPosed 作用域：`OPPO 健康`、`系统框架`

锁屏路径在当前测试设备的 Android 16 / ColorOS 系统上完成实机验证。其他 OPPO 健康或 ColorOS 版本未经验证，应用或系统更新后内部类名可能变化。

## ⚙️ 工作方式

模块监听 OPPO 健康 `:transport` 进程中的真实睡眠模式链路：

```text
SleepModelSetting.isStartNow=true
→ SleepModeManager
→ 手机免打扰联动
```

收到信号后：

1. 终止残留的健康主进程，保留接收信号的 `:transport` 进程。
2. 重新启动健康睡眠页面。
3. 锁屏时仅对白名单组件 `SleepHistoryActivity` 放行 ColorOS 锁屏显示拦截。
4. 页面取得前台资格后发送原生 `AudioRecordService2 command=0`。
5. 进行一次幂等重试，随后自动返回桌面。

模块保留健康原生录音服务、通知、停止与数据保存逻辑，并对重复睡眠信号去重。

## 🚀 使用方法

1. 从 [Releases](https://github.com/V0idream/AutoSnoreOnSleep/releases/latest) 下载并安装模块。
2. 在 LSPosed 中启用模块，作用域勾选“OPPO 健康”和“系统框架”。
3. 重启手机。
4. 确认 OPPO 健康已获得麦克风权限。
5. 在手表上开启睡眠模式。

模块没有桌面图标。

## 🧾 日志

成功启动时可看到：

```text
Watch sleep-mode setting received: startNow=true
Killing Health main process
ColorOS lock-screen interception bypassed: keyguardFlagCheck
AudioRecordService2 onStartCommand command=0
Recording start dispatched; home enforced
```

## 🛠️ 构建

需要 Android Gradle Plugin 8.7.3、Gradle 8.10.2、JDK 17 或更高版本以及 Android SDK 35。

```powershell
gradle --offline --no-daemon clean :app:assembleRelease
```

输出文件：

```text
app/build/outputs/apk/release/app-release.apk
```

## ⚠️ 免责声明

本项目仅供个人研究与自动化使用，与 OPPO、欢太或 LSPosed 项目无关。系统框架 Hook 严格限制为 OPPO 健康睡眠页面，但安装前仍应理解其系统级作用范围。
