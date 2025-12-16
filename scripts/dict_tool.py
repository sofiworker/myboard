#!/usr/bin/env python3
from __future__ import annotations

import argparse
import dataclasses
import json
import struct
import sys
import time
import zlib
from collections import defaultdict
from pathlib import Path
from typing import Iterable, Protocol


@dataclasses.dataclass(frozen=True, slots=True)
class DictionaryEntry:
    word: str
    code: str
    weight: int = 0


class DictionaryFormatParser(Protocol):
    format_id: str

    def parse(self, path: Path) -> Iterable[DictionaryEntry]:
        ...


class RimeDictYamlParser:
    """
    Parses Rime *.dict.yaml, including rime-ice (雾凇拼音) dictionaries.

    Expected entry lines (after YAML header):
      <word>\t<code>\t<weight?>
    """

    format_id = "rime_dict_yaml"

    def parse(self, path: Path) -> Iterable[DictionaryEntry]:
        with path.open("r", encoding="utf-8", errors="replace") as f:
            in_body = False
            for raw in f:
                line = raw.strip("\n")
                if not in_body:
                    if line.strip() == "...":
                        in_body = True
                    continue

                s = line.strip()
                if not s or s.startswith("#"):
                    continue

                # Rime dict lines are tab-separated; keep a whitespace fallback for robustness.
                parts = s.split("\t")
                if len(parts) < 2:
                    parts = s.split()
                if len(parts) < 2:
                    continue

                word = parts[0].strip()
                code = parts[1].strip()
                if not word or not code:
                    continue
                # Keep raw code as-is (incl. spaces/apostrophes) so higher-level converters can
                # canonicalize deterministically and optionally derive single-character entries.
                code = code.lower()

                weight = 0
                if len(parts) >= 3:
                    try:
                        weight = int(parts[2].strip())
                    except ValueError:
                        weight = 0

                yield DictionaryEntry(word=word, code=code, weight=weight)


class MyBoardDictPayloadV1Writer:
    """
    Compact dictionary payload v1 (MYBDICT1).

    Header (little-endian):
      magic[8] = b"MYBDICT1"
      u32 version = 1
      u32 flags = 0
      u32 code_count
      u32 entry_count
      u32 code_index_offset
      u32 entry_table_offset
      u32 code_blob_offset
      u32 word_blob_offset
      u32 payload_size
      code_index[code_count] of:
        u32 code_offset
        u32 first_entry_index
        u32 entry_count_for_code
      entry_table[entry_count] of:
        u32 word_offset
        i32 weight
      code_blob: NUL-terminated utf-8 strings
      word_blob: NUL-terminated utf-8 strings
    """

    MAGIC = b"MYBDICT1"
    VERSION = 1
    FLAGS = 0

    def encode(self, entries: Iterable[DictionaryEntry]) -> bytes:
        grouped: dict[str, list[DictionaryEntry]] = defaultdict(list)
        for e in entries:
            grouped[e.code].append(e)

        codes = sorted(grouped.keys())
        for code in codes:
            grouped[code].sort(key=lambda e: (-e.weight, e.word))

        code_blob_parts: list[bytes] = []
        code_offsets: dict[str, int] = {}
        code_blob_size = 0
        for code in codes:
            b = code.encode("utf-8") + b"\0"
            code_offsets[code] = code_blob_size
            code_blob_parts.append(b)
            code_blob_size += len(b)
        code_blob = b"".join(code_blob_parts)

        word_blob_parts: list[bytes] = []
        word_offsets: list[int] = []
        weights: list[int] = []
        word_blob_size = 0
        for code in codes:
            for e in grouped[code]:
                b = e.word.encode("utf-8") + b"\0"
                word_offsets.append(word_blob_size)
                weights.append(int(e.weight))
                word_blob_parts.append(b)
                word_blob_size += len(b)
        word_blob = b"".join(word_blob_parts)

        code_index: list[tuple[int, int, int]] = []
        first_entry_index = 0
        for code in codes:
            count = len(grouped[code])
            code_index.append((code_offsets[code], first_entry_index, count))
            first_entry_index += count

        entry_count = len(word_offsets)
        code_count = len(codes)

        code_index_offset = 8 + 4 * 9
        entry_table_offset = code_index_offset + code_count * 12
        code_blob_offset = entry_table_offset + entry_count * 8
        word_blob_offset = code_blob_offset + len(code_blob)
        payload_size = word_blob_offset + len(word_blob)

        out = bytearray()
        out += self.MAGIC
        out += struct.pack("<I", self.VERSION)
        out += struct.pack("<I", self.FLAGS)
        out += struct.pack("<II", code_count, entry_count)
        out += struct.pack(
            "<IIIII",
            code_index_offset,
            entry_table_offset,
            code_blob_offset,
            word_blob_offset,
            payload_size,
        )

        for code_offset, first, count in code_index:
            out += struct.pack("<III", code_offset, first, count)

        for off, w in zip(word_offsets, weights, strict=True):
            out += struct.pack("<Ii", off, w)

        out += code_blob
        out += word_blob
        if len(out) != payload_size:
            raise RuntimeError(f"payload_size mismatch: header={payload_size} actual={len(out)}")
        return bytes(out)


