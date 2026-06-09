package com.minipai.article.core.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * B 站粉主题搜索框。
 *
 * 用法：
 * ```kotlin
 * BiliSearchBar(
 *     query = state.query,
 *     onQueryChange = vm::onQueryChange,
 *     onSubmit = { vm.onSubmit(state.query) },
 *     onClear = { vm.onQueryChange("") },
 *     expanded = state.isResultMode,  // 是否有结果，决定搜索框在顶部还是居中
 *     modifier = Modifier.fillMaxWidth()
 * )
 * ```
 *
 * 关键动画：
 * - `expanded=false`（landing）：搜索框圆角 28dp，padding 16dp
 * - `expanded=true`（结果页）：搜索框圆角 24dp，padding 8dp
 * - 用 `animateDpAsState` 让尺寸平滑过渡
 */
@Composable
fun BiliSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索 B 站专栏",
    compact: Boolean = false
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (expanded) 24.dp else 28.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "searchBarCorner"
    )
    val horizontalPadding by animateDpAsState(
        targetValue = if (expanded) 8.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "searchBarPadding"
    )
    val keyboard = LocalSoftwareKeyboardController.current

    Surface(
        modifier = modifier
            .padding(horizontal = horizontalPadding)
            .fillMaxWidth(),
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = if (expanded) 2.dp else 0.dp,
        shadowElevation = if (expanded) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = if (compact) 12.dp else 12.dp, vertical = if (compact) 2.dp else 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(if (compact) 19.dp else 22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { newValue ->
                        runCatching { onQueryChange(newValue) }
                            .onFailure { Log.e(TAG, "onQueryChange('$newValue') crashed", it) }
                    },
                    textStyle = (if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge).copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboard?.hide()
                        runCatching { onSubmit() }
                            .onFailure { Log.e(TAG, "onSubmit crashed", it) }
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(if (compact) 28.dp else 32.dp)
                        .clickable {
                            runCatching { onClear() }
                                .onFailure { Log.e(TAG, "onClear crashed", it) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "清空",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (compact) 18.dp else 20.dp)
                    )
                }
            }
        }
    }
}

private const val TAG = "BiliSearchBar"
