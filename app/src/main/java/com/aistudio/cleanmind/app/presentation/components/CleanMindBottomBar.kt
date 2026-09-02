package com.aistudio.cleanmind.app.presentation.components

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aistudio.cleanmind.app.presentation.navigation.CleanMindDestination
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkBackground
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkSurfaceCard
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextMuted
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextPrimary

@Composable
fun CleanMindBottomBar(
    currentRoute: String,
    onNavigateToDestination: (CleanMindDestination) -> Unit,
    recommendationsBadgeCount: Int = 0,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("cleanmind_bottom_navigation_bar"),
        containerColor = ElegantDarkSurfaceCard,
        contentColor = ElegantDarkTextPrimary
    ) {
        CleanMindDestination.bottomNavDestinations.forEach { destination ->
            val isSelected = currentRoute == destination.route

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    if (destination == CleanMindDestination.Recommendations && recommendationsBadgeCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = ElegantDarkPrimary,
                                    contentColor = Color.Black
                                ) {
                                    Text(
                                        text = if (recommendationsBadgeCount > 99) "99+" else recommendationsBadgeCount.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = stringResource(id = destination.titleRes)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = stringResource(id = destination.titleRes)
                        )
                    }
                },
                label = {
                    Text(
                        text = stringResource(id = destination.titleRes),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    selectedTextColor = ElegantDarkPrimary,
                    indicatorColor = ElegantDarkPrimary,
                    unselectedIconColor = ElegantDarkTextMuted,
                    unselectedTextColor = ElegantDarkTextMuted
                ),
                modifier = Modifier.testTag("nav_item_${destination.route}")
            )
        }
    }
}
