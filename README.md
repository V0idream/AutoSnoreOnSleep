<div align="center">

# 😴 AutoSnoreOnSleep

### OPPO Watch 睡眠模式联动下的 OPPO 健康鼾声监测自动启动模块

**LSPosed Module · OPPO Health · Sleep Mode Linkage · Snore Monitoring · Android 12+**

<p>
  <strong>语言</strong><br/>
  <strong>简体中文</strong>
</p>

<p>
  <strong>导航</strong><br/>
  <a href="#项目简介">项目简介</a> ·
  <a href="#支持版本">支持版本</a> ·
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
![Release](https://img.shields.io/github/v/release/V0idream/AutoSnoreOnSleep?include_prereleases)

<p>
  <a href="https://github.com/V0idream/AutoSnoreOnSleep/releases">
    <img src="https://img.shields.io/github/downloads/V0idream/AutoSnoreOnSleep/total?label=Release%20Downloads&style=for-the-badge" alt="Release Downloads" />
  </a>
</p>

</div>

---

<a id="项目简介"></a>

## 📌 项目简介

**AutoSnoreOnSleep** 是一个用于 OPPO 健康的 LSPosed 模块。当 OPPO Watch 开启睡眠模式并联动手机免打扰时，模块会调用 OPPO 健康原生逻辑，自动启动“手机监测鼾声”。

项目面向个人设备上的自动化研究与自用场景。它不替代 OPPO 健康本身的录音服务、权限管理、前台通知、停止流程和数据保存逻辑，使用前应确认本人设备、本人账号和本人授权环境均符合要求。

<a id="支持版本"></a>

## 🧩 支持版本

* 目标包名：`com.heytap.health`
* 已验证版本：`6.4.6_cb99e90_260626`
* 模块版本：`1.7.0`
* 系统要求：LSPosed；Android 12 及以上

其他 OPPO 健康版本未经验证。目标应用更新或混淆名称变化后，模块可能失效。

<a id="工作方式"></a>

## ⚙️ 工作方式

模块监听 OPPO 健康 `:transport` 进程中的真实睡眠模式链路：

```text
SleepModelSetting.isStartNow=true → SleepModeManager → 手机免打扰联动
```

收到信号后，模块使用 OPPO 健康原生 `AudioRecordService2` 的启动命令开始鼾声监测：

* 不打开 OPPO 健康页面。
* 不依赖 Activity、按钮或可见 View。
* 锁屏时仍可执行。
* 保留应用原生录音服务、前台通知、停止及数据保存逻辑。
* 对重复信号进行去重。

旧版的 `SleepStateChangeNotify state=1` 入睡广播仍作为兼容入口保留。

<a id="使用方法"></a>

## 🚀 使用方法

1. 安装模块。
2. 在 LSPosed 中启用模块，作用域仅勾选“OPPO 健康”。
3. 强制停止并重新打开 OPPO 健康，或重启手机。
4. 确认 OPPO 健康已经获得麦克风权限。
5. 在手表上开启睡眠模式。

模块没有桌面图标。

## 🧾 日志

日志以 `[AutoSnoreOnSleep]` 开头。正常启动时可看到：

```text
Watch sleep-mode setting received: startNow=true
Phone DND linkage reached: enabled=true
Native background snore start command dispatched
AudioRecordService2 onStartCommand command=0
```

<a id="构建"></a>

## 🛠️ 构建

项目使用 Android Gradle Plugin 8.7.3、Gradle 8.10.2、JDK 17 或更高版本以及 Android SDK 35。

```powershell
gradle --offline --no-daemon clean :app:assembleRelease
```

<a id="免责声明"></a>

## ⚠️ 免责声明

本项目仅供个人研究与自动化使用，与 OPPO、欢太或 LSPosed 项目无关。请仅在本人设备、本人账号和已授权环境中使用，并自行遵守所在地区法律法规、平台规则和设备厂商规则。
