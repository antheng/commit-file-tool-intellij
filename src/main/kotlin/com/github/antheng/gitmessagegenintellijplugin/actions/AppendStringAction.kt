package com.github.antheng.gitmessagegenintellijplugin.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.FilePath
import com.intellij.vcsUtil.VcsUtil
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.ui.Refreshable
import com.intellij.openapi.ui.Messages
import git4idea.branch.GitBranchUtil
import java.io.File

class AppendStringAction : AnAction() {
    private val logger = Logger.getInstance(AppendStringAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val document = getCommitMessageDocument(e) ?: return
        val project = e.project ?: return

        val data = Refreshable.PANEL_KEY.getData(e.dataContext)
        val checkinProjectPanel : CheckinProjectPanel? = if (data is CheckinProjectPanel) data else null
        if (checkinProjectPanel != null) {
            // Will include all files selected
            for (file in checkinProjectPanel.files) {
                logger.info("file: ${file.path}")
            }

            for (change in checkinProjectPanel.selectedChanges) {
                logger.info("Change: ${change.beforeRevision?.file?.path}")
            }
        } else{
            return
        }

        /**
         * checkinProjectPanel.selectedChanges seems to only include changes that are already tracked by git.
         * checkinProjectPanel.files only include files selected for the commit
         * To get the full list of changes, filter checkinProjectPanel.files to find any files that are not in
         * checkinProjectPanel.selectedChanges, and create new Change objects for them as if they are new files.
         * Then combine these with the selectedChanges to get the full list of changes to be committed.
         */
        val selectedChangeFiles = checkinProjectPanel.selectedChanges.mapNotNull { change ->
            (change.afterRevision?.file?.ioFile ?: change.beforeRevision?.file?.ioFile)?.path
        }.toSet()

        val unversionedNewFileChanges = checkinProjectPanel.files
            .filter { file -> file.path !in selectedChangeFiles }
            .map { file ->
                Change(
                    null, // New file changes are determined by the absence of a before revision
                    CurrentContentRevision(
                        VcsUtil.getFilePath(file.path, false)
                    ),
                )
            }

        val fullChangeList = checkinProjectPanel.selectedChanges + unversionedNewFileChanges
        

        WriteCommandAction.runWriteCommandAction(project) {
            var currentText = document.text
            // If no text currently in commit, set a default message that needs to be overwritten
            if (currentText.trim().isEmpty()){
                currentText = Messages.showInputDialog(
                    project,
                    "Enter your commit message:",
                    "Commit Message",
                    null
                )?: "(WRITE YOUR COMMIT MESSAGE HERE)"
            }
            // If its still empty
            if (currentText.isEmpty()){
                currentText = "(WRITE YOUR COMMIT MESSAGE HERE)"
            }

            val toSet = listOf(
                currentText,
                " On branch ${GitBranchUtil.guessRepositoryForOperation(project, e.dataContext)?.currentBranch?.name }",
                fileListing(fullChangeList, project?.basePath)
            ).filter { it.isNotBlank() }.joinToString("\n\n").trim() // 1 paragraph gap between elements

            document.setText(toSet)
        }
    }

    private fun fileListing(changeList: Collection<Change>, projectPath: String?): String {
        val typeLabels = mapOf(
            Change.Type.MODIFICATION to "modified:",
            Change.Type.NEW to "new file:",
            Change.Type.DELETED to "deleted:",
            Change.Type.MOVED to "renamed:"
        )
        val labelWidth = typeLabels.values.maxOf { it.length } // "new file:" = 9

        return buildString {
            append(" Changes to be committed:\n")
            for (change in changeList) {

                val label = typeLabels[change.type] ?: continue
                val paddedLabel = label.padEnd(labelWidth)
                val path = when (change.type) {
                    Change.Type.MOVED -> {
                        val before = change.beforeRevision?.file?.let {
                            getRelativePath(projectPath, it)
                        } ?: ""
                        val after = change.afterRevision?.file?.let {
                            getRelativePath(projectPath, it)
                        } ?: ""
                        "$before -> $after"
                    }
                    Change.Type.DELETED -> getRelativePath(projectPath, change.beforeRevision!!.file)
                    else -> getRelativePath(projectPath, change.afterRevision!!.file)
                }
                append("       $paddedLabel   $path\n") // Prefix spacing, and at least 3 in front of the label
            }
        }.trimEnd()
    }

    private fun getRelativePath(projectPath: String?, filePath: FilePath): String {
        val file = filePath.ioFile
        val projectPath = projectPath ?: return file.name // Reassign project path to filename if its empty
        return file.relativeToOrSelf(File(projectPath)).path
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = getCommitMessageDocument(e) != null
    }

    private fun getCommitMessageDocument(e: AnActionEvent): Document? {
        val document = e.getData(VcsDataKeys.COMMIT_MESSAGE_DOCUMENT)
        if (document != null) return document

        val control = e.getData(VcsDataKeys.COMMIT_MESSAGE_CONTROL) ?: return null

        // Use reflection to get the document from CommitMessageI/CommitMessage without direct reference
        try {
            // CommitMessageI has getEditorField() which returns EditorTextField
            val getEditorField = control.javaClass.getMethod("getEditorField")
            val editorField = getEditorField.invoke(control) ?: return null
            val getDocument = editorField.javaClass.getMethod("getDocument")
            return getDocument.invoke(editorField) as? Document
        } catch (ignored: Exception) {
        }

        try {
            // Some versions might have getDocument() directly on the control
            val getDocument = control.javaClass.getMethod("getDocument")
            return getDocument.invoke(control) as? Document
        } catch (ignored: Exception) {
        }

        return null
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