def _parse_semver(text: str) -> tuple[int, int, int]:
    parts = text.strip().split(".")
    if len(parts) != 3:
        raise ValueError(f"dict-version must be a.b.c, got: {text}")
    a = int(parts[0])
    b = int(parts[1])
    c = int(parts[2])
    for v in (a, b, c):
        if not (0 <= v <= 65535):
            raise ValueError(f"semver part out of range 0..65535: {text}")
    return a, b, c


def _pack_lang2(code2: str) -> int:
    s = (code2 or "").strip().lower()
    if len(s) < 2:
        s = (s + "un")[:2]
    return (ord(s[0]) & 0xFF) | ((ord(s[1]) & 0xFF) << 8)


class ScriptType:
    UNKNOWN = 0
    LATIN = 1
    HAN = 2
    HIRAGANA = 3
    KATAKANA = 4
    HANGUL = 5
    TIBETAN = 6
    ARABIC = 7
    THAI = 8
    DEVANAGARI = 9


class FeatureFlags:
    HAS_TONES = 1 << 0
    HAS_PITCH = 1 << 1
    HAS_DIACRITICS = 1 << 2
    REQUIRES_COMPOSE = 1 << 3
    IS_LOGGRAPHIC = 1 << 4
    IS_SYLLABIC = 1 << 5
    IS_ABJAD = 1 << 6
    HAS_CASE = 1 << 7
    RTL_WRITING = 1 << 8


def _derive_profile(primary_lang_tag: str | None) -> tuple[int, int, int, int]:
    """
    Returns (language_code_u16, region_code_u8, script_type_u8, feature_flags_u32).
    Inspired by `dictionary/a.cc` but simplified for current MyBoard needs.
    """
    tag = (primary_lang_tag or "").strip().replace("_", "-")
    parts = [p for p in tag.split("-") if p]
    language = parts[0].lower() if parts else ""
    region = parts[1].upper() if len(parts) > 1 else ""

    region_code = {
        "CN": 1,
        "TW": 2,
        "HK": 3,
        "MO": 4,
        "US": 10,
    }.get(region, 0)

    if language == "zh":
        return _pack_lang2("zh"), region_code, ScriptType.HAN, FeatureFlags.HAS_TONES | FeatureFlags.IS_LOGGRAPHIC
    if language == "ja":
        return _pack_lang2("ja"), region_code, ScriptType.HIRAGANA, FeatureFlags.HAS_PITCH | FeatureFlags.IS_SYLLABIC
    if language == "ko":
        return _pack_lang2("ko"), region_code, ScriptType.HANGUL, FeatureFlags.REQUIRES_COMPOSE | FeatureFlags.IS_SYLLABIC
    if language == "ar":
        return _pack_lang2("ar"), region_code, ScriptType.ARABIC, FeatureFlags.RTL_WRITING | FeatureFlags.IS_ABJAD

    lang2 = language[:2] if language else "un"
    return _pack_lang2(lang2), region_code, ScriptType.LATIN, FeatureFlags.HAS_CASE


