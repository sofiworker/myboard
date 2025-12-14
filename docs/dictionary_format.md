# MyBoard 字典文件格式（MYBDF v1）

本项目把“字典元数据（dictionaryId/适用语言/版本等）”与“字典内容（code->候选词索引）”打包到同一个二进制文件中，便于：

- 用户上传不同来源字典后统一转换、校验、落盘
- APK 打包时自动生成内置字典资源
- 运行时仅靠 `.mybdict` 文件即可完成校验/匹配/加载（`.json` 元数据可选）

## 1. 顶层文件（MYBDF v1）

字节序：小端（little-endian）

文件布局：

1) 固定头（64 bytes）
2) 元数据 JSON（UTF-8，长度由头部 `meta_size` 指定）
3) payload（raw 或 zlib，长度由头部 `payload_size_stored` 指定）

### 1.1 魔术头 / 版本 / 语言 / 校验和

固定头（64 bytes）字段（offset 单位：byte，全部为 little-endian）：

- `0..7` `magic[8]`：固定为 `"MYBDF001"`
- `8..11` `format_version u32`：固定为 `1`
- `12..13` `dict_ver_major u16`
- `14..15` `dict_ver_minor u16`
- `16..17` `dict_ver_patch u16`：语义版本 `a.b.c`
- `18..19` `reserved0 u16`：固定 `0`
- `20..21` `language_code u16`：主语言（2 字节小写 ASCII 打包，例如 `"zh"` / `"en"`）
- `22` `region_code u8`：主地区枚举（见下）
- `23` `script_type u8`：文字类型枚举（见下）
- `24..27` `feature_flags u32`：语言/输入特性位图（见下）
- `28..31` `flags u32`：低 4 bit 为压缩算法 id
  - `0`：不压缩（payload 为 raw）
  - `1`：zlib（payload 为 zlib 压缩字节流）
- `32..35` `header_size u32`：固定为 `64`
- `36..39` `meta_size u32`：元数据 JSON 字节长度
- `40..43` `payload_size_uncompressed u32`：payload 解压后的长度
- `44..47` `payload_size_stored u32`：payload 在文件中的长度（压缩后/未压缩）
- `48..51` `crc32_payload u32`：对“解压后的 payload（MYBDICT1 bytes）”做 CRC32
- `52..55` `crc32_header_meta u32`：对 `[header + meta]` 做 CRC32，但计算时把该字段视为 0
- `56..63` `reserved[8]`：固定填 0，预留扩展

校验规则：

- `crc32_header_meta`：对 `[header(64) + meta(meta_size)]` 计算 CRC32，计算时将 `52..55` 视为 0
- `crc32_payload`：对“解压后的 payload（MYBDICT1 bytes）”计算 CRC32（因此与是否压缩无关）

实现参考：

- 运行期读写：`app/src/main/java/xyz/xiao6/myboard/dictionary/format/MyBoardDictionaryFileV1.kt`
- 构建期生成：`scripts/dict_tool.py`

### 1.2 元数据（JSON，可扩展）

当前元数据 schema（`MyBoardDictionaryFileV1.DictionaryFileMeta`）：

- `dictionaryId: string`：字典唯一 ID（与 `Subtype.dictionaryId` 对齐）
- `name?: string`：展示名称
- `languages: string[]`：适用语言（BCP-47，例如 `zh-CN` / `en`）
- `sourceFormat?: string`：源格式 id（例如 `rime_dict_yaml`）
- `sourceVersion?: string`：源字典版本文本（原样保留，可能不是 a.b.c）
- `createdBy?: string`：生成器标识（例如 `myboard` / `myboard_app` / `myboard_build`）
- `createdAtEpochMs?: number`：生成时间戳

### 1.3 枚举与位图（参考 a.cc 思路）

`language_code u16`：

- 2 字节小写 ASCII 打包（例如 `"zh"` -> `0x687A`，bytes 为 `'z','h'`）

