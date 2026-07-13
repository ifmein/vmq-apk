#!/usr/bin/env bash
set -euo pipefail

VERSION_FILE="${VERSION_FILE:-version.properties}"
GRADLE="${GRADLE:-./gradlew}"
REMOTE="${REMOTE:-origin}"
BUMP_PART="${BUMP_PART:-patch}"
DRY_RUN=0
SKIP_TESTS=0

usage() {
    printf '%s\n' \
        'Usage:' \
        '  scripts/release.sh [x.y.z] [--dry-run] [--skip-tests] [--remote <name>] [--bump <patch|minor|major>]' \
        '  scripts/release.sh current' \
        '  scripts/release.sh version-code <x.y.z>' \
        '  scripts/release.sh bump <x.y.z> [--dry-run]' \
        '  scripts/release.sh release <x.y.z> [--dry-run] [--skip-tests]' \
        '  scripts/release.sh tag <x.y.z> [--dry-run]' \
        '  scripts/release.sh publish [x.y.z] [--dry-run] [--skip-tests] [--remote <name>] [--bump <patch|minor|major>]' \
        '' \
        'Default behavior:' \
        '  - If x.y.z is provided, publish that version.' \
        '  - If x.y.z is omitted, auto-bump the current version (default: patch).' \
        '  - Publish means: bump version, run tests, build release APK, commit version.properties, tag, push branch, and push tag.'
}

print_command() {
    printf '+'
    for arg in "$@"; do
        printf ' %q' "$arg"
    done
    printf '\n'
}

run_command() {
    print_command "$@"
    if [[ "$DRY_RUN" == "1" ]]; then
        return 0
    fi
    "$@"
}

require_version_file() {
    if [[ ! -f "$VERSION_FILE" ]]; then
        printf 'Version file not found: %s\n' "$VERSION_FILE" >&2
        exit 1
    fi
}

validate_version() {
    local version="$1"
    if [[ ! "$version" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
        printf 'Invalid version: %s\n' "$version" >&2
        printf 'Expected semantic version format: x.y.z\n' >&2
        exit 1
    fi
}

validate_bump_part() {
    local part="$1"
    case "$part" in
        patch|minor|major)
            ;;
        *)
            printf 'Invalid bump part: %s\n' "$part" >&2
            printf 'Expected one of: patch, minor, major\n' >&2
            exit 1
            ;;
    esac
}

version_code_for() {
    local version="$1"
    validate_version "$version"
    local major minor patch
    IFS='.' read -r major minor patch <<< "$version"
    printf '%d\n' $((major * 1000000 + minor * 1000 + patch))
}

current_version_name() {
    rg '^VERSION_NAME=' "$VERSION_FILE" -N | head -n1 | cut -d'=' -f2-
}

current_version_code() {
    rg '^VERSION_CODE=' "$VERSION_FILE" -N | head -n1 | cut -d'=' -f2-
}

next_version_for() {
    local version="$1"
    local part="$2"
    validate_version "$version"
    validate_bump_part "$part"

    local major minor patch
    IFS='.' read -r major minor patch <<< "$version"

    case "$part" in
        patch)
            patch=$((patch + 1))
            ;;
        minor)
            minor=$((minor + 1))
            patch=0
            ;;
        major)
            major=$((major + 1))
            minor=0
            patch=0
            ;;
    esac

    printf '%s.%s.%s\n' "$major" "$minor" "$patch"
}

tag_exists() {
    local version="$1"
    git rev-parse "v${version}" >/dev/null 2>&1
}

resolve_version() {
    local requested_version="${1:-}"
    if [[ -n "$requested_version" ]]; then
        validate_version "$requested_version"
        printf '%s\n' "$requested_version"
        return 0
    fi

    local candidate
    candidate="$(next_version_for "$(current_version_name)" "$BUMP_PART")"

    ensure_git_repo
    while tag_exists "$candidate"; do
        candidate="$(next_version_for "$candidate" "$BUMP_PART")"
    done

    printf '%s\n' "$candidate"
}

update_versions() {
    local version="$1"
    local version_code="$2"

    if [[ "$DRY_RUN" == "1" ]]; then
        printf 'Would update %s\n' "$VERSION_FILE"
        printf '  versionName: %s -> %s\n' "$(current_version_name)" "$version"
        printf '  versionCode: %s -> %s\n' "$(current_version_code)" "$version_code"
        return 0
    fi

    printf 'VERSION_NAME=%s\nVERSION_CODE=%s\n' "$version" "$version_code" > "$VERSION_FILE"
}

ensure_git_repo() {
    if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
        printf 'Not inside a git repository\n' >&2
        exit 1
    fi
}

ensure_clean_worktree() {
    ensure_git_repo
    if [[ -n "$(git status --porcelain)" ]]; then
        if [[ "$DRY_RUN" == "1" ]]; then
            printf 'Working tree is not clean. Dry-run will continue without publishing.\n' >&2
            return 0
        fi
        printf 'Working tree is not clean. Commit or stash changes before publishing.\n' >&2
        exit 1
    fi
}

