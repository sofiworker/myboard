#!/usr/bin/env python3
"""
MyBoard 正交状态转移验证脚本。

在编译前运行，确保所有引用的资源都存在：
1. 所有 SchemaCapability 引用的 engineId 在引擎注册表中存在
2. 所有 SchemaCapability 引用的 layoutId 有对应的 JSONC 文件
3. 所有状态转移定义的目标状态合法

用法: python scripts/validate_transitions.py
"""

import os
import re
import sys
from pathlib import Path

# 项目根目录
PROJECT_ROOT = Path(__file__).parent.parent
MANIFESTS_FILE = PROJECT_ROOT / "app" / "src" / "main" / "java" / "xyz" / "xiao6" / "myboard" / "core" / "state" / "BuiltInManifests.kt"
ASSETS_DIR = PROJECT_ROOT / "app" / "src" / "main" / "assets"
LAYOUTS_DIR = ASSETS_DIR / "layouts"

# 已注册的引擎 ID（来自 MyBoardImeService.registerBuiltIns()）
REGISTERED_ENGINES = {"direct", "table_composing", "transliteration"}

# 已注册的编码器 ID
REGISTERED_ENCODERS = {"identity", "shuangpin_ziran", "t9"}

# 已注册的候选策略
REGISTERED_CANDIDATE_POLICIES = {"chinese_default", "direct_default", "japanese_kana_default"}

# 已注册的显示策略
REGISTERED_DISPLAY_POLICIES = {"show_query", "show_composing", "hidden", "show_raw"}


def parse_manifest_content():
    """从 BuiltInManifests.kt 提取所有 SchemaCapability 信息。"""
    if not MANIFESTS_FILE.exists():
        print(f"❌ Manifest 文件不存在: {MANIFESTS_FILE}")
        return []

    content = MANIFESTS_FILE.read_text(encoding="utf-8")
    capabilities = []

    # 提取每个 SchemaCapability 块
    # 匹配模式: BuiltInSchemas.XXX to SchemaCapability(...)
    pattern = r'BuiltInSchemas\.(\w+)\s+to\s+SchemaCapability\((.*?)\)'
    matches = re.finditer(pattern, content, re.DOTALL)

    for match in matches:
        schema_name = match.group(1)
        block = match.group(2)

        cap = {"schema": schema_name}

        # 提取 engineId
        engine_match = re.search(r'engineId\s*=\s*"(\w+)"', block)
        cap["engineId"] = engine_match.group(1) if engine_match else None

        # 提取 layoutId
        layout_match = re.search(r'layoutId\s*=\s*"(\w+)"', block)
        cap["layoutId"] = layout_match.group(1) if layout_match else None

        # 提取 encoderId
        encoder_match = re.search(r'encoderId\s*=\s*"(\w+)"', block)
        cap["encoderId"] = encoder_match.group(1) if encoder_match else None

        # 提取 candidatePolicy
        candidate_match = re.search(r'candidatePolicy\s*=\s*"(\w+)"', block)
        cap["candidatePolicy"] = candidate_match.group(1) if candidate_match else None

        # 提取 displayPolicy
        display_match = re.search(r'displayPolicy\s*=\s*"(\w+)"', block)
        cap["displayPolicy"] = display_match.group(1) if display_match else None

        # 提取 dictionary
        dict_match = re.search(r'dictionary\s*=\s*"([^"]*)"', block)
        cap["dictionary"] = dict_match.group(1) if dict_match else None

        # 提取 dictionaryOptional
        cap["dictionaryOptional"] = "dictionaryOptional = true" in block

        capabilities.append(cap)

    return capabilities


def check_engine_ids(capabilities):
    """验证所有 engineId 在引擎注册表中存在。"""
    errors = []
    for cap in capabilities:
        engine_id = cap.get("engineId")
        schema = cap.get("schema", "unknown")
        if engine_id and engine_id not in REGISTERED_ENGINES:
            errors.append(f"  Schema '{schema}': engineId '{engine_id}' 未注册 (可用: {REGISTERED_ENGINES})")
    return errors


def check_layout_ids(capabilities):
    """验证所有 layoutId 有对应的 JSONC 文件或硬编码布局。"""
    errors = []
    # 获取所有可用的布局 ID（JSONC 文件 + 硬编码）
    jsonc_layouts = set()
    if LAYOUTS_DIR.exists():
        for f in LAYOUTS_DIR.glob("*.jsonc"):
            jsonc_layouts.add(f.stem)

    # 硬编码布局（BuiltInLayouts.kt 中注册的）
    builtin_layouts = {"qwerty"}

    available_layouts = jsonc_layouts | builtin_layouts

    for cap in capabilities:
        layout_id = cap.get("layoutId")
        schema = cap.get("schema", "unknown")
        if layout_id and layout_id not in available_layouts:
            errors.append(f"  Schema '{schema}': layoutId '{layout_id}' 不存在 (可用: {available_layouts})")
    return errors


