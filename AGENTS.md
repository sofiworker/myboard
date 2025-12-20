### 编码参考
- 当前做的任何修改都不需要考虑兼容性，目前app并为发版和上架
- 你需要尽可能的考虑代码的扩展性
- 尽量多的使用kotlin的语言特性
- 尽量多的使用jetpack和compose
- 必须要保证不同版本之间的权限处理，同时要求最小化权限申请
- 无论任何地方的设置都需要保证 SettingsActivity.kt 中存在，并且需要保证设置的单一来源，不能存在多份变量导致状态不一致
- 尽量保证数据与视图分离
- 每次修改后都需要保证编译通过
- 需要保证i18n实现

### 键盘存在语言切换的按钮时
- 获取当前语言layout，判断该layout是否在目标语言中也有
- 如果目标语言存在该layout，禁止重绘键盘，直接进行符号替换
- 如果目标语言不存在该layout，判断用户的layout manager中是否有该语言的layout，如果存在直接切换过去
- 如果目标语言不存在该layout，则选用语言默认的layout

### 验证方式（必须可用 `./gradlew` 验证）
1) 生成 subtype 资源并编译 Debug 包：
   - `./gradlew :app:assembleDebug`
2) 只验证 Kotlin 编译（更快）：
   - `./gradlew :app:compileDebugKotlin`
3) 验证 subtype 生成结果存在：
   - 编译完成后检查：`app/build/generated/subtypesAssets/subtypes/generated.json`
4)（实现 DictionaryResolver 等后）建议补充的验证点：
   - `./gradlew :app:testDebugUnitTest`（若后续加了单元测试）
   - 打开 Demo/IME 后，切换 layout 与 mode 时候选/decoder 会跟随变更（人工回归）
