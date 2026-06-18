package com.minipai.article.feature.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.minipai.article.core.network.model.Stats
import com.minipai.article.data.ArticleDetail
import com.minipai.article.feature.reader.model.ArticleBlock
import com.minipai.article.feature.reader.model.TextSpan
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 文章正文 LazyColumn。
 * - 头部：标题 / 作者 / 时间 / 浏览-评论-点赞
 * - body：blocks 顺序渲染
 * - 滚动停止 500ms 后通过 `onScrollChanged` 上抛位置
 */
@Composable
fun ArticleNativeView(
    detail: ArticleDetail,
    fontSize: Int,
    initialIndex: Int,
    initialOffset: Int,
    onScrollChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex,
        initialFirstVisibleItemScrollOffset = initialOffset
    )

    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    // 滚动停止 (debounce 500ms) 后保存位置
    val currentIndex = listState.firstVisibleItemIndex
    val currentOffset = listState.firstVisibleItemScrollOffset
    LaunchedEffect(currentIndex, currentOffset) {
        delay(500)
        onScrollChanged(currentIndex, currentOffset)
    }

    // 顶栏间距由外层 Box 的 statusBarsPadding() + padding(top=48.dp) 提供，
    // 此处仅保留 12dp 的视觉呼吸空间
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            ArticleHeader(detail)
        }
        itemsIndexed(items = detail.blocks, key = { index, block -> blockKey(index, block) }) { _, block ->
            BlockRender(
                block = block,
                fontSize = fontSize,
                onImageClick = { previewImageUrl = it }
            )
        }
        item(key = "bottom_spacer") {
            Spacer(Modifier.height(32.dp))
        }
    }

    previewImageUrl?.let { url ->
        ImagePreviewDialog(
            url = url,
            onDismiss = { previewImageUrl = null }
        )
    }
}

private fun blockKey(index: Int, block: ArticleBlock): String = when (block) {
    is ArticleBlock.Paragraph -> "${index}_p_${block.spans.hashCode()}"
    is ArticleBlock.Heading -> "${index}_h${block.level}_${block.text.hashCode()}"
    is ArticleBlock.Image -> "${index}_img_${block.url.hashCode()}"
    is ArticleBlock.Quote -> "${index}_q_${block.spans.hashCode()}"
    is ArticleBlock.Code -> "${index}_code_${block.code.hashCode()}"
    is ArticleBlock.ListBlock -> "${index}_list_${block.ordered}_${block.items.hashCode()}"
    ArticleBlock.Divider -> "${index}_divider"
}

@Composable
private fun ArticleHeader(detail: ArticleDetail) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = detail.title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        if (detail.authorName.isNotBlank()) {
            Text(
                text = detail.authorName + formatPublishTime(detail.publishTime),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            formatPublishTime(detail.publishTime).takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        StatsRow(detail.stats)
        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
    }
}

private fun formatPublishTime(epochSec: Long): String {
    if (epochSec <= 0L) return ""
    val sdf = SimpleDateFormat("yyyy-MM-dd · ", Locale.getDefault())
    return sdf.format(Date(epochSec * 1000L))
}

@Composable
private fun StatsRow(stats: Stats) {
    val parts = buildList {
        if (stats.view > 0) add("${stats.view} 浏览")
        if (stats.reply > 0) add("${stats.reply} 评论")
        if (stats.like > 0) add("${stats.like} 点赞")
    }
    if (parts.isEmpty()) return
    Text(
        text = parts.joinToString(" · "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun BlockRender(
    block: ArticleBlock,
    fontSize: Int,
    onImageClick: (String) -> Unit
) {
    when (block) {
        is ArticleBlock.Paragraph -> ParagraphBlock(block.spans, fontSize)
        is ArticleBlock.Heading -> HeadingBlock(block)
        is ArticleBlock.Image -> ImageBlock(block, onClick = { onImageClick(block.url) })
        is ArticleBlock.Quote -> QuoteBlock(block.spans, fontSize)
        is ArticleBlock.Code -> CodeBlock(block.code)
        is ArticleBlock.ListBlock -> ListBlockView(block, fontSize)
        ArticleBlock.Divider -> HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun ParagraphBlock(spans: List<TextSpan>, fontSize: Int) {
    Text(
        text = spans.toAnnotated(fontSize),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun HeadingBlock(block: ArticleBlock.Heading) {
    val style = when (block.level) {
        1 -> MaterialTheme.typography.headlineLarge
        2 -> MaterialTheme.typography.headlineMedium
        3 -> MaterialTheme.typography.headlineSmall
        else -> MaterialTheme.typography.titleLarge
    }
    Text(
        text = block.text,
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ImageBlock(
    block: ArticleBlock.Image,
    onClick: () -> Unit
) {
    val ctx = LocalContext.current
    val req = remember(block.url) {
        ImageRequest.Builder(ctx)
            .data(block.url)
            .size(540, 960)
            .precision(Precision.INEXACT)
            .allowHardware(false)
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = req,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    )
}

@Composable
private fun ImagePreviewDialog(
    url: String,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (scale == 1f) Offset.Zero else offset + panChange
    }
    val req = remember(url) {
        ImageRequest.Builder(ctx)
            .data(url)
            .size(1440, 2560)
            .precision(Precision.INEXACT)
            .allowHardware(false)
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = req,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(transformState)
            )
        }
    }
}

@Composable
private fun QuoteBlock(spans: List<TextSpan>, fontSize: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = spans.toAnnotated(fontSize),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ListBlockView(block: ArticleBlock.ListBlock, fontSize: Int) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        block.items.forEachIndexed { i, spans ->
            val prefix = if (block.ordered) "${i + 1}. " else "•  "
            Text(
                text = buildAnnotatedString {
                    append(prefix)
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                        append(spans.toAnnotated(fontSize))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun List<TextSpan>.toAnnotated(fontSize: Int): AnnotatedString = buildAnnotatedString {
    forEach { span ->
        when (span) {
            is TextSpan.Plain -> withStyle(
                SpanStyle(fontSize = fontSize.sp, color = MaterialTheme.colorScheme.onSurface)
            ) { append(span.text) }
            is TextSpan.Bold -> withStyle(
                SpanStyle(fontSize = fontSize.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            ) { append(span.text) }
            is TextSpan.Italic -> withStyle(
                SpanStyle(fontSize = fontSize.sp, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurface)
            ) { append(span.text) }
            is TextSpan.Link -> withStyle(
                SpanStyle(
                    fontSize = fontSize.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    textDecoration = TextDecoration.Underline
                )
            ) { append(span.text) }
        }
    }
}
