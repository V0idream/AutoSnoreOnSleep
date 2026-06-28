# v2.3.0

## 本次更新

- 唯一主触发源改为手表下发的 `SleepModelSettings.isStartNow=true`。
- 删除 `SleepModeManager.r(...)` 的手机免打扰联动触发。
- 删除旧版 `action_broadcast_device_fall_asleep` 兼容触发。
- 手动开启手机免打扰不再启动鼾声监测，避免误触发。
- 保留主进程强制重启、锁屏放行、原生 `AudioRecordService2 command=0` 和自动返回桌面逻辑。

## 实机依据

- OPPO 健康：`6.4.6_cb99e90_260626`
- 包名：`com.heytap.health`
- 睡眠状态入口：`SleepModeManager.t(SleepModelSettings)`
- 生效条件：`isStartNow=true`

## 安装要求

在 LSPosed 作用域中同时勾选：

- OPPO 健康
- 系统框架

更新模块或调整作用域后需要重启手机。
