package com.droidbubble.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.droidbubble.app.icons.AppIcons

/** Which pending items the setup section shows, and how (C1.2). Pure, so it's unit-testable. */
data class SetupSectionModel(
    /** Rows drawn in full, in catalogue order. */
    val visible: List<PermissionItem>,
    /** Skipped optional items still pending, folded into one "Review" row. */
    val folded: List<PermissionItem>,
) {
    val requiredPending: Int get() = visible.count { it.level == PermissionLevel.REQUIRED }
    val isEmpty: Boolean get() = visible.isEmpty() && folded.isEmpty()

    companion object {
        fun of(pending: List<PermissionItem>, skipped: Set<String>, showSkipped: Boolean): SetupSectionModel {
            // Required items can never be skipped, whatever the stored set says
            val isSkipped = { item: PermissionItem -> item.level != PermissionLevel.REQUIRED && item.id in skipped }
            return if (showSkipped) SetupSectionModel(pending, emptyList())
            else SetupSectionModel(pending.filterNot(isSkipped), pending.filter(isSkipped))
        }
    }
}

/**
 * C1.6: the overline on the main screen. Required rows read "Step 1 of 2" / "Step 2 of 2" (their
 * place among the required items, so it stays put as items are granted); others their level.
 */
fun setupStepLabel(item: PermissionItem): String {
    if (item.level != PermissionLevel.REQUIRED) return item.level.label()
    val required = PermissionHelper.allPermissions(android.os.Build.VERSION.SDK_INT).filter { it.level == PermissionLevel.REQUIRED }
    val step = required.indexOfFirst { it.id == item.id }
    return if (step < 0) item.level.label() else "Step ${step + 1} of ${required.size}"
}

/** The level label over a row's title. */
internal fun PermissionLevel.label(): String = when (this) {
    PermissionLevel.REQUIRED -> "Required"
    PermissionLevel.RECOMMENDED -> "Recommended"
    PermissionLevel.OPTIONAL -> "Optional"
}

/**
 * C1.2: the main screen's setup section. Each row explains itself; only Required rows are red;
 * optional rows can be skipped with "Not now", and skipped rows fold into one "Review" row.
 * Draws nothing when nothing is pending.
 */
@Composable
fun PermissionSection(
    items: List<PermissionItem>,
    skipped: Set<String>,
    showSkipped: Boolean,
    onAllow: (PermissionItem) -> Unit,
    onSkip: (String) -> Unit,
    onReviewSkipped: () -> Unit,
    modifier: Modifier = Modifier,
    overline: (PermissionItem) -> String = { it.level.label() },
    restrictedHelp: Set<String> = emptySet(),
    onOpenAppInfo: () -> Unit = {},
) {
    val model = SetupSectionModel.of(items, skipped, showSkipped)
    if (model.isEmpty) return
    Column(modifier.fillMaxWidth()) {
        if (model.visible.isNotEmpty()) {
            if (model.requiredPending > 0) {
                SectionHeader(
                    title = "Required setup",
                    icon = AppIcons.Security,
                    badge = "${model.requiredPending} required",
                    badgeColor = MaterialTheme.colorScheme.error
                )
            } else {
                SectionHeader(
                    title = "Finish setup",
                    icon = AppIcons.Security,
                    badge = "${model.visible.size} optional",
                    badgeColor = MaterialTheme.colorScheme.secondary
                )
            }
            // The LazyColumn's item spacing, which used to sit between the header and the rows
            Spacer(Modifier.height(16.dp))
        }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val rows = model.visible.size + if (model.folded.isNotEmpty()) 1 else 0
            fun position(index: Int) = when {
                rows == 1 -> GroupPosition.SINGLE
                index == 0 -> GroupPosition.TOP
                index == rows - 1 -> GroupPosition.BOTTOM
                else -> GroupPosition.MIDDLE
            }
            model.visible.forEachIndexed { index, item ->
                PermissionRow(item, overline(item), position(index), onAllow, onSkip, item.id in restrictedHelp, onOpenAppInfo)
            }
            if (model.folded.isNotEmpty()) {
                SkippedRow(model.folded.size, position(rows - 1), onReviewSkipped)
            }
        }
    }
}

