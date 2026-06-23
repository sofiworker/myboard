# 阶段 07：Subtype 动态生成、权限与 i18n

> 顺序：07  
> 目标：实现动态 subtype 生成、权限集中管理、i18n 字符串补齐。  
> 依据：`docs/android-bridge.md` 第 4 节、`docs/core.md` 第 7 节

## 1. 预期目标

本阶段结束时：

- `SubtypeBridge` 真实实现，能动态生成和管理 `InputMethodSubtype`。
- `PermissionGateway` 真实实现，能集中管理权限请求。
- 所有 UI 字符串有中英文翻译。
- 旧 `method.xml`（已在阶段 06 删除）的 subtype 由动态生成替代。
- 旧 `assets/subtypes/` 目录已在阶段 04 删除，subtype 由动态生成替代。

**依赖说明**：本阶段使用阶段 02 的 `OrthogonalRegistry`（获取 Manifest 中的 subtype 定义）和阶段 06 的 `MyBoardImeService.kt`（注册 subtype）。

## 2. 前置依赖

- 阶段 01 的 `SubtypeBridge`、`PermissionGateway` 契约已定义。
- 阶段 02 的 `OrthogonalRegistry` 可查询已注册 Manifest。
- 阶段 06 的 `MyBoardImeService.kt` 可注册 subtype。

## 3. 实施步骤

### 7.1 `SubtypeBridge` 真实实现

做什么：

- 实现阶段 01 定义的 `SubtypeBridge` 接口。
- `onCurrentSubtypeChanged(subtype: InputMethodSubtype)`：系统 subtype 变化时更新 `KeyboardContext`。
- `syncOutbound(state: OrthogonalState)`：状态变化时同步到系统 subtype。
- `switchToNext()`：切换到下一个启用的 subtype。
- 动态生成 `InputMethodSubtype`：
  - 从 `OrthogonalRegistry` 获取所有已注册 Manifest 的 subtype 定义。
  - 使用 `InputMethodSubtypeBuilder` 构建 subtype。
  - 调用 `InputMethodManager.setAdditionalInputMethodSubtypes` 注册。
- **直接删除**旧 `assets/subtypes/` 目录（如阶段 04 未删除）。

测试：

- 动态生成 subtype 数量与 Manifest 一致。
- subtype 切换时 `KeyboardContext` 更新。
- 旧 `generated.json` 引用已全部清除，`assets/subtypes/` 目录不存在。

预期目标：

- subtype 完全由 Manifest 驱动，无硬编码。

性能：

- subtype 生成在 IME 启动时一次性完成。

### 7.2 `PermissionGateway` 真实实现

做什么：

- 实现阶段 01 定义的 `PermissionGateway` 接口。
- `isGranted(permission: String): Boolean`：检查权限。
- `requestMicrophone(callback: (granted: Boolean) -> Unit)`：请求麦克风权限。
- `requestFileImport(callback: (granted: Boolean) -> Unit)`：请求文件导入权限。
- 使用 `ActivityCompat.requestPermissions` 和 `ContextCompat.checkSelfPermission`。

测试：

- 权限已授予时 `isGranted` 返回 `true`。
- 权限未授予时 `requestMicrophone` 触发系统对话框。

预期目标：

- 权限请求集中管理，不散落在各处。

性能：

- 无影响。

### 7.3 i18n 字符串补齐

做什么：

- 补齐 `values/strings.xml` 和 `values-zh-rCN/strings.xml`。
- 覆盖所有 UI 字符串：
  - 设置项名称和描述。
  - subtype 显示名。
  - 面板标签。
  - 错误提示。
  - 权限说明。
- Manifest 中的 `subtype.labelKey` 必须在字符串资源中存在。
- 补齐 `OrthogonalRegistry` 中 subtype `labelKey` 存在性校验。

测试：

- 所有字符串 key 在中英文资源中都存在。
- Manifest `labelKey` 校验通过。

预期目标：

- 所有 UI 文本可本地化。

性能：

- 无影响。

### 7.4 补齐 `OrthogonalRegistry` labelKey 存在性校验

做什么：

- 在 `OrthogonalRegistry` 注册 Manifest 时，校验 `subtype.labelKey` 在字符串资源中存在。
- 不存在时注册失败。

测试：

- `labelKey` 存在时注册成功。
- `labelKey` 不存在时注册失败。

预期目标：

- Manifest 注册后，subtype 显示名一定存在。

性能：

- 校验在注册期一次性完成。

## 4. 阶段验收

运行：

```bash
./gradlew test
./gradlew assembleDebug
```

静态检查：

```bash
rg "generated.json" app/src/main
ls app/src/main/assets/subtypes 2>/dev/null  # 应不存在
```

验收标准：

- `SubtypeBridge` 真实实现，动态生成 subtype。
- `PermissionGateway` 真实实现，集中管理权限。
- i18n 字符串齐全，中英文一致。
- `assets/subtypes/` 目录已删除。
- `OrthogonalRegistry` labelKey 存在性校验补齐。
