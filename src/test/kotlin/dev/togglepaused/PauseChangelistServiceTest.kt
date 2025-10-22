package dev.togglepaused

import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.intellij.openapi.vcs.changes.shelf.ShelveChangesManager
import com.intellij.openapi.vcs.changes.shelf.ShelvedChangeList
import dev.togglepaused.PauseChangelistService.Companion.PAUSE_PREFIX
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.nio.file.Paths

class PauseChangelistServiceTest {

    private lateinit var service: PauseChangelistService
    private lateinit var clm: ChangeListManager
    private lateinit var shelveManager: ShelveChangesManager

    @Before
    fun setUp() {
        service = PauseChangelistService()
        clm = mock()
        shelveManager = mock()
    }

    @Test
    fun `pause empty changelist only renames`() {
        // Arrange
        val changelist = mockChangelist("Feature", emptySet())

        // Act
        val result = service.pause(changelist, clm, shelveManager)

        // Assert
        assertNull(result.shelfName)
        assertEquals("${PAUSE_PREFIX}Feature", result.newName)
        verify(clm).editName("Feature", "${PAUSE_PREFIX}Feature")
        verify(shelveManager, never()).shelveChanges(any(), any(), any(), any())
    }

    @Test
    fun `pause changelist with changes creates shelf and renames`() {
        // Arrange
        val mockChange = mock<Change>()
        val changelist = mockChangelist("Feature", setOf(mockChange))

        // Act
        val result = service.pause(changelist, clm, shelveManager)

        // Assert
        assertNotNull(result.shelfName)
        assertTrue(result.shelfName!!.startsWith("paused_Feature_"))
        assertEquals("${PAUSE_PREFIX}Feature", result.newName)
        verify(shelveManager).shelveChanges(listOf(mockChange), result.shelfName, true, true)
        verify(clm).editName("Feature", "${PAUSE_PREFIX}Feature")
    }

    @Test
    fun `findShelfForChangelist with multiple shelves returns most recent by timestamp`() {
        // Arrange
        val oldShelf = mockShelf("paused_Feature_1000000000")
        val newShelf = mockShelf("paused_Feature_2000000000")
        val midShelf = mockShelf("paused_Feature_1500000000")
        whenever(shelveManager.allLists).thenReturn(listOf(midShelf, oldShelf, newShelf))

        // Act
        val result = service.findShelfForChangelist("${PAUSE_PREFIX}Feature", shelveManager)

        // Assert
        assertNotNull(result)
        assertEquals("paused_Feature_2000000000", result?.description)
    }

    @Test
    fun `findShelfForChangelist with no matching shelves returns null`() {
        // Arrange
        val unrelatedShelf = mockShelf("paused_OtherFeature_1234567890")
        whenever(shelveManager.allLists).thenReturn(listOf(unrelatedShelf))

        // Act
        val result = service.findShelfForChangelist("${PAUSE_PREFIX}Feature", shelveManager)

        // Assert
        assertNull(result)
    }

    @Test
    fun `unpause with no shelf found just renames`() {
        // Arrange
        val changelist = mockChangelist("${PAUSE_PREFIX}Feature", emptySet())
        whenever(shelveManager.allLists).thenReturn(emptyList())

        // Act
        val result = service.unpause(changelist, clm, shelveManager)

        // Assert
        assertTrue(result is UnpauseResult.Success)
        assertEquals("Feature", (result as UnpauseResult.Success).newName)
        assertFalse(result.restoredFromShelf)
        verify(clm).editName("${PAUSE_PREFIX}Feature", "Feature")
        verify(shelveManager, never()).unshelveChangeList(any(), any(), any(), any(), any())
    }

    @Test
    fun `unpause when target changelist not found returns error`() {
        // Arrange
        val changelist = mockChangelist("${PAUSE_PREFIX}Feature", emptySet())
        val shelf = mockShelf("paused_Feature_1234567890")
        whenever(shelveManager.allLists).thenReturn(listOf(shelf))
        whenever(clm.findChangeList("${PAUSE_PREFIX}Feature")).thenReturn(null)

        // Act
        val result = service.unpause(changelist, clm, shelveManager)

        // Assert
        assertTrue(result is UnpauseResult.ChangelistNotFound)
        assertEquals("${PAUSE_PREFIX}Feature", (result as UnpauseResult.ChangelistNotFound).changelistName)
        verify(shelveManager, never()).unshelveChangeList(any(), any(), any(), any(), any())
    }

    @Test
    fun `unpause with valid shelf unshelves and renames`() {
        // Arrange
        val changelist = mockChangelist("${PAUSE_PREFIX}Feature", emptySet())
        val shelf = mockShelf("paused_Feature_1234567890")
        whenever(shelveManager.allLists).thenReturn(listOf(shelf))
        whenever(clm.findChangeList("${PAUSE_PREFIX}Feature")).thenReturn(changelist)

        // Act
        val result = service.unpause(changelist, clm, shelveManager)

        // Assert
        assertTrue(result is UnpauseResult.Success)
        assertEquals("Feature", (result as UnpauseResult.Success).newName)
        assertTrue(result.restoredFromShelf)
        verify(shelveManager).unshelveChangeList(shelf, null, null, changelist, false)
        verify(clm).editName("${PAUSE_PREFIX}Feature", "Feature")
    }

    private fun mockChangelist(name: String, changes: Set<Change>): LocalChangeList {
        return mock {
            on { this.name } doReturn name
            on { this.changes } doReturn changes
        }
    }

    private fun mockShelf(description: String): ShelvedChangeList {
        return mock {
            on { this.description } doReturn description
            on { path } doReturn Paths.get("/tmp/shelf")
        }
    }
}
