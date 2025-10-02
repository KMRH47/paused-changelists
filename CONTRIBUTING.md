# Contributing to Paused Changelists

Thank you for your interest in contributing! This document provides guidelines for contributing to this project.

## Commit Messages

This project uses [Conventional Commits](https://www.conventionalcommits.org/) for clear and structured commit history.

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- **feat**: A new feature
- **fix**: A bug fix
- **docs**: Documentation only changes
- **style**: Code style changes (formatting, missing semi-colons, etc.)
- **refactor**: Code changes that neither fix bugs nor add features
- **perf**: Performance improvements
- **test**: Adding or updating tests
- **chore**: Maintenance tasks, dependency updates, etc.
- **ci**: Changes to CI/CD configuration files and scripts

### Examples

```bash
# Bug fix
git commit -m "fix: resolve incorrect changelist selection when pausing files"

# New feature
git commit -m "feat: add keyboard shortcut customization"

# Documentation
git commit -m "docs: update installation instructions"

# Breaking change
git commit -m "feat!: redesign pause mechanism

BREAKING CHANGE: Paused changelists now use a different shelf naming convention."
```

### Multi-line Commits

For detailed commits, use a heredoc:

```bash
git commit -m "$(cat <<'EOF'
fix: resolve incorrect changelist selection when pausing files/folders

Previously, when right-clicking and pausing files or folders within a 
changelist (instead of the changelist header itself), the plugin would 
incorrectly default to marking the "Changes" changelist as paused.

This fix enhances the changelist selection logic to:
- First attempt to get the directly selected changelist
- Then check which changelist contains the selected changes/files
- Only return null if neither is found

Fixes #123
EOF
)"
```

## Releasing

To create a new release and trigger the automated build and publish workflow:

### 1. Update Version

Update the version in `gradle.properties`:

```properties
pluginVersion=0.1.2
```

Also update the changelog in `build.gradle.kts`:

```kotlin
changeNotes.set(
    """
    <h3>0.1.2</h3>
    <ul>
      <li>Your changes here</li>
    </ul>
    <h3>0.1.1</h3>
    ...
    """.trimIndent()
)
```

### 2. Commit Changes

```bash
git add gradle.properties build.gradle.kts
git commit -m "chore: bump version to 0.1.2"
```

### 3. Create and Push Tag

**IMPORTANT**: The GitHub Actions workflow is triggered by tags starting with `v*`. Without a tag, the plugin will NOT be published to JetBrains Marketplace or create a GitHub release.

```bash
# Create an annotated tag
git tag -a v0.1.2 -m "Release v0.1.2

Brief description of changes"

# Push commit and tag
git push origin master --tags
```

Or push separately:

```bash
git push origin master
git push origin v0.1.2
```

### 4. GitHub Actions Workflows

This project has two workflows:

**CI Build** (`.github/workflows/build.yml`)
- Triggers on: Push to `master` branch and Pull Requests
- Actions: Builds the plugin and uploads artifacts
- Does NOT publish or create releases

**Release** (`.github/workflows/release.yml`)
- Triggers on: Push of tags matching `v*`
- Actions: Builds, publishes to JetBrains Marketplace, and creates GitHub release

Once the tag is pushed, the Release workflow will automatically:
- Build the plugin
- Publish to JetBrains Marketplace (requires `JETBRAINS_TOKEN` secret)
- Create a GitHub release with the plugin ZIP and auto-generated release notes

## Development

### Building Locally

```bash
./gradlew buildPlugin
```

The plugin ZIP will be at `build/distributions/paused-changelists-*.zip`

### Running in IDE

```bash
./gradlew runIde
```

### Testing Changes

Install the locally built plugin:
1. Build the plugin with `./gradlew buildPlugin`
2. In IntelliJ IDEA: **Settings → Plugins → ⚙️ → Install Plugin from Disk**
3. Select `build/distributions/paused-changelists-*.zip`
4. Restart IntelliJ IDEA

## Pull Requests

1. Fork the repository
2. Create a feature branch: `git checkout -b feat/your-feature`
3. Make your changes following the commit guidelines
4. Test your changes
5. Push to your fork: `git push origin feat/your-feature`
6. Open a Pull Request

## Questions?

Feel free to open an issue for any questions or concerns!
