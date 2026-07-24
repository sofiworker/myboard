package xyz.xiao6.myboard.layout

import xyz.xiao6.myboard.contract.manifest.LayoutMirrorPolicy
import xyz.xiao6.myboard.contract.manifest.ScriptDescriptor
import xyz.xiao6.myboard.contract.manifest.TextDirection

data class LayoutPresentation(
    val isRtl: Boolean,
    val mirrorHorizontal: Boolean
)

fun ScriptDescriptor.toLayoutPresentation(): LayoutPresentation = LayoutPresentation(
    isRtl = direction == TextDirection.RTL,
    mirrorHorizontal = layoutMirror == LayoutMirrorPolicy.MIRROR_HORIZONTAL
)
