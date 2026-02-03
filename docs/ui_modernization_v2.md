# MyBoard UI 精细化改造 - 第二阶段

## 概述
本次精细化改造进一步优化了弹出布局、Toolbar 图标和输入主题的视觉细节，使整体UI更加精致、现代化。

## 主要改动

### 1. Toolbar 图标精细化 (app/src/main/res/drawable/ic_toolbar_*.xml)

所有 Toolbar 图标重新设计，采用更精细的 Material Design 风格：

| 图标 | 改进点 |
|------|--------|
| `ic_toolbar_clipboard` | 更清晰的剪贴板形状，优化的角标细节 |
| `ic_toolbar_emoji` | 更现代的笑脸设计，更自然的曲线 |
| `ic_toolbar_settings` | 齿轮比例优化，线条更流畅 |
| `ic_toolbar_layout` | 键盘布局图标，清晰的按键网格 |
| `ic_toolbar_voice` | 麦克风图标，简洁的轮廓设计 |

设计规范：
- 24dp x 24dp 标准尺寸
- 2dp 线条粗细一致性
- 统一的圆角处理
- 纯黑色填充 (#000000)

### 2. 弹出布局优化 (PopupView.kt)

#### 按键预览弹出
- **更大的圆角**: 从 12dp 增至 16dp
- **更白的背景**: 纯白 (#FFFFFFFF) 替代半透明黑
- **更大的字体**: 32sp 粗体，更清晰可读
- **更大的内边距**: 24dp x 16dp，更舒适的视觉
- **移除边框**: 无边框设计，更现代

#### 长按候选弹出
- **圆角增大**: 从 12dp 增至 20dp
- **纯白背景**: 干净清爽
- **候选项圆角**: 14dp，选中状态更明显
- **选中样式**: 蓝色背景 + 白色文字
- **间距优化**: 8dp 项间距，更透气

### 3. 候选栏优化 (CandidateView.kt)

- **更大圆角**: 从 10dp 增至 20dp
- **首个候选高亮**: 半透明蓝色背景，视觉引导
- **更大点击区域**: 最小宽度 52dp，更易点击
- **优化内边距**: 14dp x 8dp，更平衡
- **字重优化**: 首个候选使用粗体

### 4. Modern 主题配置更新

```json
{
  "keyPopup": {
    "surface": {
      "cornerRadiusDp": 16,
      "elevationDp": 8,
      "shadowColor": "#40000000"
    },
    "text": { "sizeSp": 32, "bold": true },
    "item": {
      "cornerRadiusDp": 14,
      "paddingDp": 12,
      "selectedBackground": "colors.accent"
    }
  }
}
```

**暗色模式优化**:
- 更深的阴影 (#80000000)
- 保持一致的字号和圆角

## 设计细节

### 圆角系统更新
| 元素 | 之前 | 之后 |
|------|------|------|
| 按键 | 10dp | 16dp |
| 按键预览弹出 | 12dp | 16dp |
| 候选弹出 | 12dp | 20dp |
| 候选栏 | 10dp | 20dp |
| 候选项 | - | 14dp |
| 工具栏 | 12dp | 20dp |

### 颜色系统
**亮色模式**:
- 弹出背景: 纯白 (#FFFFFFFF)
- 弹出边框: 浅灰 (#FFE2E8F0)
- 选中背景: 蓝色 (#FF3B82F6)
- 文字: 深黑 (#FF1A1C1E)

**暗色模式**:
- 弹出背景: 深灰 (#FF1E293B)
- 选中背景: 亮蓝 (#FF60A5FA)
- 文字: 近白 (#FFF1F5F9)

### 间距系统
- 按键预览内边距: 24dp x 16dp
- 候选弹出内边距: 10dp
- 候选项内边距: 14dp x 12dp
- 候选栏内边距: 6dp x 4dp
- 项间距: 8dp

## 视觉效果

### 层次增强
1. **阴影层次**: 
   - 按键: 1dp 高度微阴影
   - 弹出: 8dp 明显阴影
   - 强调键: 4dp 带色阴影

2. **圆角层次**:
   - 外层容器: 最大圆角 (20dp)
   - 内容区域: 中等圆角 (16dp)
   - 内部元素: 小圆角 (14dp)

### 交互反馈
- 按键按下: 背景色变化 + 轻微收缩
- 候选选中: 蓝色背景 + 白色文字 + 粗体
- 首个候选: 半透明蓝色背景提示

## 技术实现

### PopupView 关键改进
```kotlin
// 更大的圆角和字体
previewTextView.apply {
    textSize = 32f
    setPadding(dpInt(24f), dpInt(16f), dpInt(24f), dpInt(16f))
    background = GradientDrawable().apply {
        cornerRadius = dp(16f)
        setColor(Color.parseColor("#FFFFFFFF"))
    }
}
```

### CandidateView 关键改进
```kotlin
// 首个候选高亮
if (isFirst) {
    textView.background = GradientDrawable().apply {
        cornerRadius = ...
        setColor(Color.parseColor("#103B82F6")) // 10% 透明度蓝色
    }
}
```

## 对比总结

| 特性 | 改造前 | 改造后 |
|------|--------|--------|
| 整体风格 | iOS 风格 | Material You |
| 圆角大小 | 较小 (10-12dp) | 较大 (14-20dp) |
| 弹出背景 | 半透明黑 | 纯白/深灰 |
| 图标风格 | 多样 | 统一 Material |
| 阴影效果 | 轻微 | 层次分明 |
| 候选高亮 | 仅文字颜色 | 背景+文字 |

## 后续建议

1. **动画优化**: 添加弹出/消失的缩放和透明度动画
2. **手势增强**: 候选栏支持滑动删除已选词
3. **个性化**: 允许用户自定义弹出样式
4. **无障碍**: 优化 TalkBack 朗读体验
