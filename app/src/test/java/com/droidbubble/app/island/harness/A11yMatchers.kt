package com.droidbubble.app.island.harness

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher

/**
 * C3.1: semantics matchers for the island's TalkBack contract (phase 8, "Semantics spec for each
 * island element"). Use with `assert(...)` on a node, or `onAllNodes(...)`.
 */
object A11yMatchers {

    /** The node's tap action is labelled [label] (what TalkBack reads as "Double-tap to <label>"). */
    fun hasClickLabel(label: String) = SemanticsMatcher("OnClick label = \"$label\"") { node ->
        node.config.getOrNull(SemanticsActions.OnClick)?.label == label
    }

    /** The node's long-press action is labelled [label] ("Double-tap and hold to <label>"). */
    fun hasLongClickLabel(label: String) = SemanticsMatcher("OnLongClick label = \"$label\"") { node ->
        node.config.getOrNull(SemanticsActions.OnLongClick)?.label == label
    }

    /** The node offers a custom action (TalkBack's Actions menu) labelled [label]. */
    fun hasCustomAction(label: String) = SemanticsMatcher("custom action \"$label\"") { node ->
        node.config.getOrNull(SemanticsActions.CustomActions).orEmpty().any { it.label == label }
    }

    /** The node is a live region in [mode]; null means it must not be a live region at all. */
    fun hasLiveRegion(mode: LiveRegionMode?) = SemanticsMatcher("live region = $mode") { node ->
        node.config.getOrNull(SemanticsProperties.LiveRegion) == mode
    }

    /** The node is a pane titled [title] (announced when it appears). */
    fun hasPaneTitle(title: String) = SemanticsMatcher("pane title = \"$title\"") { node ->
        node.config.getOrNull(SemanticsProperties.PaneTitle) == title
    }

    /** The node has a tap action at all, labelled or not. */
    val hasClickAction = SemanticsMatcher("has OnClick") { node ->
        node.config.getOrNull(SemanticsActions.OnClick) != null
    }
}
