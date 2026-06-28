# v2.2.0

## 本次更新

- 收到真实手表睡眠信号后，先终止健康主进程并重新启动，解决前后台状态不一致。
- 健康页面取得前台资格后发送原生 `AudioRecordService2 command=0`，兼容 Android 16 麦克风前台限制。
- 增加仅针对 `com.heytap.health/.sleep.SleepHistoryActivity` 的 ColorOS 锁屏拦截放行。
- 录音启动后自动返回桌面；保留原生通知、停止和数据保存逻辑。

## 实机验证

- OPPO 健康：`6.4.6_cb99e90_260626`
- 包名：`com.heytap.health`
- 场景：健康主进程已清理、手机锁屏
- 结果：睡眠信号、主进程重启、锁屏放行、`command=0` 与麦克风录音均已确认

## 安装要求

在 LSPosed 作用域中同时勾选：

- OPPO 健康
- 系统框架

更新模块或调整作用域后需要重启手机。
