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

        // First, rename back to unpaused
        clm.editName(changelistName, unpausedName)

        // Find the corresponding shelf
        val shelfPrefix = "paused_${unpausedName}_"
        val shelf = shelveManager.allLists.firstOrNull { it.description.startsWith(shelfPrefix) }

        if (shelf != null) {
            // Run unshelving on background thread to avoid EDT blocking
            ProgressManager.getInstance().runProcessWithProgressSynchronously({
                // Get fresh reference to the renamed changelist
                val targetList = clm.findChangeList(unpausedName)
                if (targetList != null) {
                    // Unshelve the changes
                    shelveManager.unshelveChangeList(shelf, null, null, targetList, false)

                    // Move changes to the correct changelist if they didn't go there
                    ApplicationManager.getApplication().invokeLater {
                        val currentTargetList = clm.findChangeList(unpausedName)
                        if (currentTargetList != null) {
                            // Get all changes that might have gone to the wrong list
                            val defaultList = clm.defaultChangeList
                            val shelvedChanges = shelf.changes ?: emptyList()
                            val changesToMove = defaultList.changes.filter { change ->
                                // Check if this change was part of the unshelved changes
                                shelvedChanges.any { shelvedChange ->
                                    shelvedChange.afterPath == change.afterRevision?.file?.path
                                }
                            }

                            // Move them to the target changelist
                            if (changesToMove.isNotEmpty() && defaultList.id != currentTargetList.id) {
                                clm.moveChangesTo(currentTargetList, *changesToMove.toTypedArray())
                            }
                        }
                    }
                }
            }, "Unpausing Changelist...", false, project)
        }
    }

    private fun selectedChangeList(e: AnActionEvent, clm: ChangeListManager): LocalChangeList? {
        // First, try to get the selected changelist directly
        val lists = e.getData(VcsDataKeys.CHANGE_LISTS)
        if (!lists.isNullOrEmpty()) return lists.firstOrNull() as? LocalChangeList
        
        // If no changelist is directly selected, try to get it from selected changes
        val changes = e.getData(VcsDataKeys.CHANGES)
        if (!changes.isNullOrEmpty()) {
            val change = changes.firstOrNull()
            if (change != null) {
                // Find which changelist contains this change
                return clm.getChangeList(change)
            }
        }
        
        // Last resort: return null to avoid accidentally modifying the default changelist
        return null
    }

    override fun update(e: AnActionEvent) {
        val project: Project? = e.project
        val clm = project?.let { ChangeListManager.getInstance(it) }
        val lists = e.getData(VcsDataKeys.CHANGE_LISTS)
        val hasTarget = lists?.isNotEmpty() == true || clm != null
        e.presentation.isEnabledAndVisible = hasTarget
    }
}
