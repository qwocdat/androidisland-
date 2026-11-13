package com.xiao.androidisland.droidbubble

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Classic DroidBubble mode of AndroidIsland — preserved intact per Section I specification
 * ("DroidBubble: Đã hoàn thiện, giữ nguyên. Không chỉnh sửa hoặc làm hỏng DroidBubble").
 */
@Immutable
data class DroidBubbleState(
    val title: String = "DroidBubble Ready",
    val subtitle: String = "Tap to expand classic bubble",
    val isExpanded: Boolean = false,
)

@Composable
fun DroidBubbleOverlayView(
    state: DroidBubbleState,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(state.isExpanded) }
    val width = if (expanded) 260.dp else 136.dp
    val height = if (expanded) 64.dp else 34.dp

    Row(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(Color(0xEE181A20))
            .clickable {
                expanded = !expanded
                onToggleExpand()
            }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(Color(0xFF3DDC84)),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = if (expanded) "${state.title} • ${state.subtitle}" else state.title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}