class MyBoardDictionaryFileV1Writer:
    """
    MYBDF v1 container writer (MYBDF001).

    Header (little-endian, 64 bytes):
      magic[8] = b"MYBDF001"
      u32 version = 1
      u16 dict_ver_major
      u16 dict_ver_minor
      u16 dict_ver_patch
      u16 reserved0 = 0
      u16 language_code (2-letter ASCII packed)
      u8 region_code
      u8 script_type
      u32 feature_flags
      u32 flags (low 4 bits: compression id; 0=none, 1=zlib)
      u32 header_size (=64)
      u32 meta_size
      u32 payload_size_uncompressed
      u32 payload_size_stored
      u32 crc32_payload (over uncompressed payload)
      u32 crc32_header_meta (over [header+meta] with this field zeroed)
      reserved[8] = 0

    Then:
      meta JSON (UTF-8)
      payload bytes (raw or zlib-compressed)
    """

    MAGIC = b"MYBDF001"
    VERSION = 1

    def write(
        self,
        payload_uncompressed: bytes,
        out_path: Path,
        *,
        dict_version: tuple[int, int, int],
        meta: dict,
        languages: list[str],
        compression: str = "zlib",
    ) -> None:
        out_path.parent.mkdir(parents=True, exist_ok=True)
        compression_id = 0 if compression == "none" else 1
        payload_stored = payload_uncompressed if compression_id == 0 else zlib.compress(payload_uncompressed, 9)

        meta_obj = dict(meta)
        meta_obj["languages"] = list(languages)
        meta_json = json.dumps(meta_obj, ensure_ascii=False, separators=(",", ":")).encode("utf-8")

        (lang_code, region_code, script_type, feature_flags) = _derive_profile((languages or [""])[0])
        (a, b, c) = dict_version
        crc_payload = zlib.crc32(payload_uncompressed) & 0xFFFFFFFF

        # Build header with crc32_header_meta placeholder = 0.
        header = struct.pack(
            "<8sIHHHHHBBIIIIIIII8s",
            self.MAGIC,
            self.VERSION,
            a,
            b,
            c,
            0,
            lang_code,
            region_code & 0xFF,
            script_type & 0xFF,
            feature_flags & 0xFFFFFFFF,
            compression_id & 0xF,
            64,
            len(meta_json),
            len(payload_uncompressed),
            len(payload_stored),
            crc_payload,
            0,  # crc32_header_meta
            b"\0" * 8,
        )
        if len(header) != 64:
            raise RuntimeError(f"header size mismatch: {len(header)}")

        header_meta = header + meta_json
        # Zero crc32_header_meta field (offset 52..55) for checksum.
        header_meta = header_meta[:52] + b"\0\0\0\0" + header_meta[56:]
        crc_header_meta = zlib.crc32(header_meta) & 0xFFFFFFFF

        # Patch crc32_header_meta into header.
        header = header[:52] + struct.pack("<I", crc_header_meta) + header[56:]

        with out_path.open("wb") as f:
            f.write(header)
            f.write(meta_json)
            f.write(payload_stored)


class CodeScheme:
    """
    Canonical code scheme used inside MyBoard payload.
    Convert layer must normalize external codes into one of these schemes.
    """

    PINYIN_FULL = "PINYIN_FULL"


def _canonicalize_code(code: str, *, scheme: str) -> str:
    c = (code or "").strip()
    if not c:
        return ""
    if scheme == CodeScheme.PINYIN_FULL:
        # Canonical:
        # - lowercased
        # - remove spaces (syllable separators)
        # - remove apostrophes
        # - keep only a-z (strict, deterministic)
        c = c.lower().replace(" ", "").replace("'", "")
        c = "".join(ch for ch in c if "a" <= ch <= "z")
        return c
    raise ValueError(f"Unknown code scheme: {scheme}")


