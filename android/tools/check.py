#!/usr/bin/env python3
"""
Pre-CI structural checks for the Kotlin sources.

There is no Android SDK in this environment — dl.google.com is unreachable — so CI is the
only thing that actually compiles this project. That makes the feedback loop minutes long,
and every class of error caught here is a round trip saved.

This does not type-check. It catches the specific mistakes that have actually reached CI:

  1. Unbalanced braces/parens/brackets, ignoring comments and string literals.
  2. Imports of `com.debubble.*` symbols that no file declares — the usual aftermath of
     renaming or deleting a component.
  3. Modifier.padding() called with a mix of the {horizontal, vertical} overload and the
     {start, top, end, bottom} one. There is no such overload, and the compiler reports it
     as "None of the following candidates is applicable" pointing at the call, which reads
     like an unrelated problem with whatever composable is nearby.

Run: python3 android/tools/check.py
"""

import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "app", "src")


def strip_code(src: str) -> str:
    """Drop comments and string literals so brace counting is not fooled by them."""
    out = []
    i, n, depth = 0, len(src), 0
    while i < n:
        c = src[i]
        if depth == 0 and src.startswith("//", i):
            j = src.find("\n", i)
            i = n if j < 0 else j
            continue
        if src.startswith("/*", i):
            depth += 1
            i += 2
            continue
        if src.startswith("*/", i) and depth > 0:
            depth -= 1
            i += 2
            continue
        if depth > 0:
            i += 1
            continue
        if c == '"':
            if src.startswith('"""', i):
                j = src.find('"""', i + 3)
                i = n if j < 0 else j + 3
                continue
            i += 1
            while i < n and src[i] != '"':
                if src[i] == "\\":
                    i += 1
                i += 1
            i += 1
            continue
        if c == "'":
            i += 1
            while i < n and src[i] != "'":
                if src[i] == "\\":
                    i += 1
                i += 1
            i += 1
            continue
        out.append(c)
        i += 1
    return "".join(out)


def kotlin_files():
    for root, _, names in os.walk(ROOT):
        for name in sorted(names):
            if name.endswith(".kt"):
                yield os.path.join(root, name)


def check_balance(files):
    problems = []
    for path in files:
        code = strip_code(open(path).read())
        for open_c, close_c, label in (("{", "}", "brace"), ("(", ")", "paren"), ("[", "]", "bracket")):
            delta = code.count(open_c) - code.count(close_c)
            if delta:
                problems.append(f"{path}: {label} imbalance {delta:+d}")
    return problems


DECL = re.compile(
    r"^\s*(?:@\w+\s+)*(?:internal\s+|private\s+|public\s+)?(?:@Composable\s+)?"
    r"(?:data\s+|sealed\s+|enum\s+|value\s+|abstract\s+|open\s+)*"
    r"(?:class|object|interface|fun|val|var)\s+(?:<[^>]+>\s+)?([A-Za-z_][A-Za-z0-9_]*)",
    re.M,
)


def check_imports(files):
    declared = set()
    for path in files:
        src = open(path).read()
        declared.update(m.group(1) for m in DECL.finditer(src))
        # Extension declarations: `fun Modifier.panel(` and `val Pillar.accent:`.
        declared.update(
            m.group(1) for m in re.finditer(r"fun\s+[A-Za-z0-9_.<>]+\.([A-Za-z_]\w*)\s*\(", src)
        )
        declared.update(
            m.group(1) for m in re.finditer(r"val\s+[A-Za-z0-9_.<>]+\.([A-Za-z_]\w*)\s*:", src)
        )

    problems = []
    for path in files:
        for num, line in enumerate(open(path), 1):
            stripped = line.strip()
            if stripped.startswith("import com.debubble."):
                symbol = stripped.split(".")[-1]
                if symbol != "*" and symbol not in declared:
                    problems.append(f"{path}:{num}: imports '{symbol}', which nothing declares")
    return problems


# The two padding overloads are mutually exclusive; mixing them compiles to nothing.
EDGE_ARGS = re.compile(r"\b(start|top|end|bottom)\s*=")
AXIS_ARGS = re.compile(r"\b(horizontal|vertical)\s*=")


def check_padding(files):
    problems = []
    for path in files:
        for num, line in enumerate(open(path), 1):
            for call in re.finditer(r"padding\(([^()]*(?:\([^()]*\)[^()]*)*)\)", line):
                args = call.group(1)
                if AXIS_ARGS.search(args) and EDGE_ARGS.search(args):
                    problems.append(
                        f"{path}:{num}: padding() mixes the horizontal/vertical overload "
                        f"with start/top/end/bottom — there is no such overload"
                    )
    return problems


def main() -> int:
    files = list(kotlin_files())
    checks = (
        ("balance", check_balance),
        ("imports", check_imports),
        ("padding", check_padding),
    )
    failed = 0
    for name, fn in checks:
        problems = fn(files)
        if problems:
            failed += len(problems)
            print(f"\n=== {name}: {len(problems)} problem(s) ===")
            for p in problems:
                print(f"  {p}")
        else:
            print(f"{name}: ok")
    print(f"\n{len(files)} files checked, {failed} problem(s)")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