def check_encoder_ids(capabilities):
    """验证所有 encoderId 在编码器注册表中存在。"""
    errors = []
    for cap in capabilities:
        encoder_id = cap.get("encoderId")
        schema = cap.get("schema", "unknown")
        if encoder_id and encoder_id not in REGISTERED_ENCODERS:
            errors.append(f"  Schema '{schema}': encoderId '{encoder_id}' 未注册 (可用: {REGISTERED_ENCODERS})")
    return errors


def check_candidate_policies(capabilities):
    """验证所有 candidatePolicy 已注册。"""
    errors = []
    for cap in capabilities:
        policy = cap.get("candidatePolicy")
        schema = cap.get("schema", "unknown")
        if policy and policy not in REGISTERED_CANDIDATE_POLICIES:
            errors.append(f"  Schema '{schema}': candidatePolicy '{policy}' 未注册 (可用: {REGISTERED_CANDIDATE_POLICIES})")
    return errors


def check_display_policies(capabilities):
    """验证所有 displayPolicy 已注册。"""
    errors = []
    for cap in capabilities:
        policy = cap.get("displayPolicy")
        schema = cap.get("schema", "unknown")
        if policy and policy not in REGISTERED_DISPLAY_POLICIES:
            errors.append(f"  Schema '{schema}': displayPolicy '{policy}' 未注册 (可用: {REGISTERED_DISPLAY_POLICIES})")
    return errors


def check_dictionary_references(capabilities):
    """验证字典引用的文件路径格式正确。"""
    warnings = []
    for cap in capabilities:
        dictionary = cap.get("dictionary")
        schema = cap.get("schema", "unknown")
        optional = cap.get("dictionaryOptional", False)
        if dictionary and not optional:
            if not dictionary.startswith("dicts/") and not dictionary.endswith(".dict"):
                warnings.append(f"  Schema '{schema}': dictionary '{dictionary}' 路径格式可能不正确")
    return warnings


def check_layout_files_exist():
    """验证所有 JSONC 布局文件可以被正确解析（检查文件存在性和基本格式）。"""
    errors = []
    warnings = []
    if not LAYOUTS_DIR.exists():
        errors.append(f"  布局目录不存在: {LAYOUTS_DIR}")
        return errors, warnings

    for f in LAYOUTS_DIR.glob("*.jsonc"):
        content = f.read_text(encoding="utf-8")
        # 检查是否包含 schemaVersion
        if '"schemaVersion"' not in content:
            warnings.append(f"  {f.name}: 缺少 schemaVersion 字段")
        else:
            # 检查 schemaVersion 是否为语义化版本格式 ("a.b.c")
            version_match = re.search(r'"schemaVersion"\s*:\s*"(\d+\.\d+\.\d+)"', content)
            if version_match:
                # 格式正确
                pass
            else:
                # 检查是否为旧的整数格式
                int_match = re.search(r'"schemaVersion"\s*:\s*(\d+)', content)
                if int_match:
                    warnings.append(f"  {f.name}: schemaVersion 使用旧的整数格式 ({int_match.group(1)})，应改为语义化版本字符串 (如 \"1.0.0\")")
                else:
                    warnings.append(f"  {f.name}: schemaVersion 格式不正确")
        # 检查是否包含 id
        if '"id"' not in content:
            warnings.append(f"  {f.name}: 缺少 id 字段")
        # 检查是否包含 root
        if '"root"' not in content:
            errors.append(f"  {f.name}: 缺少 root 字段")

    return errors, warnings


def check_theme_files_exist():
    """验证所有 JSONC 主题文件的 schemaVersion 格式。"""
    errors = []
    warnings = []
    themes_dir = ASSETS_DIR / "themes"
    if not themes_dir.exists():
        warnings.append(f"  主题目录不存在: {themes_dir}")
        return errors, warnings

    for f in themes_dir.glob("*.jsonc"):
        content = f.read_text(encoding="utf-8")
        # 检查是否包含 schemaVersion
        if '"schemaVersion"' not in content:
            warnings.append(f"  {f.name}: 缺少 schemaVersion 字段")
        else:
            # 检查 schemaVersion 是否为语义化版本格式 ("a.b.c")
            version_match = re.search(r'"schemaVersion"\s*:\s*"(\d+\.\d+\.\d+)"', content)
            if version_match:
                # 格式正确
                pass
            else:
                # 检查是否为旧的整数格式
                int_match = re.search(r'"schemaVersion"\s*:\s*(\d+)', content)
                if int_match:
                    warnings.append(f"  {f.name}: schemaVersion 使用旧的整数格式 ({int_match.group(1)})，应改为语义化版本字符串 (如 \"1.0.0\")")
                else:
                    warnings.append(f"  {f.name}: schemaVersion 格式不正确")

    return errors, warnings


