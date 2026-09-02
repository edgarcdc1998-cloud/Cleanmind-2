package com.aistudio.cleanmind.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.cleanmind.app.domain.model.StorageCategory
import com.aistudio.cleanmind.app.presentation.home.CategorySummaryUi
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkSurfaceCard
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextMuted
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTextPrimary
import com.aistudio.cleanmind.app.presentation.theme.ElegantDarkTrack

@Composable
fun CategorySummaryCard(
    summary: CategorySummaryUi,
    modifier: Modifier = Modifier,
    percentageOfTotal: Float = 0f
) {
    val categoryIcon: ImageVector = when (summary.category) {
        StorageCategory.IMAGES -> Icons.Default.Image
        StorageCategory.VIDEOS -> Icons.Default.Movie
        StorageCategory.AUDIOS -> Icons.Default.Audiotrack
        StorageCategory.DOCUMENTS -> Icons.AutoMirrored.Filled.InsertDriveFile
        StorageCategory.LARGE_FILES -> Icons.Default.FolderZip
        StorageCategory.OTHERS -> Icons.Default.Folder
    }

    val categoryColor: Color = when (summary.category) {
        StorageCategory.IMAGES -> Color(0xFF64B5F6)
        StorageCategory.VIDEOS -> Color(0xFFFF8A65)
        StorageCategory.AUDIOS -> Color(0xFFA5D6A7)
        StorageCategory.DOCUMENTS -> Color(0xFFFFD54F)
        StorageCategory.LARGE_FILES -> Color(0xFFBA68C8)
        StorageCategory.OTHERS -> Color(0xFF90A4AE)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_card_${summary.category.name}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = ElegantDarkSurfaceCard
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(categoryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = stringResource(id = summary.titleResId),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElegantDarkTextPrimary
                        )
                        Text(
                            text = "${summary.fileCount} arquivos",
                            fontSize = 12.sp,
                            color = ElegantDarkTextMuted
                        )
                    }
                }

                Text(
                    text = summary.formattedSize,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = categoryColor
                )
            }

            if (percentageOfTotal > 0f) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { percentageOfTotal.coerceIn(0.01f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = categoryColor,
                        trackColor = ElegantDarkTrack.copy(alpha = 0.5f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "${(percentageOfTotal * 100).toInt()}% do analisado",
                            fontSize = 11.sp,
                            color = ElegantDarkTextMuted
                        )
                    }
                }
            }
        }
    }
}
