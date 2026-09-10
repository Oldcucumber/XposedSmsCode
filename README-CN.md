# XposedSmsCode

## legacy-APIupdate fork

本仓库由 **Oldcucumber** 维护，作为原项目的兼容性 fork，面向 **libxposed API 102**，尽量保留原界面和操作流程。提供 JSON 配置备份恢复、旧数据保留迁移、短信解析超时保护及 Android 15/16 权限适配。

源码与发布：https://github.com/Oldcucumber/XposedSmsCode

致谢原作者 **tianma8023**，保留原作者署名与 GPLv3 许可证，本 fork 无需捐赠。

当前版本 **2.5.3**：优化默认提取规则，汇总 API 102 作用域、初始化与日志改进以及 Release 混淆修复。详见 [变更与验证范围](docs/release-2.5.3.md) 和 [澎湃兼容性排查](docs/hyperos-compatibility.md)。

![Total Downloads](https://img.shields.io/github/downloads/Oldcucumber/XposedSmsCode/total) ![Total Stars](https://img.shields.io/github/stars/Oldcucumber/XposedSmsCode?style=social) [![Latest Release](https://img.shields.io/github/v/release/Oldcucumber/XposedSmsCode?label=Latest%20Release)](https://github.com/Oldcucumber/XposedSmsCode/releases)

![Star History Chart](https://api.star-history.com/svg?repos=Oldcucumber/XposedSmsCode&type=Date)

识别短信验证码的Xposed模块，并将验证码拷贝到剪切板，亦可以自动输入验证码。

[English README](./README.md)

# 应用截图
<img src="./art/cn/01.png" width="180"/><img src="./art/cn/02.png" width="180"/><img src="./art/cn/03.png" width="180"/>

# 下载
下载地址:
- [GitHub Releases](https://github.com/Oldcucumber/XposedSmsCode/releases)
- 本 fork 请从上方 GitHub Releases 下载；原模块仓库不提供此 fork 更新。
- ~~[酷安](https://www.coolapk.com/apk/com.github.tianma8023.xposed.smscode)~~
- ~~[Xposed仓库](http://repo.xposed.info/module/com.github.tianma8023.xposed.smscode)~~

# 使用
1. 在 Android 8.0 及以上设备取得 Root，安装实现 libxposed API 102 的框架；
2. 启用模块，确认系统框架（`system`）与电话（`com.android.phone`）作用域，重启后打开模块应用同步设置；
3. Enjoy it！

欢迎反馈，欢迎提出意见或建议。

# 注意
- **此模块适用于偏原生的系统，其他第三方定制Rom可能不适用。**
- **兼容性：需要 Android 8.0 及以上（API ≥ 26）及 libxposed API 102；澎湃 OS 4 仍待真机验证。**
- **此 fork 不支持旧版 Xposed、EdXposed 或太极。**
- **遇到问题请先阅读模块中的"常见问题"**

# 功能
- 收到验证码短信后将验证码复制到系统剪贴板
- 收到验证码时显示Toast
- 收到验证码时显示通知
- 将验证码短信标记为已读（实验性）
- 验证码提取成功后，删除验证码短信（实验性）
- 拦截验证码短信
- 自定义验证码短信关键字（正则表达式）
- 自定义验证码匹配规则，并支持规则导入导出
- 自动输入验证码
- 主题换肤

# 更新日志
[更新日志](/LOG-CN.md)

# 感谢
- [Xposed](https://github.com/rovo89/Xposed)
- [NekoSMS](https://github.com/apsun/NekoSMS)
- [ButterKnife](https://github.com/JakeWharton/butterknife)
- [Material Dialogs](https://github.com/afollestad/material-dialogs)
- [EventBus](https://github.com/greenrobot/EventBus)
- [GreenDao](https://github.com/greenrobot/greenDAO)
- [GreenDaoUpgradeHelper](https://github.com/yuweiguocn/GreenDaoUpgradeHelper)
- [Gson](https://github.com/google/gson)
- [dagger](https://github.com/google/dagger)
- [rxjava](https://github.com/ReactiveX/RxJava)
- [rxandroid](https://github.com/ReactiveX/RxAndroid)
- [Cyanea](https://github.com/jaredrummler/Cyanea)


# 协议
所有的源码均遵循 [GPLv3](https://www.gnu.org/licenses/gpl-3.0.txt) 协议
