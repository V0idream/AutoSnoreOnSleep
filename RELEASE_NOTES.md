# v2.4.0

## 本次修复

- 适配 OPPO 健康 `6.4.7_930e22c_260629`。
- 修复锁屏唤醒后录音仅运行约 5 秒便被 Android 16 静音的问题。
- 在 `AudioRecordService2 command=0` 到达时强制建立 microphone 前台服务。
- 返回桌面后继续保留麦克风访问资格和健康主进程。
- 收到原生停止命令 `command=1` 后移除前台服务通知。

## 根因

旧版实际已经启动 `AudioRecord`，但健康页面返回后台后，系统将录音状态改为 `silenced`。同时服务未保持前台状态，健康主进程可能被回收。

## 信号入口

- 仅监听 `SleepModeManager.t(SleepModelSettings)`。
- 仅在 `SleepModelSettings.isStartNow=true` 时触发。
- 不依赖手机免打扰状态。

## 安装要求

- OPPO 健康：`6.4.7_930e22c_260629`
- LSPosed 作用域：`OPPO 健康`、`系统框架`
- 更新后需要重启手机。
