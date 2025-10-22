package dev.togglepaused

import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.intellij.openapi.vcs.changes.shelf.ShelveChangesManager
import com.intellij.openapi.vcs.changes.shelf.ShelvedChangeList
import java.util.Date

class PauseChangelistService {

    companion object {
        const val PAUSE_PREFIX = "[PAUSED] "
    }

    fun pause(
        changelist: LocalChangeList,
        clm: ChangeListManager,
        shelveManager: ShelveChangesManager
    ): PauseResult {
        val changes = changelist.changes
        val changelistName = changelist.name

        if (changes.isEmpty()) {
            clm.editName(changelistName, PAUSE_PREFIX + changelistName)
            return PauseResult(shelfName = null, newName = PAUSE_PREFIX + changelistName)
        }

        val shelfName = "paused_${changelistName}_${Date().time}"
        shelveManager.shelveChanges(changes.toList(), shelfName, true, true)
        clm.editName(changelistName, PAUSE_PREFIX + changelistName)

        return PauseResult(shelfName = shelfName, newName = PAUSE_PREFIX + changelistName)
    }

    fun findShelfForChangelist(
        changelistName: String,
        shelveManager: ShelveChangesManager
    ): ShelvedChangeList? {
        val unpausedName = changelistName.removePrefix(PAUSE_PREFIX)
        val shelfPrefix = "paused_${unpausedName}_"

        return shelveManager.allLists.firstOrNull { it.description.startsWith(shelfPrefix) }
    }

    fun unpause(
        changelist: LocalChangeList,
        clm: ChangeListManager,
        shelveManager: ShelveChangesManager
    ): UnpauseResult {
        val changelistName = changelist.name
        val unpausedName = changelistName.removePrefix(PAUSE_PREFIX)
        val shelf = findShelfForChangelist(changelistName, shelveManager)

        if (shelf == null) {
            clm.editName(changelistName, unpausedName)
            return UnpauseResult.Success(unpausedName, restoredFromShelf = false)
        }

        val targetList = clm.findChangeList(changelistName)
        if (targetList == null) {
            return UnpauseResult.ChangelistNotFound(changelistName)
        }

        shelveManager.unshelveChangeList(shelf, null, null, targetList, false)
        clm.editName(changelistName, unpausedName)

        return UnpauseResult.Success(unpausedName, restoredFromShelf = true)
    }
}

data class PauseResult(
    val shelfName: String?,
    val newName: String
)

sealed class UnpauseResult {
    data class Success(val newName: String, val restoredFromShelf: Boolean) : UnpauseResult()
    data class ChangelistNotFound(val changelistName: String) : UnpauseResult()
}