`region_code u8`（当前实现）：

- `0=UNKNOWN, 1=CN, 2=TW, 3=HK, 4=MO, 10=US`

`script_type u8`（当前实现，见 `LanguageProfile.ScriptType`）：

- `0=UNKNOWN, 1=LATIN, 2=HAN, 3=HIRAGANA, 4=KATAKANA, 5=HANGUL, 6=TIBETAN, 7=ARABIC, 8=THAI, 9=DEVANAGARI`

`feature_flags u32`（当前实现，见 `LanguageProfile.FeatureFlags`）：

- `HAS_TONES(1<<0)`：有声调
- `HAS_PITCH(1<<1)`：有音高
- `HAS_DIACRITICS(1<<2)`：有变音符号
- `REQUIRES_COMPOSE(1<<3)`：需要组合规则
- `IS_LOGGRAPHIC(1<<4)`：表意文字
- `IS_SYLLABIC(1<<5)`：音节文字
- `IS_ABJAD(1<<6)`：辅音音素文字
- `HAS_CASE(1<<7)`：有大小写
- `RTL_WRITING(1<<8)`：从右向左书写

## 2. payload（MYBDICT1）

payload 是“code->候选词列表”的紧凑索引，magic 为：

- `magic[8] = "MYBDICT1"`

### 2.1 结构（little-endian）

Header（固定 44 bytes）：

- `magic[8] = "MYBDICT1"`
- `payload_version u32 = 1`
- `flags u32 = 0`
- `code_count u32`
- `entry_count u32`
- `code_index_offset u32`
- `entry_table_offset u32`
- `code_blob_offset u32`
- `word_blob_offset u32`
- `payload_size u32`

Records：

- `code_index[code_count]` 每条 12 bytes：
  - `code_offset u32`（相对 `code_blob_offset`）
  - `first_entry_index u32`
  - `entry_count_for_code u32`
- `entry_table[entry_count]` 每条 8 bytes：
  - `word_offset u32`（相对 `word_blob_offset`）
  - `weight i32`

Blobs：

- `code_blob`：NUL 结尾 UTF-8 字符串池（按 code 排序）
- `word_blob`：NUL 结尾 UTF-8 字符串池

### 2.2 查询方式（运行期）

当前运行时查询流程：

- 头部给出 `code_index`、`entry_table`、`code_blob`、`word_blob` 的 offset
- `code_index` 通过二分查找定位 code
- `entry_table` 存储每条 entry 的 `word_offset + weight`
- `*_blob` 为 NUL 结尾 UTF-8 字符串池

实现参考：

- `app/src/main/java/xyz/xiao6/myboard/dictionary/MyBoardDictionary.kt`

### 2.3 code 规范（MyBoard Canonical Code）

MyBoard 的字典 payload 并不关心“外部字典是什么格式”，它只存储 **MyBoard 规范化后的 code**（Canonical Code）并提供查询。

运行时原则：

- 运行时（Kotlin）只需要知道：如何把“用户按键输入流”编码成 canonical code，然后对 `.mybdict` 做查询。
- 任何外部来源（Rime YAML / txt / 在线等）的兼容逻辑都必须在 **convert 层**完成，运行时不为外部格式加分支。

通用约束（所有 code scheme 都必须满足）：

- `code` 使用 UTF-8 存储；运行时查询以字符串为单位。
- `code` **必须是稳定、可比较的字节序列**（用于二分查找和前缀扫描）。
- `code` 不允许前后空白；不允许包含 `\0`。
- 推荐仅使用 ASCII 可见字符（便于调试与跨平台一致性）。

查询语义（IME 必需能力）：

- IME 组合输入期间，用户输入通常是完整 code 的**前缀**；因此运行时查询应支持：
  - `exact(code)`：精确匹配
  - `prefix(prefixCode)`：前缀匹配（返回所有 `code.startswith(prefixCode)` 的候选集合，按 code 顺序收集，不做全局权重重排）

