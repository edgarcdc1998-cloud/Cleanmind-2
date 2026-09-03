package com.aistudio.cleanmind.app.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanMindNavigationTest {

    @Test
    fun cleanMindDestinations_haveUniqueRoutes() {
        val destinations = CleanMindDestination.bottomNavDestinations
        assertEquals(6, destinations.size)

        val routes = destinations.map { it.route }
        assertEquals(routes.distinct().size, destinations.size)
    }

    @Test
    fun cleanMindDestinations_routesMatchExpectedNames() {
        assertEquals("home", CleanMindDestination.Home.route)
        assertEquals("analysis", CleanMindDestination.Analysis.route)
        assertEquals("recommendations", CleanMindDestination.Recommendations.route)
        assertEquals("storage", CleanMindDestination.Storage.route)
        assertEquals("history", CleanMindDestination.History.route)
        assertEquals("settings", CleanMindDestination.Settings.route)
    }
}
