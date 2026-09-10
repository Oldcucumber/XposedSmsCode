# 澎湃 OS 4 兼容性排查（2026-09-10）

本轮没有澎湃真机及故障用户日志，不能宣称已经修复或完成 OS 4 适配。
公开反馈包含 Android 17 / SDK 37 的 OS 4 设备；必须记录具体机型、ROM 完整版本、
Android SDK、框架分支和版本，不能把所有澎湃版本视为同一实现。

## 有依据的修正

- 现代 LSPosed 将 `system`（system_server）与 `android` 区分。补充 `system` 到作用域，
  保留原来的 `android` 和 `com.android.phone`，使现有权限 Hook 能在标准现代框架中加载。
- 模块日志改用 API 102 的 `log(priority, tag, message)`。独立应用不加载 compileOnly API，
  由模块入口安装日志桥接；框架日志异常不会中断短信处理。
- 仍使用原有 `InboundSmsHandler.dispatchIntent` 匹配与短信解析流程。
  仅在构造器初始化缺失时从处理器 `mContext` 恢复上下文；通知渠道和复制接收器分别初始化，
  失败后可重试。未知签名、上下文不可用、配置缺失或解析失败时放行原短信。
- 增加系统构建信息、Hook 安装签名、配置加载状态日志，不增加短信正文或验证码日志。

未放宽歧义方法匹配，未新增短信拦截入口，未更改权限白名单、剪贴板限制、
解锁检查、界面或解析规则。添加作用域会使原本未加载的权限 Hook 开始运行，
仍需在目标 ROM 上确认；不能承诺零回归。

## 用户复测

1. 安装同签名更新，在框架中确认系统框架（system）和电话（com.android.phone）作用域，重启设备。
2. 解锁并打开模块应用，使配置发布到框架，确认启用开关，再接收一条验证码短信。
3. 分别记录解析、复制、通知、记录、拦截的实际结果；“已激活”只说明模块应用连接到了框架，
   不代表电话进程 Hook 和权限 Hook 都已成功。
4. 导出框架模块日志。关注 `System server permission hooks initializing`、
   `SMS dispatch hook installed`、`Remote configuration loaded`，及相邻错误。
   分享前删除短信正文、验证码和其他个人信息。
5. 在原有可用设备上复测同样功能，尤其是不启用拦截时原短信正常送达。

## 公开资料与证据边界

- [libxposed API 102 源码：日志接口](https://github.com/libxposed/api/blob/102.0.0/api/src/main/java/io/github/libxposed/api/XposedInterface.java)
- [API 102 生命周期及 classloader 说明](https://github.com/libxposed/api/blob/102.0.0/api/src/main/java/io/github/libxposed/api/XposedModuleInterface.java)
- [LSPosed 1.9.1：现代 API 的 system/android 区分](https://github.com/LSPosed/LSPosed/releases/tag/v1.9.1)
- [LSPosed ConfigManager：system_server 查询 system 作用域](https://github.com/LSPosed/LSPosed/blob/master/daemon/src/main/java/org/lsposed/lspd/service/ConfigManager.java)
- [AOSP InboundSmsHandler](https://android.googlesource.com/platform/frameworks/opt/telephony/+/refs/heads/main/src/java/com/android/internal/telephony/InboundSmsHandler.java)：
  检查时的主线仍有 `mContext`，分发签名含 Intent 和一个 BroadcastReceiver 子类。
  这是 AOSP 参考，不能据此证明小米 ROM 完全一致。
- [MiCTS OS 4 / Android 17 兼容性 PR](https://github.com/parallelcc/MiCTS/pull/184)：
  有小米 Android 17 / SDK 37 的实测描述，但处理的是搜索接口，不能当作短信问题根因。
- [原项目澎湃锁屏复制反馈](https://github.com/tianma8023/XposedSmsCode/issues/73)：
  OS 1 / Android 14 的历史个案，不能直接外推 OS 4。

现阶段没有公开证据足以确认本次用户故障来自小米特有的短信分发改动。
如仅复制/通知失败，优先检查权限 Hook、系统剪贴板及通知限制；若完全无反应，
优先检查电话进程注入、Hook 签名与远程配置日志。不要为了试错直接扩大权限或拦截范围。
