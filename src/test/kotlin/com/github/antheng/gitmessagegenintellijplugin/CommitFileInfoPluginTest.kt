package com.github.antheng.gitmessagegenintellijplugin

import com.intellij.driver.sdk.ui.components.common.ideFrame
import com.intellij.driver.sdk.ui.xQuery
import com.intellij.driver.sdk.waitForIndicators
import com.intellij.ide.starter.driver.engine.runIdeWithDriver
import com.intellij.ide.starter.ide.IdeProductProvider
import com.intellij.ide.starter.models.TestCase
import com.intellij.ide.starter.plugins.PluginConfigurator
import com.intellij.ide.starter.project.GitHubProject
import com.intellij.ide.starter.runner.Starter
import org.junit.Test
import org.junit.jupiter.api.Assertions
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.event.KeyEvent
import java.lang.Thread.sleep
import java.io.File
import java.nio.file.Paths
import kotlin.time.Duration.Companion.minutes


class CommitFileInfoPluginTest () {

    fun getClipBoardContent(): String {

        // Read clipboard content
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        try {
            return clipboard.getData(DataFlavor.stringFlavor) as? String ?: ""
        } catch (e: Exception) {
            // fallback if clipboard access fails
            println(e.message)
            return ""
        }
    }

    @Test
    fun testPlugin() {
        /**
         * Basic test to verify that the plugin can be installed and loaded in the IDE without errors.
         */
        val context = Starter.newContext(
            testName = "testExample",
            TestCase(
                ideInfo = IdeProductProvider.IU,
                // Use Jetbrain's example project as a base
                projectInfo = GitHubProject.fromGithub(
                    branchName = "master",
                    repoRelativeUrl = "JetBrains/ij-perf-report-aggregator"
                )
            ).withVersion("2025.3")
        ).apply {
            PluginConfigurator(this).installPluginFromPath(
                Paths.get("build/distributions/git-message-gen-intellij-plugin-0.0.1.zip")
            )
        }

        val projectPath: String = context.resolvedProjectHome.toString()

        // Edit "README.md"
        val readmeFile = File(projectPath, "README.md")
        if (readmeFile.exists()) {
            readmeFile.appendText("\n// Edited by test\n")
        }

        // Delete "LICENSE"
        val licenseFile = File(projectPath, "LICENSE")
        if (licenseFile.exists()) {
            licenseFile.delete()
        }

        // These will appear as unversioned files. Regardless the plugin should be able to detect these.
        // Create "test.txt"
        val testFile = File(projectPath, "new_file.txt")
        testFile.writeText("This is a test file created by the plugin test.")

        // Move "Makefile" into "pkg" using git mv so the move is tracked by Git
        val gitMvProcess = ProcessBuilder("git", "mv", "Makefile", "pkg/Makefile")
            .directory(File(projectPath))
            .redirectErrorStream(true)
            .start()
        val exitCode = gitMvProcess.waitFor()
        Assertions.assertEquals(
            0,
            exitCode,
            "git mv failed: ${gitMvProcess.inputStream.bufferedReader().readText()}"
        )


        context.runIdeWithDriver().useDriverAndCloseIde {
            // Wait for the IDE to load and the plugin to initialize
            waitForIndicators(1.minutes)
            ideFrame {

                fun checkPresenceViaTooltipAndClick(tooltipText: String, assertMessage: String) {
                    Assertions.assertTrue(
                        x(xQuery {
                            byTooltip(tooltipText)
                        }).present(), //5
                        assertMessage
                    )
                    x(xQuery {
                        byTooltip(tooltipText)
                    }).click()

                }



                checkPresenceViaTooltipAndClick("Commit", "IDE missing commit tool")

                keyboard {
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A) // Select all files in the commit dialog
                    hotKey(KeyEvent.VK_SPACE) // Mark them all ready
                }
                
                
                checkPresenceViaTooltipAndClick(
                    "Append Commit File Info", "Plugin append button missing"
                )

                // Ensure a dialog window appeared after clicking the button
                Assertions.assertTrue(
                    x(xQuery {
                        byType("javax.swing.JDialog")
                    }).present(),
                    "Expected dialog window did not appear after clicking 'Append Commit File Info'"
                )
                keyboard {
                    typeText("Test commit message")
                    enter()
                }


                // Click the message editor window, select all and copy
                x(xQuery {
                    byType("com.intellij.openapi.editor.impl.EditorComponentImpl")
                }).click()
                keyboard {
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A)
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_C)
                }
                // allow clipboard to update
                sleep(250)

