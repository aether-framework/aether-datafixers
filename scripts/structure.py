import os
import fnmatch
import argparse

def apply_color(text, color_code, use_colors):
    return f"\033[{color_code}m{text}\033[0m" if use_colors else text

def print_structure(dir_path, depth, file_pattern, use_colors, excluded_folders):
    try:
        entries = os.listdir(dir_path)
    except Exception:
        return

    if not entries:
        return

    full_paths = [os.path.join(dir_path, entry) for entry in entries]
    full_paths.sort(key=lambda p: os.path.isfile(p))

    for path in full_paths:
        name = os.path.basename(path)
        if name in {".", ".."} or name in excluded_folders:
            continue
        indent = " " * (depth * 4)
        if os.path.isdir(path):
            print(f"{indent}" + apply_color(f"📁 {name}", "34", use_colors))
            print_structure(path, depth + 1, file_pattern, use_colors, excluded_folders)
        elif fnmatch.fnmatch(name, file_pattern):
            print(f"{indent}  " + apply_color(f"📄 {name}", "32", use_colors))

def print_dependency_tree(dir_path, depth=0, file_pattern="*.java", use_colors=True, excluded_folders=set()):
    try:
        entries = os.listdir(dir_path)
    except Exception:
        return

    entries.sort()
    for entry in entries:
        full_path = os.path.join(dir_path, entry)
        if entry in excluded_folders or entry in {".", ".."}:
            continue
        indent = "|   " * depth + "|-- "
        if os.path.isdir(full_path):
            print(indent + apply_color(f"{entry}/", "34", use_colors))
            print_dependency_tree(full_path, depth + 1, file_pattern, use_colors, excluded_folders)
        elif fnmatch.fnmatch(entry, file_pattern):
            print(indent + apply_color(entry, "32", use_colors))

def export_structure_to_file(output_file, mode, file_pattern, root_path, excluded_folders):
    with open(output_file, "w", encoding="utf-8") as f:
        def write_structure(dir_path, depth):
            try:
                entries = os.listdir(dir_path)
            except Exception:
                return

            entries.sort()
            for entry in entries:
                full_path = os.path.join(dir_path, entry)
                if entry in excluded_folders or entry in {".", ".."}:
                    continue
                indent = "  " * depth
                if os.path.isdir(full_path):
                    f.write(f"{indent}{entry}/\n")
                    write_structure(full_path, depth + 1)
                elif fnmatch.fnmatch(entry, file_pattern):
                    f.write(f"{indent}{entry}\n")

        f.write("Package Structure:\n")
        if mode == "tree":
            f.write(f"ROOT ({root_path})\n")
            write_structure(root_path, 1)
        else:
            f.write(f"ROOT ({root_path})\n")
            write_structure(root_path, 1)

def print_package_structure(root_path, mode="default", file_pattern="*.java", output_file=None, use_colors=True, excluded_folders=set()):
    if not os.path.exists(root_path) or not os.path.isdir(root_path):
        print("❌ Root directory does not exist: " + root_path)
        return

    print(f"\n📂 Package Structure ({root_path}):")
    if mode == "tree":
        print("|-- ROOT")
        print_dependency_tree(root_path, 1, file_pattern, use_colors, excluded_folders)
    else:
        print("📁 ROOT")
        print_structure(root_path, 1, file_pattern, use_colors, excluded_folders)

    if output_file:
        export_structure_to_file(output_file, mode, file_pattern, root_path, excluded_folders)
        print(f"\n✅ Structure exported to {output_file}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Print the package structure of a Java project.")
    parser.add_argument("root_path", type=str, help="Root directory of the project")
    parser.add_argument("mode", nargs="?", choices=["default", "tree"], default="default", help="Output mode")
    parser.add_argument("--filter", type=str, default="*.java", help="Filter files by wildcard pattern (e.g., '*.java')")
    parser.add_argument("--output", type=str, help="Export output to a file")
    parser.add_argument("--no-color", action="store_true", help="Disable colored output")
    parser.add_argument("--exclude", type=str, nargs="*", default=["target", "build", ".git", "node_modules"], help="Folders to exclude")

    args = parser.parse_args()
    print_package_structure(args.root_path, args.mode, args.filter, args.output, not args.no_color, set(args.exclude))
