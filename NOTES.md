# Build Issues and Research Notes

## Problem
The original Danish instructions provided a skeleton that uses `ChangeListDecorator` API, but this API may not exist or may have changed in recent IntelliJ versions.

## Research Findings
1. The `com.intellij.vcs.changeListDecorator` extension point exists
2. Classes like `ChangeListDecorator` and `ChangeListPresentation` cannot be resolved during compilation
3. The VCS API may have changed between versions

## Alternative Approaches to Consider
1. Use a simpler action-based approach (just rename, no decorator)
2. Use file colors/scopes system instead of decorators
3. Find the correct modern API for changelist decoration
4. Look at IntelliJ Community source code directly for the current API

## Next Steps
- Try building a minimal version that just renames changelists (no visual decoration)
- Test if that builds successfully
- Then investigate the correct modern API for decoration