                // Read clipboard content from the test
                var generatedCommitMessage = getClipBoardContent()


                println("Full commit message (clipboard):\n$generatedCommitMessage")

                // Assert the commit message starts with the message we typed, branch info and Changes header
                Assertions.assertTrue(
                    generatedCommitMessage.startsWith("Test commit message\n\n On branch master\n\n Changes to be committed:\n"),
                    "Branch info and changes header was supposed to have a 1 newline gap from the existing commit message:\n$generatedCommitMessage"
                )

                // Assert the commit message contains all files we've edited
                Assertions.assertTrue(
                    generatedCommitMessage.contains("modified:   README.md"),
                    "Modified file did not appear in the commit message text:\n$generatedCommitMessage"
                )
                Assertions.assertTrue(
                    generatedCommitMessage.contains("deleted:    LICENSE"),
                    "Deleted file did not appear in the commit message text:\n$generatedCommitMessage"
                )
                Assertions.assertTrue(
                    generatedCommitMessage.contains("new file:   new_file.txt"),
                    "New file did not appear in the commit message text:\n$generatedCommitMessage"
                )
                Assertions.assertTrue(
                    generatedCommitMessage.contains("renamed:    Makefile -> pkg\\Makefile"),
                    "Renamed filed did not appear in the commit message text:\n$generatedCommitMessage"
                )

                // Wipe text, add existing text, and perform the same checks
                // Click the message editor window, select all and copy
                x(xQuery {
                    byType("com.intellij.openapi.editor.impl.EditorComponentImpl")
                }).click()
                keyboard {
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A)
                    backspace()
                    typeText("Test prexisting message")
                }

                checkPresenceViaTooltipAndClick(
                    "Append Commit File Info", "Plugin append button missing"
                )
                // Wait for message to generate
                sleep(250)

                // Select message box again and copy to clipboard
                x(xQuery {
                    byType("com.intellij.openapi.editor.impl.EditorComponentImpl")
                }).click()

                keyboard {
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_A)
                    hotKey(KeyEvent.VK_CONTROL, KeyEvent.VK_C)
                }

                generatedCommitMessage = getClipBoardContent()

                println("Full commit message (clipboard):\n$generatedCommitMessage")

                // Assert the commit message starts with the message we typed
                Assertions.assertTrue(
                    generatedCommitMessage.startsWith("Test prexisting message\n\n On branch master\n\n Changes to be committed:\n"),
                    "Branch info and changes header was supposed to have a 1 newline gap from the existing commit message:\n$generatedCommitMessage"
                )
                // Assert the commit message contains all files we've edited
                Assertions.assertTrue(
                    generatedCommitMessage.contains("modified:   README.md"),
                    "Modified file did not appear in the commit message text:\n$generatedCommitMessage"
                )
                Assertions.assertTrue(
                    generatedCommitMessage.contains("deleted:    LICENSE"),
                    "Deleted file did not appear in the commit message text:\n$generatedCommitMessage"
                )
                Assertions.assertTrue(
                    generatedCommitMessage.contains("new file:   new_file.txt"),
                    "New file did not appear in the commit message text:\n$generatedCommitMessage"
                )
                Assertions.assertTrue(
                    generatedCommitMessage.contains("renamed:    Makefile -> pkg\\Makefile"),
                    "Renamed filed did not appear in the commit message text:\n$generatedCommitMessage"
                )
            }

        }
    }
}