ensure_tag_absent() {
    local version="$1"
    local tag_name="v${version}"
    if tag_exists "$version"; then
        printf 'Tag already exists: %s\n' "$tag_name" >&2
        exit 1
    fi
}

ensure_version_changed() {
    local version="$1"
    local current_version
    current_version="$(current_version_name)"
    if [[ "$version" == "$current_version" ]]; then
        printf 'Target version matches current version: %s\n' "$version" >&2
        exit 1
    fi
}

run_release() {
    local version="$1"
    local version_code="$2"

    ensure_version_changed "$version"
    update_versions "$version" "$version_code"

    if [[ "$SKIP_TESTS" != "1" ]]; then
        run_command "$GRADLE" testDebugUnitTest
    fi

    run_command "$GRADLE" assembleRelease
}

create_git_tag() {
    local version="$1"
    local tag_name="v${version}"
    run_command git tag -a "$tag_name" -m "Release ${tag_name}"
}

publish_release() {
    local version="$1"
    local version_code="$2"

    ensure_clean_worktree
    ensure_tag_absent "$version"
    run_release "$version" "$version_code"
    run_command git add "$VERSION_FILE"
    run_command git commit -m "release: v${version}"
    create_git_tag "$version"
    run_command git push "$REMOTE" HEAD
    run_command git push "$REMOTE" "v${version}"
}

parse_common_options() {
    while [[ $# -gt 0 ]]; do
        case "$1" in
            --dry-run)
                DRY_RUN=1
                ;;
            --skip-tests)
                SKIP_TESTS=1
                ;;
            --remote)
                shift || true
                if [[ $# -eq 0 || -z "$1" ]]; then
                    printf 'Missing value for --remote\n' >&2
                    exit 1
                fi
                REMOTE="$1"
                ;;
            --bump)
                shift || true
                if [[ $# -eq 0 || -z "$1" ]]; then
                    printf 'Missing value for --bump\n' >&2
                    exit 1
                fi
                validate_bump_part "$1"
                BUMP_PART="$1"
                ;;
            *)
                printf 'Unknown option: %s\n' "$1" >&2
                usage
                exit 1
                ;;
        esac
        shift
    done
}

main() {
    require_version_file

    local command="${1:-}"
    case "$command" in
        '' )
            local version
            version="$(resolve_version)"
            local version_code
            version_code="$(version_code_for "$version")"
            publish_release "$version" "$version_code"
            ;;
        current)
            printf 'versionName=%s\n' "$(current_version_name)"
            printf 'versionCode=%s\n' "$(current_version_code)"
            ;;
        version-code)
            shift || true
            local version_for_code="${1:-}"
            if [[ -z "$version_for_code" ]]; then
                usage
                exit 1
            fi
            version_code_for "$version_for_code"
            ;;
        bump|release|tag)
            shift || true
            local explicit_version="${1:-}"
            if [[ -z "$explicit_version" ]]; then
                usage
                exit 1
            fi
            shift || true
            parse_common_options "$@"

            local explicit_version_code
            explicit_version_code="$(version_code_for "$explicit_version")"

            if [[ "$command" == "bump" ]]; then
                ensure_version_changed "$explicit_version"
                update_versions "$explicit_version" "$explicit_version_code"
            elif [[ "$command" == "release" ]]; then
                run_release "$explicit_version" "$explicit_version_code"
            else
                ensure_tag_absent "$explicit_version"
                create_git_tag "$explicit_version"
            fi
            ;;
        publish)
            shift || true
            local publish_version="${1:-}"
            if [[ -n "$publish_version" && "$publish_version" == --* ]]; then
                publish_version=''
            else
                shift || true
            fi
            parse_common_options "$@"

            local resolved_publish_version
            resolved_publish_version="$(resolve_version "$publish_version")"
            local publish_version_code
            publish_version_code="$(version_code_for "$resolved_publish_version")"
            publish_release "$resolved_publish_version" "$publish_version_code"
            ;;
        -h|--help)
            usage
            ;;
        *)
            if [[ "$command" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
                shift || true
                parse_common_options "$@"

                local direct_version="$command"
                local direct_version_code
                direct_version_code="$(version_code_for "$direct_version")"
                publish_release "$direct_version" "$direct_version_code"
            elif [[ "$command" == --* ]]; then
                parse_common_options "$@"

                local auto_version
                auto_version="$(resolve_version)"
                local auto_version_code
                auto_version_code="$(version_code_for "$auto_version")"
                publish_release "$auto_version" "$auto_version_code"
            else
                printf 'Unknown command: %s\n' "$command" >&2
                usage
                exit 1
            fi
            ;;
    esac
}

main "$@"
