package com.tasker.android.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokenStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val tokenStore = TokenStore(context)

    @Before
    @After
    fun clearSession() {
        tokenStore.clear()
    }

    @Test
    fun unauthorizedServerResponseKeepsExistingLocalSession() {
        tokenStore.saveToken("expired-server-token")
        tokenStore.saveUser("user-1", "owner")
        tokenStore.saveLocalCredential("owner", "encrypted-password")

        assertTrue(tokenStore.preserveLocalSession())

        val reopenedStore = TokenStore(context)
        assertTrue(reopenedStore.isLoggedIn())
        assertTrue(reopenedStore.isOfflineSession())
        assertEquals("user-1", reopenedStore.getUserId())
        assertEquals("owner", reopenedStore.getUsername())
        assertEquals("encrypted-password", reopenedStore.getLocalCredential("owner"))
    }

    @Test
    fun unauthorizedResponseWithoutIdentityCannotCreateSession() {
        tokenStore.saveToken("invalid-server-token")

        assertFalse(tokenStore.preserveLocalSession())
        assertFalse(tokenStore.isLoggedIn())
    }
}