def _to_locale_tag_underscore(tag: str) -> str:
    """
    Normalizes a locale tag into underscore style used by existing assets (e.g. zh_CN).

    Note: `generate_subtypes.py` accepts both underscore and hyphen styles.
    """
    t = (tag or "").strip().replace("-", "_")
    if not t:
        return ""
    parts = [p for p in t.split("_") if p]
    if not parts:
        return ""
    language = parts[0].lower()
    region = parts[1].upper() if len(parts) > 1 else ""
    return f"{language}_{region}" if region else language


def _write_dictionary_spec_json(
    *,
    out_path: Path,
    dictionary_id: str,
    name: str | None,
    locale_tags: list[str],
    layout_ids: list[str],
    asset_path: str | None,
    code_scheme: str | None,
    kind: str | None,
    core: str | None,
    variant: str | None,
    is_default: bool,
    enabled: bool,
    priority: int,
) -> None:
    obj = {
        "dictionaryId": dictionary_id,
        "name": name,
        "localeTags": locale_tags,
        "layoutIds": layout_ids,
        "assetPath": asset_path,
        "codeScheme": code_scheme,
        "kind": kind,
        "core": core,
        "variant": variant,
        "isDefault": bool(is_default),
        "enabled": bool(enabled),
        "priority": int(priority),
    }
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def _parser_registry() -> dict[str, DictionaryFormatParser]:
    parsers: list[DictionaryFormatParser] = [
        RimeDictYamlParser(),
    ]
    return {p.format_id: p for p in parsers}


