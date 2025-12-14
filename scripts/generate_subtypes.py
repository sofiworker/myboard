#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable


def _normalize_locale_tag(tag: str) -> str:
    t = tag.strip().replace("_", "-")
    if not t:
        return ""
    parts = [p for p in t.split("-") if p]
    if not parts:
        return ""
    language = parts[0].lower()
    region = parts[1].upper() if len(parts) > 1 else ""
    return f"{language}-{region}" if region else language


def _load_json(path: Path) -> Any:
    with path.open("r", encoding="utf-8") as f:
        return json.load(f)


def _iter_json_files(dir_path: Path) -> list[Path]:
    if not dir_path.exists():
        return []
    return sorted([p for p in dir_path.iterdir() if p.is_file() and p.suffix.lower() == ".json"])


@dataclass(frozen=True)
class LayoutDef:
    layout_id: str
    name: str | None
    locale_tags: tuple[str, ...]  # normalized, unique


@dataclass(frozen=True)
class DictionaryDef:
    dictionary_id: str
    name: str | None
    locale_tags: tuple[str, ...]  # normalized, unique
    layout_ids: tuple[str, ...]  # empty means all layouts
    enabled: bool
    priority: int
    source: Path


def _parse_layout(obj: dict[str, Any], source: Path) -> LayoutDef:
    layout_id = str(obj.get("layoutId", "")).strip()
    if not layout_id:
        raise ValueError(f"{source}: missing layoutId")
    name = obj.get("name")
    if name is not None:
        name = str(name)
    raw_locales = obj.get("locale", [])
    if raw_locales is None:
        raw_locales = []
    if not isinstance(raw_locales, list):
        raise ValueError(f"{source}: locale must be a list")
    normalized = [_normalize_locale_tag(str(x)) for x in raw_locales]
    normalized = [x for x in normalized if x]
    deduped = tuple(dict.fromkeys(normalized).keys())
    return LayoutDef(layout_id=layout_id, name=name, locale_tags=deduped)


def _parse_dictionary(obj: dict[str, Any], source: Path) -> DictionaryDef:
    dictionary_id = str(obj.get("dictionaryId", "")).strip()
    if not dictionary_id:
        raise ValueError(f"{source}: missing dictionaryId")
    name = obj.get("name")
    if name is not None:
        name = str(name)
    raw_locales = obj.get("localeTags", [])
    if raw_locales is None:
        raw_locales = []
    if not isinstance(raw_locales, list):
        raise ValueError(f"{source}: localeTags must be a list")
    normalized = [_normalize_locale_tag(str(x)) for x in raw_locales]
    normalized = [x for x in normalized if x]
    deduped = tuple(dict.fromkeys(normalized).keys())

    enabled = bool(obj.get("enabled", True))
    priority = int(obj.get("priority", 0))

    raw_layout_ids = obj.get("layoutIds", [])
    if raw_layout_ids is None:
        raw_layout_ids = []
    if not isinstance(raw_layout_ids, list):
        raise ValueError(f"{source}: layoutIds must be a list")
    layout_ids = tuple([str(x).strip() for x in raw_layout_ids if str(x).strip()])

    return DictionaryDef(
        dictionary_id=dictionary_id,
        name=name,
        locale_tags=deduped,
        layout_ids=layout_ids,
        enabled=enabled,
        priority=priority,
        source=source,
    )


def generate(layouts: list[LayoutDef], dictionaries: list[DictionaryDef]) -> dict[str, Any]:
    """
    Final scheme2: generate a "locale -> layoutIds[]" mapping table.
    Dictionary/decoder selection is a runtime concern (driven by mode and layout).
    """
    # Collect mapping from localeTag -> layoutIds
    locale_to_layouts: dict[str, list[str]] = {}
    for layout in layouts:
        # If a layout has no locale tags, it is language-agnostic; do not attach it to any locale.
        # (It can still be reached via explicit layout switching, e.g. numeric.)
        for tag in layout.locale_tags:
            locale_to_layouts.setdefault(tag, []).append(layout.layout_id)

    # Stable + unique
    locales: list[dict[str, Any]] = []
    for tag in sorted(locale_to_layouts.keys()):
        ids = sorted(dict.fromkeys(locale_to_layouts[tag]).keys())
        locales.append(
            {
                "localeTag": tag,
                "layoutIds": ids,
                "defaultLayoutId": ids[0] if ids else None,
                "enabled": True,
                "priority": 0,
            }
        )

    return {"version": 3, "locales": locales}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Generate subtype JSON from layouts and dictionary definitions.")
    parser.add_argument("--layouts-dir", required=True, type=Path, help="Directory containing layout JSON files.")
    parser.add_argument(
        "--dictionary-dir",
        required=True,
        action="append",
        type=Path,
        help="Directory containing dictionary meta JSON files. Can be specified multiple times.",
    )
    parser.add_argument("--output", required=True, type=Path, help="Output JSON path (SubtypePack schema).")
    parser.add_argument("--fail-on-empty", action="store_true", help="Fail if no subtypes are generated.")
    args = parser.parse_args(argv)

    layout_files = _iter_json_files(args.layouts_dir)
    dict_files: list[Path] = []
    for d in args.dictionary_dir:
        dict_files.extend(_iter_json_files(d))

    layouts: list[LayoutDef] = []
    for f in layout_files:
        obj = _load_json(f)
        if not isinstance(obj, dict):
            raise SystemExit(f"{f}: layout json root must be an object")
        layouts.append(_parse_layout(obj, f))

    dictionaries: list[DictionaryDef] = []
    for f in dict_files:
        obj = _load_json(f)
        if not isinstance(obj, dict):
            raise SystemExit(f"{f}: dictionary json root must be an object")
        dictionaries.append(_parse_dictionary(obj, f))

    # De-duplicate dictionaries by dictionaryId, last one wins.
    # This enables a workflow where a manually maintained JSON (e.g. under src/main/assets)
    # overrides an auto-generated draft JSON (e.g. under build/generated/...).
    deduped: dict[str, DictionaryDef] = {}
    for d in dictionaries:
        prev = deduped.get(d.dictionary_id)
        if prev is not None and prev.source != d.source:
            print(
                f"warning: duplicate dictionaryId={d.dictionary_id!r}; "
                f"using {d.source} over {prev.source}",
                file=os.sys.stderr,
            )
        deduped[d.dictionary_id] = d
    dictionaries = list(deduped.values())

    pack = generate(layouts, dictionaries)
    if args.fail_on_empty and not pack.get("locales"):
        raise SystemExit("No locales generated (check layouts locale fields).")

    args.output.parent.mkdir(parents=True, exist_ok=True)
    tmp = args.output.with_suffix(args.output.suffix + ".tmp")
    with tmp.open("w", encoding="utf-8") as f:
        json.dump(pack, f, ensure_ascii=False, indent=2)
        f.write("\n")
    os.replace(tmp, args.output)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
