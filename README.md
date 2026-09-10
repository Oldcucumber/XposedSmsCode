# XposedSmsCode

## legacy-APIupdate fork

This repository is maintained by **Oldcucumber** as a compatibility-focused fork. It targets **libxposed API 102** while preserving the original interface, settings, parser behavior, and feature workflow. It adds JSON configuration backup/restore, retained-data migration, bounded SMS processing, safer notification intents, Android 15/16 permission adaptation, sensitive clipboard marking, and configuration-only system backup.

Source and releases: https://github.com/Oldcucumber/XposedSmsCode

Current release: **2.5.3** — conservative default extraction improvements, API 102 scope/initialization diagnostics, and Release shrinking fixes. [Changes and verification scope](docs/release-2.5.3.md) · [HyperOS troubleshooting](docs/hyperos-compatibility.md).

Thanks to original author **tianma8023**. The original attribution and GPLv3 license are retained. This fork does not request donations.

![Total Downloads](https://img.shields.io/github/downloads/Oldcucumber/XposedSmsCode/total) ![Total Stars](https://img.shields.io/github/stars/Oldcucumber/XposedSmsCode?style=social) [![Latest Release](https://img.shields.io/github/v/release/Oldcucumber/XposedSmsCode?label=Latest%20Release)](https://github.com/Oldcucumber/XposedSmsCode/releases)

![Star History Chart](https://api.star-history.com/svg?repos=Oldcucumber/XposedSmsCode&type=Date)

An Xposed module which can recognize, parse SMS code and copy it to clipboard when a new message arrives. It can also input SMS code automatically.

[中文版说明](./README-CN.md)

# Screenshots
<img src="./art/en/01.png" width="180"/><img src="./art/en/02.png" width="180"/><img src="./art/en/03.png" width="180"/>

# Download
- [GitHub Releases](https://github.com/Oldcucumber/XposedSmsCode/releases)
- LSPosed module: install the APK from GitHub Releases and enable it in LSPosed
- ~~[Coolapk](https://www.coolapk.com/apk/com.github.tianma8023.xposed.smscode)~~
- ~~[Xposed Repository](http://repo.xposed.info/module/com.github.tianma8023.xposed.smscode)~~

# Usage
1. Root your Android 8.0+ device and install a framework implementing libxposed API 102.
2. Enable this module for system framework (`system`) and phone (`com.android.phone`), reboot, and open the module app to sync settings.
3. Enjoy it!

Welcome any feedbacks.

# Attention
- **This module is suitable for AOSP ROM, it may not work well on other 3rd-party Rom.**
- **Compatibility: Requires Android 8.0+ (API ≥ 26) and libxposed API 102. HyperOS 4 still requires device verification.**
- **Legacy Xposed, EdXposed and TaiChi are not supported by this fork.**
- **Read the FAQ in app first if you encounter any problems.**

# Features
- Copy verification code to clipboard when a new message arrives.
- Show toast when the verification code is copied.
- Show notification when verification SMS parsed.
- Mark verification SMS as read(experimental).
- Delete verification SMS when it's extracted successfully(experimental).
- Block verification SMS if it's extracted successfully.
- Custom keywords about verification code message (regular expressions allowed).
- Support the SMS code match rules customization, importation and exportation.
- Auto-input SMS code.
- Various theme color to choose.

# Release Log
[Release Logs](/LOG-EN.md)

# Thanks To
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

# License
All code is licensed under [GPLv3](https://www.gnu.org/licenses/gpl-3.0.txt) 
