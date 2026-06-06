# Git Commit File Changes Append Tool for IntelliJ IDEs

![Build](https://github.com/antheng/git-message-gen-intellij-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/32030-git-commit-file-changes-append-tool.svg)](https://plugins.jetbrains.com/plugin/32030-git-commit-file-changes-append-tool)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/32030-git-commit-file-changes-append-tool.svg)](https://plugins.jetbrains.com/plugin/32030-git-commit-file-changes-append-tool)

<!-- Plugin description -->
This is a helper plugin for appending the current branch and the list of imminent changes to your commit, similar to the output of `git status` command or the default commit message template when using `git commit` in CLI. 

This was created to assist workflows where branch and file info are needed in the commit messages (e.g. Jira integration). This is simple using
git in CLI, as it was simple to uncomment that branch info and file list while writing the commit message. In Intellij IDEs
however this is not possible. Hence this simple plugin helps you put that info in with a simple button click in the commit menu (above the commit message block).

For v2025.3 and higher.
<!-- Plugin description end -->

## Installation

### Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "git-message-gen-intellij-plugin"</kbd> >
  <kbd>Install</kbd>

### Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/32030-git-commit-file-changes-append-tool) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/32030-git-commit-file-changes-append-tool/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

### Manually:

  Download the [latest release](https://github.com/antheng/git-message-gen-intellij-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](./LICENSE) file for details.

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
