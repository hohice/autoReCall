# 自动接听（AutoAnswer）

面向 Android 15（API 35）及以上版本的电话自动接听应用，支持黑白名单管理与自动接听记录。

## 功能特性

- **自动接听**：来电命中规则时自动接听，并在 3 秒后自动挂断
- **黑白名单两种模式**
  - 黑名单模式：仅黑名单号码自动接听
  - 白名单模式：非黑名单号码自动接听
- **通话监听开关**：一键启动 / 停止 `phoneCall` 类型前台服务
- **接听记录**：保存并查看历史自动接听记录
- **权限引导**：启动时检查权限，缺失时请求，被永久拒绝时引导至系统设置

## 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 35
- 最低支持 Android 8.0（minSdk 26）

## 构建

```bash
./gradlew assembleDebug      # 构建 Debug 版本
./gradlew assembleRelease    # 构建 Release 版本
./gradlew test               # 运行测试
```

构建产物位于 `app/build/outputs/apk/`。

> 若系统默认 Java 版本高于 17（Gradle 8.9 不支持更高版本），请显式指定 JDK 17，例如：
> ```bash
> JAVA_HOME="/path/to/jdk-17" ./gradlew assembleDebug
> ```

## 使用方式

1. 安装并打开应用，按提示授予全部电话相关权限。
2. 打开「启用通话监听（前台服务）」开关。
3. 选择「自动接听模式」：黑名单模式或白名单模式。
4. 在黑名单号码列表中添加 / 删除号码。
5. 通过「查看接听记录」进入记录页查看历史接听。

## Android 15 关键配置

### 必需权限

```xml
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.READ_PHONE_NUMBERS" />
<uses-permission android:name="android.permission.ANSWER_PHONE_CALLS" />
<uses-permission android:name="android.permission.READ_CALL_LOG" />
<uses-permission android:name="android.permission.READ_CONTACTS" />
<uses-permission android:name="android.permission.MANAGE_OWN_CALLS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_PHONE_CALL" />
```

说明：

- `MANAGE_OWN_CALLS`：targetSdk 35 上启动 `phoneCall` 类型前台服务除 `FOREGROUND_SERVICE_PHONE_CALL` 外还需此权限（或 DIALER 角色），否则 `startForeground` 抛 `SecurityException`
- `READ_CALL_LOG`：API 29+ 非默认拨号器应用获取来电号码的必需权限，缺失时回调中号码为 null / 空串
- `answerRingingCall()` 在 API 35 SDK 中已被移除，代码中使用等价的 `acceptRingingCall()`

### 前台服务

```xml
<service
    android:name=".CallService"
    android:foregroundServiceType="phoneCall" />
```

## 实现逻辑

### 自动接听流程

```
来电 → CallService.onCallStateChange(RINGING)
    → CallManager.handleIncomingCall()
    → 检查号码是否在名单中
    → 如果符合条件：answerCall() → delay(3000ms) → hangUpCall()
    → callLogRepository.addEntry() 记录接听
```

### 关键类关系

```
MainActivity
    ├── PermissionManager (权限检查)
    ├── CallService (启动/停止监听)
    └── SettingsViewModel
        ├── PreferencesRepository (模式/开关状态)
        └── BlacklistRepository (黑名单数据)

CallLogActivity
    └── CallLogRepository (接听记录数据)
```

## 项目结构

### 入口文件

| 文件 | 说明 |
|------|------|
| `app/src/main/AndroidManifest.xml` | 应用清单，配置权限和服务 |
| `app/src/main/java/com/example/autoanswer/MainActivity.kt` | 主界面入口 |
| `app/src/main/java/com/example/autoanswer/CallService.kt` | 通话监听前台服务 |

### 核心模块

| 文件 | 职责 |
|------|------|
| `PermissionManager.kt` | 权限检查和请求 |
| `CallManager.kt` | 通话控制（接听/挂断） |
| `CallService.kt` | 前台服务和电话状态监听 |
| `SettingsViewModel.kt` | 设置和状态管理 |
| `PreferencesRepository.kt` | 偏好设置存储 |
| `BlacklistRepository.kt` | 黑名单数据层 |
| `BlacklistDatabase.kt` | Room 数据库 |
| `BlacklistDao.kt` | 数据访问对象 |
| `BlacklistEntry.kt` | 黑名单实体 |
| `calllog/CallLogEntry.kt` | 接听记录实体 |
| `calllog/CallLogDao.kt` | 接听记录 DAO |
| `calllog/CallLogRepository.kt` | 接听记录数据层 |
| `calllog/CallLogActivity.kt` | 接听记录查看界面 |

## 兼容性说明

- `compileSdk = 35`、`targetSdk = 35`
- API 31+ 使用 `TelephonyCallback`，同时保留 `PhoneStateListener` 用于获取来电号码（`TelephonyCallback` 的回调不带号码）；两个监听源的事件在 `CallService` 中延迟合并
- 前台服务类型必须声明为 `phoneCall`，且需 `FOREGROUND_SERVICE_PHONE_CALL` + `MANAGE_OWN_CALLS` 权限

## 已知限制

- 某些 OEM 厂商可能限制自动接听功能
- 需在真实设备上做最终验证；模拟器可通过 console 的 `gsm call <号码>` 命令模拟来电，基础链路已验证

## 测试要点

1. 权限请求和引导流程
2. 黑名单模式：黑名单号码自动接听，其他号码手动处理
3. 白名单模式：非黑名单号码自动接听，黑名单号码手动处理
4. 监听开关生效
5. 3 秒自动挂断功能
6. 接听记录正确保存
7. 接听记录查看页面正常显示
8. 前台服务稳定性
