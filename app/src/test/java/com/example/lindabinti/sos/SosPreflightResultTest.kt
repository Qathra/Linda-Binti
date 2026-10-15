package com.example.lindabinti.sos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SosPreflightResultTest {

    @Test
    fun canSendRequiresSmsAndContacts() {
        val ok = SosPreflightResult(
            canSendSms = true,
            smsGranted = true,
            hasTrustedContacts = true,
            contactCount = 2,
            locationGranted = false,
            notificationsGranted = true,
            smsPermanentlyDenied = false,
            locationPermanentlyDenied = false
        )
        assertTrue(ok.canSendSms)
        assertTrue(ok.blockers.isEmpty())
        assertFalse(ok.locationGranted) // location never blocks
    }

    @Test
    fun blockersListSmsAndContacts() {
        val blocked = SosPreflightResult(
            canSendSms = false,
            smsGranted = false,
            hasTrustedContacts = false,
            contactCount = 0,
            locationGranted = true,
            notificationsGranted = true,
            smsPermanentlyDenied = true,
            locationPermanentlyDenied = false
        )
        assertEquals(
            listOf(
                SosPreflightResult.Blocker.SmsPermission,
                SosPreflightResult.Blocker.NoContacts
            ),
            blocked.blockers
        )
    }
}