当前内置 scheme（建议先固定一套，后续再扩展）：

- `PINYIN_FULL`（全拼，默认）：
  - 字符集：`a-z`（小写）
  - 规范化：小写；移除音节分隔空格；移除 `'`（如需保留歧义分隔，必须在 scheme 里明确并让 decoder 也产出同样形式）
  - 示例：
    - 外部 `ni hao` -> canonical `nihao`
    - 外部 `xi'an` -> canonical `xian`（注意：若此规则不满足你的歧义需求，请在 schema 中为该 scheme 明确保留 `'` 并同步调整 decoder）

备注：

- payload 里虽然存了 `weight`，但当前 `MyBoardDictionary.candidates*` 只按文件中顺序返回，不做复杂排序；如未来需要“跨 code 的 top-k”或更复杂的候选融合，应作为更高层策略实现，不要反向污染 payload 格式。

## 3. 支持范围

- App 端解析器仅支持：
  - `MYBDF001`（container）
  - `MYBDICT1`（payload）

## 4. 构建期工具链（Python）

Gradle 构建期通过 Python 脚本完成资源生成（见 `app/build.gradle.kts`）：

- `generateSubtypes`：读取 `assets/layouts/*.json` + `assets/dictionary/*.json`，生成 `build/generated/subtypesAssets/subtypes/generated.json`
- `convertDictionaries`：读取 `assets/dictionary/base.dict.yaml`，生成 `build/generated/dictionaryAssets/dictionary/base.mybdict`（MYBDF v1）
  - 同时生成该字典的 `DictionarySpec` 草稿：`build/generated/dictionaryAssets/dictionary/dict_pinyin.generated.json`
  - 可在 `assets/dictionary/dict_pinyin.json` 手动维护/修正（例如 `layoutIds`），构建时会覆盖草稿并用于 `generateSubtypes`

注意：当前未发布阶段的工作流会将生成的 `base.mybdict` 直接写入 `assets/dictionary/base.mybdict`，以确保 APK 打包时一定包含该二进制字典资源。
同理，`generateSubtypes` 会将生成的 subtype pack 直接写入 `assets/subtypes/generated.json`，以确保 APK 打包时一定包含该映射表（locale -> layoutIds[]）。

建议工作流（需要人工指定 layout 兼容性时）：

1) 先跑一次构建或单独运行 `convertDictionaries` 生成 `*.generated.json` 草稿
2) 将草稿复制为 `assets/dictionary/<dictionaryId>.json`，手动修正 `layoutIds/localeTags/priority` 等字段
3) 后续构建会优先使用 `assets/dictionary/*.json`，并据此生成 `subtypes/generated.json`

脚本位置：

- `scripts/generate_subtypes.py`
- `scripts/dict_tool.py`

### 4.1 convert 层职责（外部字典 -> MyBoard Canonical Code）

convert 层是唯一允许“兼容外部格式”的地方，职责包括：

1) 解析外部格式（例如 Rime `*.dict.yaml`）
2) 将外部的 `code` 映射为 MyBoard 的 canonical code（按上文 scheme 规则）
3) 生成 `.mybdict`（MYBDF v1 container + MYBDICT1 payload）
4) 生成/维护 `DictionarySpec`（JSON 元数据，用于 runtime 按 locale/layout/mode 选择字典）

实现参考：

- `scripts/dict_tool.py`（构建期转换）
- `app/src/main/java/xyz/xiao6/myboard/dictionary/DictionaryImporter.kt`（运行期导入转换入口）

## 5. 运行期导入（Kotlin）

用户上传字典的导入/转换入口：

- `app/src/main/java/xyz/xiao6/myboard/dictionary/DictionaryImporter.kt`
  - 内部使用 `app/src/main/java/xyz/xiao6/myboard/dictionary/format/ExternalDictionaryConverter.kt` 生成 `.mybdict`
