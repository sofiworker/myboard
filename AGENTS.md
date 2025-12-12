## 参考 FlorisBoard 的核心布局/切换设计
- FlorisBoard 将“布局切换”拆为两个正交维度：
  - `KeyboardMode`（字符/符号/数字/电话等）决定当前键盘的“模式布局”，模式切换只改 mode，不改用户主布局。
  - `Subtype/CharactersLayout` 决定字符模式下使用的具体布局（如 qwerty、t9、用户自定义），只在 `CHARACTERS` mode 生效。
- `KeyboardState` 作为单一真源：包含 `keyboardMode`、`inputShiftState`、UI 面板开关等；任何模式/面板变化都先更新 state，再统一重算可见键盘。
- `LayoutManager` 按 `keyboardMode + subtype + prefs` 计算键盘；缓存按 mode/subtype 维度，不同 mode 间状态互不污染。
- Smartbar/候选条在键盘盒子外一行显示，展开候选（下拉）在盒子内面板呈现；emoji/剪贴板等作为盒子内“sheet/panel”，不会改变 mode。
这里我们可以简化为 keyboard mode 和 keyboard layout两种维度，其中用户通过toolbar或者设置调整的为layout，layout有常见的handwriting、t9、全键盘、笔画、双拼等；
mode 则为处于 layout 下切换键盘类型，比如用户设置了 t9,那么永远保证我们的键盘布局为t9,通过t9的部分按键切换 mode，渲染公共的布局，如：数字、符号等

### Myboard 的适配落地
- 新增 `KeyboardMode`（`CHARACTERS/NUMERIC/SYMBOLS`），作为布局切换的核心真源；`SwitchToLayout("numeric"/"symbols"/"_main_")` 仅改变 mode，避免误切到 t9 或其它字符布局。
- 仅在 `CHARACTERS` mode 使用 `_characterLayoutName`（来自设置的主布局或用户指定），数字/符号布局固定为 `numeric.json`/`symbols.json`。
- `SwitchToLayout` 处理顺序与 FlorisBoard 一致：先提交 composingText、清空 suggestions/shift/候选展开，再切 mode；Service 侧同步 `finishComposingText()`。
- emoji/剪贴板/语音等归入“盒子内面板”(panel)，由 toolbar 控制显示，不影响 `keyboardMode`。

## 执行计划（两段键盘结构 + 通用特殊键）

### 目标约定
- 键盘 UI 固定两段：第一段为动态调整段（空闲=toolbar，输入=候选条/拼音+候选）；第二段为布局段（按用户布局加载 t9/双拼/26键等）。
- 第一段行为：
  - 空闲状态：显示 toolbar（全局按键，不依赖布局文件）。
  - toolbar 最左侧固定为设置按钮用于跳转到设置页面，其余为用户通过键盘功能选项自定义个数和顺序，但是默认我们添加键盘布局切换，注意是layout切换不是mode切换，然后是剪切板，剪切板高度包含第一段和第二段，其中第一段为功能用于返回和删除，一左一右，然后为 emoji，其中emoji与剪切板一样，第一段功能区，第二段用于展示表情，第二段底部还有滚动条用于不同emoji类型的切换
  - 有 composingText 时：显示候选条；右侧固定下拉箭头进入滚动候选页面；候选条上方显示当前拼音串；英文状态则不显示拼音行，仅候选列表。
  - 其中如果是中文拼音输入，拼音+候选，拼音是在第一段和第二段高度之上的，并不和整个键盘统一，或者拼音可以落入输入框中，这个功能由用户设置
- 第二段行为：
  - 仅渲染当前 characters 布局（t9/双拼/26键）或 mode 布局（numeric/symbols）。
  - 布局内的特殊键（如 26 键 `123`、t9 的 `符` 等）采用**通用映射**，对所有布局一致生效。
  - 特殊键切换 mode 后，页面高度不变，字符页面占用两段高度、数字键页面只占用第二段高度，第一段由toolbar填充
  - 字符页面实际会在整个box的下面新增滚动条用于筛选，数学、网络等字符类型
- 键盘toolbar直接通过编码实现，布局json中不再存储toolbar信息
- 调整 assets 文件夹中的文件定义和位置，让其见名知其意，目前命名十分混乱
- 调整 kotlin 代码路径和定义，拆分组件，尽量提高组件的复用性
- 将大部分能设置的地方统一到设置页面中进行设置并持久化管理
- layout 的 json 需要引入每行的高度和每个key的间隔

### 步骤
1. 定义顶层 UI 状态机
   - 在 ViewModel 增加/明确 `topBarState`（Toolbar / CandidatesCollapsed / CandidatesExpanded），由 `composingText`、`suggestions`、输入法语言模式（中/英）驱动。
   - 统一候选展开触发：固定箭头点击 -> `CandidatesExpanded`。
2. 重构 `KeyboardScreen` 为两段布局
   - 把现有 CandidateBar/Toolbar/CandidateGrid 组合成**同一段**的互斥渲染，不再占用独立“第三段”高度。
   - 第一段高度固定（如 toolbarHeight 或 candidateHeight 的较小值），展开候选时同段内部滚动/分页。
3. 统一候选展示规则
   - 中文模式：候选段顶部显示拼音串（来自 `composingText`），下方一行/多行候选；右侧箭头常驻。
   - 英文模式：候选段不显示拼音串，仅候选列表；箭头仍用于展开更多候选。
   - 选择候选/切模式时先提交 composing 并清空 suggestions。
4. 通用特殊键映射层
   - 新增 `SpecialKeyResolver`（或等价逻辑）把布局 json 中的通用键类型/label/value 映射到 `KeyAction`：
     - 例：`switch_to_layout` 的 `numeric/symbols/_main_`；t9/26/双拼均复用。
   - `toKeyAction()` 优先走 resolver，避免不同布局重复定义行为。
5. 校验与回归
   - 逐一在 26/t9/双拼布局中验证：`123`/`符`/`ABC` 等切换键不改字符布局名，只切 mode。
   - 验证两段高度在竖/横屏及浮动模式下不闪烁、不挤压候选。
6. 文档与 assets 清理（可选最后做）
   - 在 README/feature.md 补一段两段结构说明与特殊键约定。
   - 再次扫描 assets，删除无引用 json（以代码中 `assets.open(...)` 与 layout/theme 列表为准），保留用户可选的 layouts/themes。
