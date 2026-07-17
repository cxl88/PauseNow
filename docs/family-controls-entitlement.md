# Family Controls 权限申请与后台配置

## 1. 需要创建的标识

在 Apple Developer 后台创建或确认：

1. `com.pausenow.app`
2. `com.pausenow.app.device-activity-monitor`
3. `com.pausenow.app.shield-configuration`
4. `com.pausenow.app.shield-action`
5. App Group：`group.com.pausenow.shared`

为主 App 与三个 Screen Time 扩展启用 Family Controls。主 App、Device Activity Monitor 与 Shield Action 加入同一个 App Group。修改能力后重新生成开发和分发 Provisioning Profile。

## 2. 分发权限

开发真机验证通过不等于可以提交 App Store。账户持有人应尽早按 Apple 的 [Requesting the Family Controls entitlement](https://developer.apple.com/documentation/familycontrols/requesting-the-family-controls-entitlement) 为应用和相关扩展申请分发用途权限，并保存申请编号、提交日期和结果。

建议用途说明：

> PauseNow（停一下）是一款面向成年用户的自我数字健康工具。用户在自己的设备上主动授予 Screen Time 访问权限，并通过 Apple 提供的 FamilyActivityPicker 自行选择希望减少使用的应用。产品使用 DeviceActivity 在用户设定的时段或累计使用阈值到达时触发 ManagedSettings Shield，并允许用户主动撤销权限。应用不读取第三方 App 内容，不获取所选 App 的名称或 Bundle ID，不监控聊天、位置或网络流量，也不将屏幕使用数据出售给第三方。首版选择和规则均保存在本地 App Group。

## 3. 提交检查

- App 用途与权限申请说明一致；
- 不把成人自我管理虚构成儿童家长控制；
- 商店文案不承诺单独控制微信视频号；
- App Review Notes 写明授权、选择测试应用和触发 1 分钟阈值的步骤；
- 给审核人员提供完整演示视频；
- 隐私政策说明本地存储、撤权和删除方式；
- 数字功能收费时使用 StoreKit，而不是 App 内微信/支付宝绕过内购。

## 4. 申请跟踪

| 项目 | 状态 | 日期 | 负责人 | 备注 |
|---|---|---|---|---|
| 4 个 App ID 创建 | 待处理 |  | 账户持有人 |  |
| App Group 创建 | 待处理 |  | 账户持有人 |  |
| Development 能力与 Profile | 待处理 |  | iOS 开发 |  |
| Distribution entitlement 申请 | 待处理 |  | 账户持有人 |  |
| Apple 回复 | 待处理 |  | 账户持有人 |  |

