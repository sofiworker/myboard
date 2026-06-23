# 阶段 05：引擎与 Pipeline 真实实现

> 顺序：05  
> 目标：实现 `InputPipeline`、编码器、字典、候选策略、显示策略，替换所有 stub Registry。  
> 依据：`docs/engine.md`、`docs/core.md` 第 5 节

## 1. 预期目标

本阶段结束时：

- `InputPipeline` 真实实现，能处理 `InputAction` 并产生 `EngineResult`。
- `EngineRegistry`、`DictionaryRegistry`、`EncoderRegistry`、`CandidatePolicyRegistry`、`DisplayPolicyRegistry` 真实实现，替换阶段 01 的 stub。
- `EngineResourceResolver` 真实实现，替换阶段 01 的 stub。
- 内置引擎（`direct`、`table_composing`、`transliteration`）可创建 session。
- `OrthogonalRegistry` 存在性校验补齐，Manifest 注册时校验 `engineId`/`layoutId`/`encoderId`/`candidatePolicy`/`displayPolicy` 是否真实存在。
- 旧引擎代码（`InputEngine.kt`、`DirectInputEngine.kt`、`CompositionInputEngine.kt`）已直接删除。
- 旧字典代码（`TrieDict.kt`、`SuggestionEngine.kt`、`DictionaryImporter.kt`）已直接删除。

**依赖说明**：本阶段使用阶段 04 的真实 `LayoutRegistry`（布局存在性校验需要）和 `FakeInputConnectionGateway`（Pipeline 测试不依赖真实 Android `InputConnection`）。

## 2. 前置依赖

- 阶段 01 的 `InputPipeline`、`EngineRegistry`、`DictionaryRegistry`、`EncoderRegistry`、`CandidatePolicyRegistry`、`DisplayPolicyRegistry`、`EngineResourceResolver` 契约已定义，stub 已提供。
- 阶段 02 的 `OrthogonalRegistry` 已注入 stub Registry。
- 阶段 04 的 `LayoutRegistry` 真实实现可用。

## 3. 实施步骤

### 5.1 `EngineRegistry` 真实实现

做什么：

- 实现阶段 01 定义的 `EngineRegistry` 接口，替换 `StubEngineRegistry`。
- `register(engine: InputEngine)`：注册引擎工厂。
- `get(engineId: String): InputEngine?`：获取引擎。
- 内置引擎 ID：`direct`、`table_composing`、`transliteration`。
- **直接删除**旧 `InputEngine.kt`（`InputEngine` 接口 + `EngineType`）、`DirectInputEngine.kt`、`CompositionInputEngine.kt`。

测试：

- `get("direct")` 返回直接输入引擎。
- `get("table_composing")` 返回表组合引擎。
- `get("transliteration")` 返回转写引擎。
- `get("unknown")` 返回 `null`。
- 旧引擎代码引用已全部清除。

预期目标：

- 引擎注册和查询可用。

性能：

- `get` 使用 map 查询，目标 O(1)。

### 5.2 `DictionaryRegistry` 真实实现

做什么：

- 实现阶段 01 定义的 `DictionaryRegistry` 接口，替换 `StubDictionaryRegistry`。
- `load(key: DictionaryKey): Dictionary?`：加载字典。
- `invalidate(key: DictionaryKey)`：使缓存失效。
- 字典格式：二进制 Trie 或其他高效格式。
- **直接删除**旧 `TrieDict.kt`、`SuggestionEngine.kt`、`DictionaryImporter.kt`。

测试：

- 内置字典加载成功。
- 缓存失效后重新加载。
- 旧字典代码引用已全部清除。

预期目标：

- 字典加载和查询可用。

性能：

- 字典加载目标小于 500 ms（冷启动）。
- 查询目标小于 5 ms/次。

### 5.3 `EncoderRegistry` 真实实现

做什么：

- 实现阶段 01 定义的 `EncoderRegistry` 接口，替换 `StubEncoderRegistry`。
- `register(encoder: Encoder)`：注册编码器。
- `get(encoderId: String): Encoder?`：获取编码器。
- 内置编码器：全拼、双拼、日文罗马字等。

测试：

- `get("pinyin_full")` 返回全拼编码器。
- `get("unknown")` 返回 `null`。

预期目标：

- 编码器注册和查询可用。

性能：

- `get` 使用 map 查询，目标 O(1)。

### 5.4 `CandidatePolicyRegistry` 真实实现

做什么：

- 实现阶段 01 定义的 `CandidatePolicyRegistry` 接口，替换 `StubCandidatePolicyRegistry`。
- `register(policy: CandidatePolicy)`：注册候选策略。
- `get(policyId: String): CandidatePolicy?`：获取候选策略。
- 内置策略：拼音候选、日文候选、英文预测等。

测试：

- `get("pinyin_candidate")` 返回拼音候选策略。
- `get("unknown")` 返回 `null`。

预期目标：

- 候选策略注册和查询可用。

性能：

- `get` 使用 map 查询，目标 O(1)。

### 5.5 `DisplayPolicyRegistry` 真实实现

