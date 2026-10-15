package com.example.lindabinti.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LindaMigrationsTest {

    @Test
    fun migrationVersionsAreOrdered() {
        assertEquals(1, LindaMigrations.MIGRATION_1_2.startVersion)
        assertEquals(2, LindaMigrations.MIGRATION_1_2.endVersion)
        assertEquals(2, LindaMigrations.MIGRATION_2_3.startVersion)
        assertEquals(3, LindaMigrations.MIGRATION_2_3.endVersion)
        assertEquals(3, LindaMigrations.MIGRATION_3_4.startVersion)
        assertEquals(4, LindaMigrations.MIGRATION_3_4.endVersion)
    }

    @Test
    fun migrationsAreRegisteredObjects() {
        assertNotNull(LindaMigrations.MIGRATION_1_2)
        assertNotNull(LindaMigrations.MIGRATION_2_3)
        assertNotNull(LindaMigrations.MIGRATION_3_4)
    }
}