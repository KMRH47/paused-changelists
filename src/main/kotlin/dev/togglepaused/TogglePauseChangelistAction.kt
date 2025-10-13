package dev.togglepaused

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.intellij.openapi.vcs.changes.shelf.ShelveChangesManager
import java.util.Date

private const val PAUSE_PREFIX = "[PAUSED] "

class TogglePauseChangelistAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val clm = ChangeListManager.getInstance(project)
        val target = selectedChangeList(e, clm) ?: return

        if (target.name.startsWith(PAUSE_PREFIX)) {
            // Un-pausing: restore the changes from shelf
            unpauseChangelist(project, target)
        } else {
            // Pausing: shelf the changes and mark as paused
            pauseChangelist(project, target)
        }
    }

    private fun pauseChangelist(project: Project, changelist: LocalChangeList) {
        val clm = ChangeListManager.getInstance(project)
        val shelveManager = ShelveChangesManager.getInstance(project)

        val changes = changelist.changes
        if (changes.isEmpty()) {
            // No changes to pause, just rename
            clm.editName(changelist.name, PAUSE_PREFIX + changelist.name)
            return
        }

        val changelistName = changelist.name

        // Run shelving on background thread to avoid EDT blocking
        ProgressManager.getInstance().runProcessWithProgressSynchronously({
            // Shelf the changes with a descriptive name
            val shelfName = "paused_${changelistName}_${Date().time}"
            shelveManager.shelveChanges(changes.toList(), shelfName, true, true)

            // Rename the changelist on EDT
            ApplicationManager.getApplication().invokeLater {
                clm.editName(changelistName, PAUSE_PREFIX + changelistName)
            }
        }, "Pausing Changelist...", false, project)
    }

    private fun unpauseChangelist(project: Project, changelist: LocalChangeList) {
        val clm = ChangeListManager.getInstance(project)
        val shelveManager = ShelveChangesManager.getInstance(project)

        val unpausedName = changelist.name.removePrefix(PAUSE_PREFIX)
        val changelistName = changelist.name

        // Find the corresponding shelf before renaming
        val shelfPrefix = "paused_${unpausedName}_"
        val shelf = shelveManager.allLists.firstOrNull { it.description.startsWith(shelfPrefix) }

        if (shelf != null) {
            // Run unshelving on background thread to avoid EDT blocking
            ProgressManager.getInstance().runProcessWithProgressSynchronously({
                // Get fresh reference to the changelist (still has PAUSED prefix at this point)
                val targetList = clm.findChangeList(changelistName)
                if (targetList != null) {
                    // Unshelve the changes directly to the target changelist
                    // The unshelveChangeList API will properly handle placing changes in the target list
                    shelveManager.unshelveChangeList(shelf, null, null, targetList, false)

                    // Rename on EDT after unshelving completes
                    ApplicationManager.getApplication().invokeLater {
                        val pausedList = clm.findChangeList(changelistName)
                        if (pausedList != null) {
                            clm.editName(changelistName, unpausedName)
                        }
                    }
                }
            }, "Unpausing Changelist...", false, project)
        } else {
            // No shelf found, just rename back
            clm.editName(changelistName, unpausedName)
        }
    }

    private fun selectedChangeList(e: AnActionEvent, clm: ChangeListManager): LocalChangeList? {
        // First, try to get the selected changelist directly
        val lists = e.getData(VcsDataKeys.CHANGE_LISTS)
        if (!lists.isNullOrEmpty()) return lists.firstOrNull() as? LocalChangeList

        // If no changelist is directly selected, try to get it from selected changes
        val changes = e.getData(VcsDataKeys.CHANGES)
        val firstChange = changes?.firstOrNull() ?: return null

        // Find which changelist contains this change
        return clm.getChangeList(firstChange)
    }

    override fun update(e: AnActionEvent) {
        val project: Project? = e.project
        val clm = project?.let { ChangeListManager.getInstance(it) }
        val lists = e.getData(VcsDataKeys.CHANGE_LISTS)
        val hasTarget = lists?.isNotEmpty() == true || clm != null
        e.presentation.isEnabledAndVisible = hasTarget
    }
}