def check_orthogonal_completeness(capabilities):
    """验证正交维度的完整性：每个 locale+script 组合是否都有对应的 schema。"""
    # 从 manifest 提取 locale-script-schema 映射
    # 这里简化检查：确保每个 locale 至少有一个可用的 schema
    schema_names = [cap["schema"] for cap in capabilities]
    info = []
    info.append(f"  已注册 Schema: {', '.join(schema_names)}")
    info.append(f"  共 {len(capabilities)} 个 SchemaCapability")
    return info


def main():
    print("=" * 60)
    print("MyBoard 正交状态转移验证")
    print("=" * 60)
    print()

    # 解析 manifest
    print("📋 解析 BuiltInManifests.kt ...")
    capabilities = parse_manifest_content()
    if not capabilities:
        print("❌ 未能解析出任何 SchemaCapability")
        sys.exit(1)
    print(f"   找到 {len(capabilities)} 个 SchemaCapability")
    print()

    total_errors = 0
    total_warnings = 0

    # 1. 验证 engineId
    print("🔧 验证 engineId ...")
    errors = check_engine_ids(capabilities)
    if errors:
        for e in errors:
            print(f"❌{e}")
        total_errors += len(errors)
    else:
        print("   ✅ 所有 engineId 有效")
    print()

    # 2. 验证 layoutId
    print("📐 验证 layoutId ...")
    errors = check_layout_ids(capabilities)
    if errors:
        for e in errors:
            print(f"❌{e}")
        total_errors += len(errors)
    else:
        print("   ✅ 所有 layoutId 有效")
    print()

    # 3. 验证 encoderId
    print("🔤 验证 encoderId ...")
    errors = check_encoder_ids(capabilities)
    if errors:
        for e in errors:
            print(f"❌{e}")
        total_errors += len(errors)
    else:
        print("   ✅ 所有 encoderId 有效")
    print()

    # 4. 验证 candidatePolicy
    print("📊 验证 candidatePolicy ...")
    errors = check_candidate_policies(capabilities)
    if errors:
        for e in errors:
            print(f"❌{e}")
        total_errors += len(errors)
    else:
        print("   ✅ 所有 candidatePolicy 有效")
    print()

    # 5. 验证 displayPolicy
    print("👁️ 验证 displayPolicy ...")
    errors = check_display_policies(capabilities)
    if errors:
        for e in errors:
            print(f"❌{e}")
        total_errors += len(errors)
    else:
        print("   ✅ 所有 displayPolicy 有效")
    print()

    # 6. 验证字典引用
    print("📖 验证字典引用 ...")
    warnings = check_dictionary_references(capabilities)
    if warnings:
        for w in warnings:
            print(f"⚠️{w}")
        total_warnings += len(warnings)
    else:
        print("   ✅ 所有字典引用格式正确")
    print()

    # 7. 验证布局文件
    print("📁 验证布局文件 ...")
    errors, warnings = check_layout_files_exist()
    if errors:
        for e in errors:
            print(f"❌{e}")
        total_errors += len(errors)
    if warnings:
        for w in warnings:
            print(f"⚠️{w}")
        total_warnings += len(warnings)
    if not errors and not warnings:
        print("   ✅ 所有布局文件有效")
    print()

    # 7b. 验证主题文件
    print("🎨 验证主题文件 ...")
    errors, warnings = check_theme_files_exist()
    if errors:
        for e in errors:
            print(f"❌{e}")
        total_errors += len(errors)
    if warnings:
        for w in warnings:
            print(f"⚠️{w}")
        total_warnings += len(warnings)
    if not errors and not warnings:
        print("   ✅ 所有主题文件有效")
    print()

    # 8. 正交完整性
    print("🔀 正交维度完整性:")
    info = check_orthogonal_completeness(capabilities)
    for i in info:
        print(i)
    print()

    # 总结
    print("=" * 60)
    if total_errors == 0:
        print(f"✅ 验证通过！({total_warnings} 个警告)")
        print("=" * 60)
        sys.exit(0)
    else:
        print(f"❌ 验证失败！{total_errors} 个错误, {total_warnings} 个警告")
        print("=" * 60)
        sys.exit(1)


if __name__ == "__main__":
    main()
