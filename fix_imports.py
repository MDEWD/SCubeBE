import os
import re

BASE_PATH = "src/main/java"
BASE_PKG_DIR = "src/main/java/com/scube/scubebackend"

# Mapping of ClassName -> Full Package
# We will build this by scanning the directory
class_location_map = {}

def scan_classes():
    print("Scanning classes...")
    for root, dirs, files in os.walk(BASE_PATH):
        for file in files:
            if file.endswith(".java") and file != "package-info.java":
                classname = file[:-5]

                # Determine package from path
                rel_path = os.path.relpath(root, BASE_PATH)
                package_name = rel_path.replace(os.path.sep, ".")

                full_class_name = f"{package_name}.{classname}"
                class_location_map[classname] = full_class_name
                # print(f"Found {classname} -> {full_class_name}")

def fix_imports_in_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    new_lines = []

    # Analyze the file content to find USED classes (simple regex approach)
    # We look for Capitalized words which are likely classes.
    content = "".join(lines)

    # Find package declaration of current file
    current_package = ""
    for line in lines:
        if line.strip().startswith("package "):
            current_package = line.strip().replace("package ", "").replace(";", "")
            break

    # Collect existing imports
    existing_imports = set()
    for line in lines:
        if line.strip().startswith("import "):
            imp = line.strip().replace("import ", "").replace(";", "")
            existing_imports.add(imp)

    # Find potential class usages (Whole words starting with uppercase)
    # This is a heuristic.
    used_classes = set(re.findall(r'\b[A-Z][a-zA-Z0-9]*\b', content))

    # Filter usages:
    # 1. Ignore keywords/java built-ins if they conflict (though usually unlikely with Capitalized)
    # 2. Ignore classes in the same package (don't need import)
    # 3. Ignore classes already imported fully

    needed_imports = set()

    for cls in used_classes:
        if cls in class_location_map:
            full_params = class_location_map[cls]
            cls_package = full_params.rsplit('.', 1)[0]

            # If same package, skip
            if cls_package == current_package:
                continue

            # If already imported, skip
            if full_params in existing_imports:
                continue

            # Check for conflict? (multiple classes with same name) - strict mapping assumes unique names for now
            # or simply add it.

            # Should we add it?
            # Yes, if we can match it.
            needed_imports.add(full_params)

    # Now rewrite the file
    # We will remove invalid wildcard imports and add missing specific imports

    # Specific problematic wildcard imports to remove
    bad_wildcards = [
        "com.scube.scubebackend.mapper.*",
        "com.scube.scubebackend.model.entity.*",
        "com.scube.scubebackend.model.dto.*",
        "com.scube.scubebackend.service.*" # If there are any
    ]

    imports_modified = False

    final_lines = []
    last_import_idx = -1

    for i, line in enumerate(lines):
        stripped = line.strip()

        # Remove bad wildcards
        is_bad = False
        if stripped.startswith("import "):
            for bad in bad_wildcards:
                if stripped.startswith(f"import {bad}"):
                    is_bad = True
                    imports_modified = True
                    break

        if is_bad:
            continue

        final_lines.append(line)
        if stripped.startswith("package ") or stripped.startswith("import "):
            last_import_idx = len(final_lines) - 1

    # Insert needed imports
    if needed_imports:
        # Sort for cleanliness
        sorted_imports = sorted(list(needed_imports))

        insertion_point = last_import_idx + 1 if last_import_idx != -1 else 1

        added_block = []
        for imp in sorted_imports:
            # check if we already have it (maybe it was a valid import we kept)
            already_there = False
            for line in final_lines:
                if f"import {imp};" in line:
                    already_there = True
                    break

            if not already_there:
                added_block.append(f"import {imp};\n")
                imports_modified = True

        if added_block:
            final_lines[insertion_point:insertion_point] = added_block

    if imports_modified:
        print(f"Fixing imports for {filepath}")
        with open(filepath, 'w', encoding='utf-8') as f:
            f.writelines(final_lines)

def main():
    scan_classes()

    # Walk through all java files and fix imports
    for root, dirs, files in os.walk(BASE_PATH):
        for file in files:
            if file.endswith(".java") and file != "package-info.java":
                filepath = os.path.join(root, file)
                fix_imports_in_file(filepath)

if __name__ == "__main__":
    main()