@Composable
private fun PermissionRow(
    item: PermissionItem,
    overline: String,
    position: GroupPosition,
    onAllow: (PermissionItem) -> Unit,
    onSkip: (String) -> Unit,
    showRestrictedHelp: Boolean = false,
    onOpenAppInfo: () -> Unit = {},
) {
    val required = item.level == PermissionLevel.REQUIRED
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = getGroupedShape(position),
        color = if (required) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = if (required) 0.dp else 1.dp
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            overlineContent = { Text(overline, fontWeight = FontWeight.SemiBold) },
            headlineContent = {
                Text(item.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            },
            supportingContent = {
                Column {
                    Text("${item.whatItDoes} ${item.whyNeeded}")
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val allowSemantics = Modifier.semantics { contentDescription = "Allow ${item.title}" }
                        if (required) {
                            Button(onClick = { onAllow(item) }, modifier = allowSemantics, shape = RoundedCornerShape(12.dp)) {
                                Text("Allow", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            FilledTonalButton(onClick = { onAllow(item) }, modifier = allowSemantics, shape = RoundedCornerShape(12.dp)) {
                                Text("Allow", fontWeight = FontWeight.SemiBold)
                            }
                            TextButton(onClick = { onSkip(item.id) }) { Text("Not now") }
                        }
                    }
                    if (showRestrictedHelp) RestrictedSettingsHelp(onOpenAppInfo)
                }
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (required) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (required) AppIcons.Warning else AppIcons.Info,
                        contentDescription = null,
                        tint = if (required) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )
    }
}

@Composable
private fun SkippedRow(count: Int, position: GroupPosition, onReview: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = getGroupedShape(position),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        ListItem(
            modifier = Modifier.clickable(onClickLabel = "Review skipped items", onClick = onReview),
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    if (count == 1) "1 optional item skipped" else "$count optional items skipped",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Text("Review", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        )
    }
}

/**
 * C1.3: the Allow path for every setup item. Items that need a disclosure first (the status bar
 * layer) show [AccessibilityDisclosureDialog]; "Continue" then calls [onGrant], "Not now" does
 * nothing. Others go straight to [onGrant]. Returns the function to call on Allow.
 */
@Composable
fun rememberDisclosureGate(onGrant: (PermissionItem) -> Unit): (PermissionItem) -> Unit {
    var asking by remember { mutableStateOf<PermissionItem?>(null) }
    asking?.let { item ->
        AccessibilityDisclosureDialog(
            onContinue = { asking = null; onGrant(item) },
            onDismiss = { asking = null }
        )
    }
    return { item -> if (item.grantKind == GrantKind.DISCLOSURE_THEN_SETTINGS) asking = item else onGrant(item) }
}

/** C1.3 copy. Every claim matches `res/xml/accessibility_service_config.xml`. */
internal const val DISCLOSURE_TITLE = "Turn on Droidbubble's status bar layer?"

/**
 * C1.3: what the accessibility service does and doesn't do, before Android's Accessibility
 * settings open. Keep it true to the service config (window-state events only, no window
 * content); when a new use is added (for example opening the shade), add it here first.
 */
@Composable
fun AccessibilityDisclosureDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(DISCLOSURE_TITLE) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("What it does: ") }
                    append("lets the island sit above the status bar and respond to touches there, and tells Droidbubble the name of the app on screen so that app's own activity can be hidden while you use it.")
                })
                Text(buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("What it can't do: ") }
                    append("read what's on your screen, what you type, or passwords. Droidbubble doesn't send this anywhere.")
                })
                Text("You can turn it off at any time in Settings › Accessibility.")
            }
        },
        confirmButton = { TextButton(onClick = onContinue) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } }
    )
}

/**
 * C1.4: Android 13+ blocks notification access and accessibility for apps installed from a file
 * until "Allow restricted settings" is on. Shown under a row that is still missing after a try.
 */
@Composable
private fun RestrictedSettingsHelp(onOpenAppInfo: () -> Unit) {
    Column(
        modifier = Modifier.padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "Android blocked this setting because Droidbubble was installed from a file.",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Text(buildAnnotatedString {
            append("1. Tap ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Open App info") }
            append(". 2. Tap ⋮ (top right) › ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Allow restricted settings") }
            append(". 3. Come back and tap Allow again.")
        })
        Text("Android only shows that option after it has blocked the setting once.")
        FilledTonalButton(onClick = onOpenAppInfo, shape = RoundedCornerShape(12.dp)) {
            Text("Open App info", fontWeight = FontWeight.SemiBold)
        }
    }
}
