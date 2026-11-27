#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
从 Android 版 Kotlin 文案表生成 ArkTS 全量文案。
用法：python tools/i18n-gen/gen.py
"""

from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
KOTLIN_DIR = ROOT.parent / "app" / "src" / "main" / "java" / "com" / "daphile" / "remote" / "ui" / "i18n"
OUT_FILE = ROOT / "entry" / "src" / "main" / "ets" / "ui" / "i18n" / "GeneratedStrings.ets"

FILES = ["Strings.kt", "StringsIntl.kt", "StringsIntl2.kt"]

# ArkTS 常量名 -> Kotlin 变量名
LANGUAGES = [
    ("zh", "zh"),
    ("en", "en"),
    ("zhTW", "zhTW"),
    ("ja", "ja"),
    ("ko", "ko"),
    ("koKp", "koKp"),
    ("ru", "ru"),
    ("de", "de"),
    ("fr", "fr"),
    ("es", "es"),
    ("it", "it"),
    ("pt", "pt"),
    ("nl", "nl"),
    ("el", "el"),
    ("vi", "vi"),
    ("th", "th"),
    ("ar", "ar"),
    ("az", "az"),
    ("fa", "fa"),
]


def read_sources() -> dict[str, str]:
    result: dict[str, str] = {}
    for name in FILES:
        result[name] = (KOTLIN_DIR / name).read_text(encoding="utf-8")
    return result


def find_class_fields(src: str, class_name: str) -> list[str]:
    match = re.search(r"^data class " + re.escape(class_name) + r"\(([\s\S]*?)^\)",
                      src, re.MULTILINE)
    if match is None:
        raise RuntimeError(f"未找到 data class {class_name}")
    body = match.group(1)
    fields = re.findall(r"^\s*(?:val|var)\s+(\w+)\s*:", body, re.MULTILINE)
    return [field for field in fields if field != "extra"]


def extract_paren_block(src: str, open_index: int) -> str:
    depth = 0
    in_string = False
    escaped = False
    index = open_index
    start = open_index + 1
    while index < len(src):
        ch = src[index]
        if in_string:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_string = False
        else:
            if ch == '"':
                in_string = True
            elif ch == "(":
                depth += 1
            elif ch == ")":
                depth -= 1
                if depth == 0:
                    return src[start:index]
        index += 1
    raise RuntimeError("括号未闭合")


def find_decl_block(src: str, var_name: str, call_name: str) -> str | None:
    pattern = re.compile(
        r"^\s*(?:internal|private)?\s*val\s+" + re.escape(var_name) +
        r"\s*(?::\s*\w+)?\s*=\s*" + re.escape(call_name) + r"\(",
        re.MULTILINE)
    match = pattern.search(src)
    if match is None:
        return None
    open_index = src.find("(", match.start())
    return extract_paren_block(src, open_index)


def find_copy_block(src: str, var_name: str, base_name: str) -> str | None:
    pattern = re.compile(
        r"^\s*(?:internal|private)?\s*val\s+" + re.escape(var_name) +
        r"\s*(?::\s*\w+)?\s*=\s*" + re.escape(base_name) + r"\.copy\(",
        re.MULTILINE)
    match = pattern.search(src)
    if match is None:
        return None
    open_index = src.find("(", match.start())
    return extract_paren_block(src, open_index)


ASSIGN_RE = re.compile(r"(\w+)\s*=\s*\"((?:[^\"\\]|\\.)*)\"", re.DOTALL)


def unescape_kotlin(value: str) -> str:
    value = re.sub(r"\\u([0-9a-fA-F]{4})", lambda m: chr(int(m.group(1), 16)), value)
    mapping = {
        "n": "\n",
        "t": "\t",
        "r": "\r",
        '"': '"',
        "\\": "\\",
        "$": "$",
        "'": "'",
    }
    return re.sub(r"\\(.)", lambda m: mapping.get(m.group(1), m.group(1)), value)


def parse_assignments(block: str | None) -> dict[str, str]:
    if block is None:
        return {}
    result: dict[str, str] = {}
    for match in ASSIGN_RE.finditer(block):
        result[match.group(1)] = unescape_kotlin(match.group(2))
    return result


def arkts_quote(value: str) -> str:
    escaped = value.replace("\\", "\\\\").replace("'", "\\'")
    escaped = escaped.replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t")
    return "'" + escaped + "'"


def main() -> None:
    sources = read_sources()
    strings_kt = sources["Strings.kt"]

    ui_fields = find_class_fields(strings_kt, "UiStrings")
    extra_fields = find_class_fields(strings_kt, "ExtraStrings")
    all_fields = ui_fields + extra_fields
    if len(set(all_fields)) != len(all_fields):
        duplicates = sorted({f for f in all_fields if all_fields.count(f) > 1})
        raise RuntimeError(f"UiStrings 与 ExtraStrings 存在重复字段：{duplicates}")

    en_main = parse_assignments(find_decl_block(strings_kt, "en", "UiStrings"))
    en_extra = parse_assignments(find_decl_block(strings_kt, "enExtra", "ExtraStrings"))
    zh_main = parse_assignments(find_decl_block(strings_kt, "zh", "UiStrings"))
    zh_extra = parse_assignments(find_decl_block(strings_kt, "zhExtra", "ExtraStrings"))

    base_en: dict[str, str] = {}
    base_en.update(en_main)
    base_en.update(en_extra)

    base_zh: dict[str, str] = {}
    base_zh.update(zh_main)
    base_zh.update(zh_extra)

    # 为每种语言收集 copy 覆盖与 ExtraStrings
    language_maps: dict[str, dict[str, str]] = {
        "zh": base_zh,
        "en": base_en,
    }

    for ark_name, kotlin_name in LANGUAGES:
        if ark_name in ("zh", "en"):
            continue
        copy_block = find_copy_block(strings_kt, kotlin_name, "en")
        base_name = "en"
        source_text = strings_kt
        if copy_block is None:
            for file_name in ("StringsIntl.kt", "StringsIntl2.kt"):
                text = sources[file_name]
                copy_block = find_copy_block(text, kotlin_name, "en")
                if copy_block is not None:
                    source_text = text
                    break
        if copy_block is None:
            for file_name in ("StringsIntl.kt", "StringsIntl2.kt"):
                text = sources[file_name]
                copy_block = find_copy_block(text, kotlin_name, "ko")
                if copy_block is not None:
                    source_text = text
                    base_name = "ko"
                    break
        if copy_block is None:
            raise RuntimeError(f"未找到 {kotlin_name} 的 copy 块")

        extra_name = f"{kotlin_name}Extra"
        extra_block = None
        for file_name in ("Strings.kt", "StringsIntl.kt", "StringsIntl2.kt"):
            text = sources[file_name]
            extra_block = find_decl_block(text, extra_name, "ExtraStrings")
            if extra_block is not None:
                break
        if extra_block is None:
            raise RuntimeError(f"未找到 {extra_name}")

        base_map = dict(language_maps[base_name])
        overrides = parse_assignments(copy_block)
        overrides.pop("extra", None)
        base_map.update(overrides)
        base_map.update(parse_assignments(extra_block))
        language_maps[ark_name] = base_map

    lines: list[str] = []
    lines.append("// 本文件由 tools/i18n-gen/gen.py 自动生成，请勿手改。")
    lines.append("")
    lines.append("export interface UiStrings {")
    for field in all_fields:
        lines.append(f"  {field}: string;")
    lines.append("}")
    lines.append("")

    for ark_name, _ in LANGUAGES:
        values = language_maps[ark_name]
        missing = [field for field in all_fields if field not in values or values[field] == ""]
        if missing:
            raise RuntimeError(f"{ark_name} 缺少字段：{missing[:20]}（共 {len(missing)} 个）")
        lines.append(f"export const {ark_name}: UiStrings = {{")
        for field in all_fields:
            lines.append(f"  {field}: {arkts_quote(values[field])},")
        lines.append("};")
        lines.append("")

    OUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    OUT_FILE.write_text("\n".join(lines), encoding="utf-8")

    summary = {
        "output": str(OUT_FILE),
        "languages": len(LANGUAGES),
        "uiFields": len(ui_fields),
        "extraFields": len(extra_fields),
        "totalFields": len(all_fields),
        "lines": len(lines),
    }
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
