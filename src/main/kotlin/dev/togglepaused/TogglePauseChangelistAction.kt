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

    private val service = PauseChangelistService()

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val clm = ChangeListManager.getInstance(project)
        val target = selectedChangeList(e, clm) ?: return

        if (target.name.startsWith(PAUSE_PREFIX)) {
            unpauseChangelist(project, target)
            return
        }

        pauseChangelist(project, target)
    }

    private fun pauseChangelist(project: Project, changelist: LocalChangeList) {
        val clm = ChangeListManager.getInstance(project)
        val shelveManager = ShelveChangesManager.getInstance(project)

        ProgressManager.getInstance().runProcessWithProgressSynchronously({
            service.pause(changelist, clm, shelveManager)
        }, "Pausing Changelist...", false, project)
    }

    private fun unpauseChangelist(project: Project, changelist: LocalChangeList) {
        val clm = ChangeListManager.getInstance(project)
        val shelveManager = ShelveChangesManager.getInstance(project)

        ProgressManager.getInstance().runProcessWithProgressSynchronously({
            service.unpause(changelist, clm, shelveManager)
        }, "Unpausing Changelist...", false, project)
    }

    private fun selectedChangeList(e: AnActionEvent, clm: ChangeListManager): LocalChangeList? {
        val lists = e.getData(VcsDataKeys.CHANGE_LISTS)
        if (!lists.isNullOrEmpty()) return lists.firstOrNull() as? LocalChangeList

        val changes = e.getData(VcsDataKeys.CHANGES)
        val firstChange = changes?.firstOrNull() ?: return null

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
