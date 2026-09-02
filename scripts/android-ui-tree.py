#!/usr/bin/env python3
"""Convert an Android UIAutomator XML dump into a compact, coordinate-rich tree."""

import argparse
import re
import sys
import xml.etree.ElementTree as ET


BOUNDS = re.compile(r"\[(\d+),(\d+)]\[(\d+),(\d+)]")


def one_line(value: str) -> str:
    return " ".join(value.split()).replace('"', "'")


def enabled(attribute: str) -> bool:
    return attribute == "true"


def describe(node: ET.Element, depth: int, actionable_only: bool) -> list[str]:
    attributes = node.attrib
    visible = attributes.get("visible-to-user", "true") != "false"
    actionable = visible and any(
        enabled(attributes.get(name, "false"))
        for name in ("clickable", "long-clickable", "scrollable", "checkable", "focusable")
    )
    bounds = attributes.get("bounds", "[0,0][0,0]")
    match = BOUNDS.fullmatch(bounds)
    if match:
        left, top, right, bottom = (int(value) for value in match.groups())
    else:
        left = top = right = bottom = 0

    flags = []
    for source, label in (
        ("clickable", "clickable"),
        ("long-clickable", "long-clickable"),
        ("scrollable", "scrollable"),
        ("checkable", "checkable"),
        ("checked", "checked"),
        ("focusable", "focusable"),
        ("focused", "focused"),
        ("selected", "selected"),
    ):
        if enabled(attributes.get(source, "false")):
            flags.append(label)
    flags.append("enabled" if enabled(attributes.get("enabled", "false")) else "disabled")

    output = []
    if not actionable_only or actionable:
        class_name = attributes.get("class", "View").rsplit(".", 1)[-1]
        resource_id = attributes.get("resource-id", "").rsplit("/", 1)[-1] or "-"
        line = (
            f"{'  ' * depth}{bounds} center={(left + right) // 2},{(top + bottom) // 2} "
            f"size={right - left}x{bottom - top} flags={','.join(flags)} "
            f"class={class_name} id={resource_id}"
        )
        text = one_line(attributes.get("text", ""))
        description = one_line(attributes.get("content-desc", ""))
        if text:
            line += f' text="{text}"'
        if description:
            line += f' desc="{description}"'
        output.append(line)

    for child in node:
        output.extend(describe(child, depth + 1, actionable_only))
    return output


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--actionable", action="store_true", help="only print interactive nodes")
    options = parser.parse_args()
    try:
        root = ET.fromstring(sys.stdin.read())
    except ET.ParseError as error:
        print(f"Could not parse UI hierarchy: {error}", file=sys.stderr)
        return 2

    print("# Android active-window tree")
    print("# depth bounds center size flags class id text description")
    for line in describe(root, depth=0, actionable_only=options.actionable):
        print(line)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