做什么：

- 实现阶段 01 定义的 `DisplayPolicyRegistry` 接口，替换 `StubDisplayPolicyRegistry`。
- `register(policy: DisplayPolicy)`：注册显示策略。
- `get(policyId: String): DisplayPolicy?`：获取显示策略。
- 内置策略：拼音显示、日文显示等。

测试：

- `get("pinyin_display")` 返回拼音显示策略。
- `get("unknown")` 返回 `null`。

预期目标：

- 显示策略注册和查询可用。

性能：

- `get` 使用 map 查询，目标 O(1)。

### 5.6 `EngineResourceResolver` 真实实现

做什么：

- 实现阶段 01 定义的 `EngineResourceResolver` 接口，替换 `StubEngineResourceResolver`。
- `resolve(capability: SchemaCapability, packageId: String): EngineResources`：解析引擎资源。
- 返回 `EngineResources`（字典路径、FSM 路径、映射表路径等）。
- 资源路径从 Manifest 中解析。

测试：

- `resolve(pinyinCapability, "builtin")` 返回拼音引擎资源。
- 资源文件不存在时返回空资源（不抛异常，让引擎处理）。

预期目标：

- 引擎创建 session 时可获取资源路径。

性能：

- 单次 resolve 小于 1 ms。

### 5.7 `InputPipeline` 真实实现

做什么：

- 实现阶段 01 定义的 `InputPipeline` 接口。
- `handle(action: InputAction)`：处理输入动作，调用当前 session 的 `onAction`。
- `onContextChanged(context: KeyboardContext)`：上下文变化时切换 session。
- `reset(reason: ResetReason)`：重置当前 session。
- 使用 `FakeInputConnectionGateway` 进行测试。
- Session 生命周期：创建、激活、停用、销毁。

测试：

- `PushToken("a")` → 编码 → 候选更新。
- `Delete` → 编码回退。
- `CommitCandidate` → 提交候选文本。
- `SwitchSchema` → 切换到新 session。
- `ResetReason.CONTEXT_LOST` → 清空 composing。

预期目标：

- 输入管线完整可用（使用 fake gateway）。

性能：

- 单次 `handle` 目标小于 5 ms（不含字典查询）。

### 5.8 补齐 `OrthogonalRegistry` 存在性校验

做什么：

- 替换 `OrthogonalRegistry` 中的 stub Registry 注入为真实 Registry 注入。
- 补齐 Manifest 注册时的存在性校验：
  - `engineId` 在 `EngineRegistry` 中存在。
  - `layoutId` 在 `LayoutRegistry` 中存在。
  - `encoderId` 在 `EncoderRegistry` 中存在。
  - `candidatePolicy` 在 `CandidatePolicyRegistry` 中存在。
  - `displayPolicy` 在 `DisplayPolicyRegistry` 中存在。
- 资源路径文件存在性校验。
- subtype `labelKey` 在字符串资源中存在（阶段 07 补齐）。

测试：

- 合法 Manifest 注册成功（所有 ID 存在）。
- `engineId` 不存在时注册失败。
- `layoutId` 不存在时注册失败。
- `encoderId` 不存在时注册失败。
- `candidatePolicy` 不存在时注册失败。
- `displayPolicy` 不存在时注册失败。
- 资源文件不存在时注册失败。

预期目标：

- Manifest 注册后，所有引用的资源一定存在。

性能：

- 存在性校验在注册期一次性完成，不影响运行时。

### 5.9 删除所有 stub Registry

做什么：

- 确认所有 stub Registry 已无引用。
- **直接删除** `StubEngineRegistry`、`StubLayoutRegistry`、`StubDictionaryRegistry`、`StubEncoderRegistry`、`StubCandidatePolicyRegistry`、`StubDisplayPolicyRegistry`、`StubEngineResourceResolver`。
- 保留 `FakeInputConnectionGateway` 和 `FakeFeedbackPlayer`（测试用）。

测试：

- 编译通过，无 stub 引用。
- 所有测试使用真实 Registry。

预期目标：

- 代码库中不再有 stub。

性能：

- 无影响。

## 4. 阶段验收

运行：

```bash
./gradlew test
./gradlew assembleDebug
```

静态检查：

```bash
rg "StubEngineRegistry|StubLayoutRegistry|StubDictionaryRegistry|StubEncoderRegistry|StubCandidatePolicyRegistry|StubDisplayPolicyRegistry|StubEngineResourceResolver" app/src/main/java
```

```bash
rg "TrieDict|SuggestionEngine|DictionaryImporter|DirectInputEngine|CompositionInputEngine|EngineType" app/src/main/java
```

验收标准：

- `InputPipeline` 真实实现，可处理 `InputAction` 并产生 `EngineResult`。
- 所有 Registry 真实实现，替换 stub。
- `EngineResourceResolver` 真实实现，替换 stub。
- `OrthogonalRegistry` 存在性校验补齐。
- 旧引擎、旧字典代码已直接删除。
- stub Registry 已删除（fake 组件保留供测试）。
- 内置引擎可创建 session，管线可运行。
