package com.tasker.android.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionDestinationTest {
    @Test
    fun mobileStartsOnTasksWithoutAnAccount() {
        assertEquals(Screen.Tasks.route, mobileStartDestination())
    }
}
