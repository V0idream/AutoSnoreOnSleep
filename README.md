<div align="center">

# 😴 AutoSnoreOnSleep

### OPPO Watch 入睡后自动启动手机鼾声监测

**LSPosed Module · OPPO Health · Sleep Mode Linkage · Lock-screen Recording · Android 12+**

<p>
  <strong>导航</strong><br/>
  <a href="#项目简介">项目简介</a> ·
  <a href="#兼容性">兼容性</a> ·
  <a href="#工作方式">工作方式</a> ·
  <a href="#使用方法">使用方法</a> ·
  <a href="#构建">构建</a> ·
  <a href="#免责声明">免责声明</a>
</p>

![Platform](https://img.shields.io/badge/Platform-Android-111827)
![Module](https://img.shields.io/badge/Module-LSPosed-0F766E)
![Target](https://img.shields.io/badge/Target-com.heytap.health-1D4ED8)
![Android](https://img.shields.io/badge/Android-12%2B-7C3AED)
![SDK](https://img.shields.io/badge/SDK-35-FF5722)
![Release](https://img.shields.io/github/v/release/V0idream/AutoSnoreOnSleep)

<p>
  <a href="https://github.com/V0idream/AutoSnoreOnSleep/releases/latest">
    <img src="https://img.shields.io/github/downloads/V0idream/AutoSnoreOnSleep/total?label=Release%20Downloads&style=for-the-badge" alt="Release Downloads" />
  </a>
</p>

</div>

---

<a id="项目简介"></a>

## 📌 项目简介

**AutoSnoreOnSleep** 是一个用于 OPPO 健康的 LSPosed 模块。当 OPPO Watch 开启睡眠模式并联动手机免打扰时，模块自动启动“手机监测鼾声”。

即使健康主进程已被清理或手机处于锁屏状态，模块也会重新启动健康、取得麦克风前台资格、执行原生录音命令，然后返回桌面。

<a id="兼容性"></a>

## 🧩 兼容性

- 已验证 OPPO 健康：`6.4.7_930e22c_260629`
- 目标包名：`com.heytap.health`
- 模块版本：`2.4.0`
- 系统要求：Android 12 及以上、LSPosed
- LSPosed 作用域：`OPPO 健康`、`系统框架`

锁屏路径在当前测试设备的 Android 16 / ColorOS 系统上完成实机验证。其他 OPPO 健康或 ColorOS 版本未经验证，应用或系统更新后内部类名可能变化。

<a id="工作方式"></a>

## ⚙️ 工作方式

模块直接监听 OPPO 健康 `:transport` 进程中的真实睡眠状态，不再使用手机免打扰变化作为触发条件：

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
5. 将 `AudioRecordService2` 强制提升为 microphone 前台服务，避免返回桌面后被 Android 16 静音或回收。
6. 进行一次幂等重试，随后自动返回桌面。

模块保留健康原生录音服务、通知、停止与数据保存逻辑，并对重复睡眠信号去重。

<a id="使用方法"></a>

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

<a id="构建"></a>

## 🛠️ 构建

需要 Android Gradle Plugin 8.7.3、Gradle 8.10.2、JDK 17 或更高版本以及 Android SDK 35。

```powershell
gradle --offline --no-daemon clean :app:assembleRelease
```

输出文件：

```text
app/build/outputs/apk/release/app-release.apk
```

<a id="免责声明"></a>

## ⚠️ 免责声明

本项目仅供个人研究与自动化使用，与 OPPO、欢太或 LSPosed 项目无关。系统框架 Hook 严格限制为 OPPO 健康睡眠页面，但安装前仍应理解其系统级作用范围。

---

## ☕ 支持项目 / Support

如果这个项目对你有帮助，欢迎点一个 Star。  
若愿意进一步支持，也可以通过赞赏码请作者续一口 AI 订阅。

众所周知，风水宝地土耳其并非久居之所；账号颠沛流离，订阅价格又日渐高昂，维护开源项目实属不易。

你的赞赏将带来：作者诚挚的感谢、更快更稳定的更新动力，以及对合理功能建议的优先考虑。

不赞赏也完全不影响项目使用、Issue 交流和功能建议。只是如果这个项目真的帮到了你——你真的忍心看作者独自面对订阅账单吗 😢

👉 [查看赞赏方式](./docs/support.md)
