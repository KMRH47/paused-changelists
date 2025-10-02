# Paused Changelists

An IntelliJ IDEA plugin that allows you to "pause" changelists - shelving their changes while keeping the changelist visible in the Changes panel.

## Features

- **Toggle Pause**: Right-click any changelist and select "Toggle Pause Changelist" (or use `Ctrl+Alt+P`)
- **Visual Marker**: Paused changelists are marked with `[PAUSED]` prefix
- **Shelf Integration**: When paused, changes are shelved and reverted from your working directory
- **Restore to Original**: When unpaused, changes are restored to the same changelist they came from
- **No Disruption**: The changelist remains visible in the Changes panel while paused

## Installation

### From Release

1. Download the latest `paused-changelists-*.zip` from [Releases](https://github.com/KMRH47/paused-changelists/releases)
2. In IntelliJ IDEA: **Settings → Plugins → ⚙️ (gear icon) → Install Plugin from Disk**
3. Select the downloaded ZIP file
4. Restart IntelliJ IDEA

### From Source

```bash
./gradlew buildPlugin
```

The plugin ZIP will be at `build/distributions/paused-changelists-*.zip`

## Usage

1. **Pause a changelist**: Right-click on any changelist → "Toggle Pause Changelist"
   - Changes are shelved and reverted from your working directory
   - Changelist is renamed to `[PAUSED] <name>`
   - Changelist remains visible in the Changes panel

2. **Unpause a changelist**: Right-click on the paused changelist → "Toggle Pause Changelist" again
   - Changes are unshelved back to your working directory
   - Changes are restored to the original changelist
   - `[PAUSED]` prefix is removed

## Keyboard Shortcut

Default: `Ctrl+Alt+P` (can be customized in Settings → Keymap)

## Requirements

- IntelliJ IDEA 2024.3 or later
- Java 17 or later (for building from source)

## License

MIT
