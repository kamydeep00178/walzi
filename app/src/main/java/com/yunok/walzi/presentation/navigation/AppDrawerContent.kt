package com.yunok.walzi.presentation.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.yunok.walzi.R
import com.yunok.walzi.presentation.theme.Accent1
import com.yunok.walzi.presentation.theme.Accent2
import com.yunok.walzi.presentation.theme.Accent3
import com.yunok.walzi.presentation.theme.BorderColor
import com.yunok.walzi.presentation.theme.Elevated
import com.yunok.walzi.presentation.theme.TextPrimary
import com.yunok.walzi.presentation.theme.TextSecondary
import com.yunok.walzi.presentation.theme.TextTertiary

/** Each item carries its own icon color (not just an active-state tint) - gives the plain
 *  list some visual life, matching the reference's colorful-icon look, without needing a
 *  profile/login header or any "upgrade" styling above it. */
private data class DrawerItem(val route: String, val label: String, val icon: ImageVector, val iconColor: Color)

private val drawerItems = listOf(
    DrawerItem(Screen.Home.route, "Home", Icons.Filled.Home, Accent3),
    DrawerItem(Screen.Favorites.route, "Favorites", Icons.Filled.Favorite, Accent2),
    DrawerItem(Screen.Lists.route, "My Lists", Icons.Filled.PlaylistPlay, Accent1),
    DrawerItem(Screen.Settings.route, "Settings", Icons.Filled.Settings, TextSecondary)
)

@Composable
fun AppDrawerContent(
    navController: NavController,
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    ModalDrawerSheet(drawerContainerColor = Elevated) {
        Column(modifier = Modifier.fillMaxHeight().padding(vertical = 28.dp)) {

            Box(Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) {
                Image(
                    painter = painterResource(R.drawable.ic_wordmark),
                    contentDescription = "Walzi",
                    modifier = Modifier.height(34.dp)
                )
            }

           /* Text(
                text = "walzi",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
            )*/

            Divider(color = BorderColor, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))

            drawerItems.forEach { item ->
                val active = currentRoute == item.route
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(item.route) }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(width = 13.dp, height = 20.dp), contentAlignment = Alignment.CenterStart) {
                        if (active) {
                            Box(
                                modifier = Modifier
                                    .size(width = 3.dp, height = 20.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(item.iconColor)
                            )
                        }
                    }
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = item.iconColor,
                        modifier = Modifier.size(23.dp)
                    )
                    Text(
                        item.label,
                        color = if (active) TextPrimary else TextSecondary,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 15.5.sp,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}