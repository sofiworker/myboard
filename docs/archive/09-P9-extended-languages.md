# P9: 扩展语言 (2 周)

## 1. 目标

实现藏语、法语、阿拉伯语、德语、西班牙语、俄语、印地语、泰语等语言支持。

## 2. 里程碑验收标准

- [x] 藏语输入正常
- [x] 法语 AZERTY 布局可用
- [x] 阿拉伯语 RTL 布局可用
- [x] 德语 QWERTZ 布局可用
- [x] 西班牙语布局可用
- [x] 俄语布局可用
- [x] 印地语布局可用
- [x] 泰语布局可用

## 3. 详细设计

### 3.1 各语言配置

#### 3.1.1 法语 AZERTY

```jsonc
{
  "inputMethod": {
    "id": "fr_azerty",
    "name": "Français (AZERTY)",
    "engine": "direct",
    "language": "fr-FR",
    "shift": { "mode": "autoOff", "autoOffAfterKeys": true },
    "enter": { "idle": "editorAction", "composing": "editorAction" },
    "space": { "idle": "commitText" },
    "backspace": { "idle": "delete" }
  }
}
```

#### 3.1.2 德语 QWERTZ

```jsonc
{
  "inputMethod": {
    "id": "de_qwertz",
    "name": "Deutsch (QWERTZ)",
    "engine": "direct",
    "language": "de-DE",
    "shift": { "mode": "autoOff", "autoOffAfterKeys": true },
    "enter": { "idle": "editorAction", "composing": "editorAction" },
    "space": { "idle": "commitText" },
    "backspace": { "idle": "delete" }
  }
}
```

#### 3.1.3 阿拉伯语 RTL

```jsonc
{
  "inputMethod": {
    "id": "ar_arabic",
    "name": "العربية",
    "engine": "complex",
    "language": "ar",
    "engineParams": {
      "composingType": "arabic",
      "enableLigatures": true,
      "enableReshaping": true
    },
    "shift": { "mode": "disabled" },
    "enter": { "idle": "editorAction", "composing": "commitThenAction" },
    "space": { "idle": "commitText", "composing": "commitComposition" },
    "backspace": { "idle": "delete", "composing": "deleteComposition" }
  }
}
```

#### 3.1.4 俄语 ЙЦУКЕН

```jsonc
{
  "inputMethod": {
    "id": "ru_jcuken",
    "name": "Русский (ЙЦУКЕН)",
    "engine": "direct",
    "language": "ru-RU",
    "shift": { "mode": "autoOff", "autoOffAfterKeys": true },
    "enter": { "idle": "editorAction", "composing": "editorAction" },
    "space": { "idle": "commitText" },
    "backspace": { "idle": "delete" }
  }
}
```

#### 3.1.5 印地语天城文

```jsonc
{
  "inputMethod": {
    "id": "hi_devanagari",
    "name": "हिन्दी",
    "engine": "complex",
    "language": "hi-IN",
    "engineParams": {
      "composingType": "devanagari",
      "autoVirama": true
    },
    "shift": { "mode": "disabled" },
    "enter": { "idle": "editorAction", "composing": "commitThenAction" },
    "space": { "idle": "commitText", "composing": "commitComposition" },
    "backspace": { "idle": "delete", "composing": "deleteComposition" }
  }
}
```

#### 3.1.6 泰语

```jsonc
{
  "inputMethod": {
    "id": "th_thai",
    "name": "ภาษาไทย",
    "engine": "complex",
    "language": "th-TH",
    "engineParams": {
      "composingType": "thai",
      "autoCombine": true
    },
    "shift": { "mode": "disabled" },
    "enter": { "idle": "editorAction", "composing": "commitThenAction" },
    "space": { "idle": "commitText", "composing": "commitComposition" },
    "backspace": { "idle": "delete", "composing": "deleteComposition" }
  }
}
```

#### 3.1.7 藏语

```jsonc
{
  "inputMethod": {
    "id": "bo_tibetan",
    "name": "བོད་ཡིག",
    "engine": "complex",
    "language": "bo-CN",
    "engineParams": {
      "composingType": "tibetan",
      "enableStacking": true,
      "maxStackHeight": 3
    },
    "shift": { "mode": "disabled" },
    "enter": { "idle": "editorAction", "composing": "commitThenAction" },
    "space": { "idle": "commitText", "composing": "commitComposition" },
    "backspace": { "idle": "delete", "composing": "deleteComposition" }
  }
}
```

### 3.2 RTL 布局处理

```kotlin
class RTLLayoutHandler {
    fun mirrorLayout(layout: KeyboardLayout): KeyboardLayout {
        return layout.copy(
            rows = layout.rows.map { row ->
                row.copy(keys = row.keys.reversed())
            }
        )
    }

    fun mirrorIcon(iconName: String): String {
        return when (iconName) {
            "backspace" -> "backspace_rtl"
            "shift" -> "shift_rtl"
            "enter" -> "enter_rtl"
            else -> iconName
        }
    }
}
```

### 3.3 复杂文字引擎

#### 3.3.1 ArabicReshaper

```kotlin
class ArabicReshaper {
    private val shapingRules = mapOf(
        "ب" to ShapedForms(isolated = "ب", initial = "بـ", medial = "ـبـ", final = "ـب"),
        // ... 其他字母
    )

    fun reshape(text: String): String {
        val chars = text.toMutableList()
        val result = StringBuilder()

        for (i in chars.indices) {
            val prev = chars.getOrNull(i - 1)
            val next = chars.getOrNull(i + 1)
            val form = resolveForm(chars[i], prev, next)
            result.append(form)
        }

        return result.toString()
    }

    private fun resolveForm(char: Char, prev: Char?, next: Char?): String {
        val forms = shapingRules[char.toString()] ?: return char.toString()
        return when {
            prev == null && next == null -> forms.isolated
            prev == null -> forms.initial
            next == null -> forms.final
            else -> forms.medial
        }
    }
}
```

#### 3.3.2 TibetanComposer

```kotlin
class TibetanComposer {
    fun compose(base: String, stack: List<String>, vowel: String?): String {
        var result = base
        for (s in stack.filter { it.isSuperscript() }) {
            result = applySuperscript(result, s)
        }
        for (s in stack.filter { it.isSubscript() }) {
            result = applySubscript(result, s)
        }
        if (vowel != null) {
            result = applyVowel(result, vowel)
        }
        return result
    }
}
```

## 4. 文件清单

| 文件 | 说明 |
|------|------|
| `assets/input_methods/fr_azerty.json` | 法语配置 |
| `assets/input_methods/de_qwertz.json` | 德语配置 |
| `assets/input_methods/ar_arabic.json` | 阿拉伯语配置 |
| `assets/input_methods/ru_jcuken.json` | 俄语配置 |
| `assets/input_methods/hi_devanagari.json` | 印地语配置 |
| `assets/input_methods/th_thai.json` | 泰语配置 |
| `assets/input_methods/bo_tibetan.json` | 藏语配置 |
| `assets/layouts/fr_azerty.json` | 法语布局 |
| `assets/layouts/de_qwertz.json` | 德语布局 |
| `assets/layouts/ar_arabic.json` | 阿拉伯语布局 |
| `input/complex/ArabicReshaper.kt` | 阿拉伯连字 |
| `input/complex/TibetanComposer.kt` | 藏文组合 |
| `input/complex/DevanagariComposer.kt` | 天城文组合 |