def _cmd_convert(argv: list[str]) -> int:
    p = argparse.ArgumentParser(
        prog="dict_tool.py convert",
        description="Convert source dictionaries into MyBoard .mybdict (MYBDF v1) files.",
    )
    p.add_argument("--input", required=True, type=Path, help="Input dictionary file.")
    p.add_argument("--format", required=True, help="Input format id (e.g. rime_dict_yaml).")
    p.add_argument("--output", required=True, type=Path, help="Output compact dictionary file.")
    p.add_argument("--dictionary-id", required=True, help="Dictionary id (matches DictionarySpec.dictionaryId).")
    p.add_argument("--name", default=None, help="Optional display name.")
    p.add_argument("--languages", default="", help="Comma-separated BCP-47 tags (e.g. zh-CN,en).")
    p.add_argument("--dict-version", default="1.0.0", help="Semantic version a.b.c (stored in MYBDF header).")
    p.add_argument("--meta-output", default=None, type=Path, help="Optional output DictionarySpec JSON path.")
    p.add_argument("--asset-path", default=None, help='Optional assetPath in DictionarySpec (e.g. "dictionary/base.mybdict").')
    p.add_argument("--layout-ids", default="", help="Comma-separated allowed layout ids (DictionarySpec.layoutIds).")
    p.add_argument("--code-scheme", default=CodeScheme.PINYIN_FULL, help="Canonical code scheme for payload (e.g. PINYIN_FULL).")
    p.add_argument(
        "--derive-single-chars",
        action="store_true",
        help="Derive single-character entries from multi-character words (pinyin only).",
    )
    p.add_argument(
        "--single-chars-per-code",
        default="64",
        help="Max derived single characters per syllable code (default: 64).",
    )
    p.add_argument("--kind", default=None, help="Optional DictionarySpec.kind (e.g. PINYIN).")
    p.add_argument("--core", default=None, help="Optional DictionarySpec.core (e.g. PINYIN_CORE).")
    p.add_argument("--variant", default=None, help="Optional DictionarySpec.variant (e.g. quanpin).")
    p.add_argument("--is-default", action="store_true", help="DictionarySpec.isDefault (default: false).")
    p.add_argument("--enabled", default="true", choices=["true", "false"], help="DictionarySpec.enabled (default: true).")
    p.add_argument("--priority", default="0", help="DictionarySpec.priority (default: 0).")
    p.add_argument(
        "--compress",
        choices=["none", "zlib"],
        default="zlib",
        help="Compression for output file (default: zlib).",
    )
    args = p.parse_args(argv)

    reg = _parser_registry()
    parser = reg.get(args.format)
    if parser is None:
        available = ", ".join(sorted(reg.keys()))
        raise SystemExit(f"Unknown format: {args.format} (available: {available})")

    languages = [s.strip() for s in str(args.languages).split(",") if s.strip()]
    locale_tags = [_to_locale_tag_underscore(s) for s in languages]
    locale_tags = [t for t in locale_tags if t]
    layout_ids = [s.strip() for s in str(args.layout_ids).split(",") if s.strip()]
    enabled = str(args.enabled).lower() == "true"
    priority = int(args.priority)
    meta = {
        "dictionaryId": args.dictionary_id,
        "name": args.name,
        "sourceFormat": args.format,
        "createdBy": "myboard_build",
        "createdAtEpochMs": int(time.time() * 1000),
        "codeScheme": str(args.code_scheme),
    }

    scheme = str(args.code_scheme)
    derive_single_chars = bool(args.derive_single_chars) or scheme == CodeScheme.PINYIN_FULL
    single_chars_per_code = max(0, int(args.single_chars_per_code))

    def _iter_canonical() -> Iterable[DictionaryEntry]:
        # For each single-syllable code, keep best-weight single characters.
        # Keeps the output size bounded (unlike emitting per-character entries for every word).
        char_best: dict[str, dict[str, int]] = defaultdict(dict)

        for e in parser.parse(args.input):
            raw_code = (e.code or "").strip()
            word = (e.word or "").strip()
            code = _canonicalize_code(raw_code, scheme=scheme)
            if not code or not word:
                continue

            if derive_single_chars and single_chars_per_code > 0 and scheme == CodeScheme.PINYIN_FULL:
                syllables = [s for s in raw_code.split() if s]
                if syllables and len(syllables) == len(word):
                    for ch, syl in zip(word, syllables, strict=True):
                        ch = ch.strip()
                        if not ch or len(ch) != 1:
                            continue
                        syl_code = _canonicalize_code(syl, scheme=scheme)
                        if not syl_code:
                            continue
                        # Normalize derived weight: use scaled weight so long phrases don't dominate.
                        derived_weight = int(e.weight / max(1, len(word)))
                        prev = char_best[syl_code].get(ch)
                        if prev is None or derived_weight > prev:
                            char_best[syl_code][ch] = derived_weight

            yield DictionaryEntry(word=word, code=code, weight=e.weight)

        if derive_single_chars and single_chars_per_code > 0 and scheme == CodeScheme.PINYIN_FULL:
            for syl_code, m in char_best.items():
                # Sort by weight desc then char for stable output.
                items = sorted(m.items(), key=lambda kv: (-kv[1], kv[0]))
                for ch, w in items[:single_chars_per_code]:
                    yield DictionaryEntry(word=ch, code=syl_code, weight=w)

    payload = MyBoardDictPayloadV1Writer().encode(_iter_canonical())
    MyBoardDictionaryFileV1Writer().write(
        payload_uncompressed=payload,
        out_path=args.output,
        dict_version=_parse_semver(args.dict_version),
        meta=meta,
        languages=languages,
        compression=args.compress,
    )

    if args.meta_output is not None:
        asset_path = args.asset_path or f"dictionary/{args.output.name}"
        _write_dictionary_spec_json(
            out_path=args.meta_output,
            dictionary_id=args.dictionary_id,
            name=args.name,
            locale_tags=locale_tags,
            layout_ids=layout_ids,
            asset_path=asset_path,
            code_scheme=str(args.code_scheme),
            kind=args.kind,
            core=args.core,
            variant=args.variant,
            is_default=bool(args.is_default),
            enabled=enabled,
            priority=priority,
        )
    return 0


def main(argv: list[str]) -> int:
    if not argv:
        raise SystemExit("Usage: dict_tool.py <command> [args...]; command=convert")

    cmd, *rest = argv
    if cmd == "convert":
        return _cmd_convert(rest)

    raise SystemExit(f"Unknown command: {cmd}")


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
