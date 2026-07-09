package xyz.xiao6.myboard.layout

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import xyz.xiao6.myboard.common.SchemaVersion
import xyz.xiao6.myboard.contract.input.*
import xyz.xiao6.myboard.contract.layout.*
import xyz.xiao6.myboard.contract.manifest.*
import xyz.xiao6.myboard.contract.theme.*
import xyz.xiao6.myboard.contract.engine.*
import xyz.xiao6.myboard.contract.bridge.*
import xyz.xiao6.myboard.contract.registry.*
import xyz.xiao6.myboard.contract.panel.*
import xyz.xiao6.myboard.contract.language.*
import xyz.xiao6.myboard.contract.state.*

/**
 * 布局注册表真实实现。
 */
class LayoutRegistryImpl : LayoutRegistry {
    
    private val layouts = mutableMapOf<String, LayoutDoc>()
    private val sources = mutableMapOf<String, LayoutSource>()
    
    override fun register(doc: LayoutDoc, source: LayoutSource): RegisterResult {
        val issues = validate(doc)
        val errors = issues.filter { it.severity == IssueSeverity.ERROR }
        
        if (errors.isNotEmpty()) {
            return RegisterResult.Failed(errors.map { it.message })
        }
        
        layouts[doc.id] = doc
        sources[doc.id] = source
        
        return RegisterResult.Success(doc.id)
    }
    
    override fun unregister(layoutId: String) {
        layouts.remove(layoutId)
        sources.remove(layoutId)
    }
    
    override fun get(layoutId: String): LayoutDoc? {
        return layouts[layoutId]
    }
    
    override fun validate(doc: LayoutDoc): List<LayoutIssue> {
        val issues = mutableListOf<LayoutIssue>()
        
        if (doc.id.isBlank()) {
            issues.add(LayoutIssue(IssueSeverity.ERROR, "Layout id is blank"))
        }
        
        if (!SchemaVersion.isCompatible(doc.schemaVersion)) {
            issues.add(
                LayoutIssue(
                    IssueSeverity.WARNING,
                    "Schema version ${doc.schemaVersion} is not compatible with required ${SchemaVersion.CURRENT_STR} (same major version required)"
                )
            )
        }
        
        validateContainer(doc.root, doc.supportedLayers.toSet(), issues)
        
        return issues
    }
    
    private fun validateContainer(
        container: LayoutContainer,
        supportedLayers: Set<LayoutLayer>,
        issues: MutableList<LayoutIssue>
    ) {
        if (container.id.isBlank()) {
            issues.add(LayoutIssue(IssueSeverity.WARNING, "Container id is blank"))
        }
        
        when (container) {
            is RowLayout -> {
                for (key in container.keys) {
                    validateKey(key, supportedLayers, issues)
                }
            }
            is GridLayout -> {
                if (container.columns <= 0) {
                    issues.add(LayoutIssue(IssueSeverity.ERROR, "Grid columns must be > 0"))
                }
                for (cell in container.cells) {
                    validateKey(cell.key, supportedLayers, issues)
                }
            }
            is LinearLayout -> {
                for (child in container.children) {
                    if (child is LayoutNode.KeyNode) {
                        validateKey(child.key, supportedLayers, issues)
                    }
                }
            }
            is AbsoluteLayout -> {
                for (item in container.items) {
                    validateKey(item.key, supportedLayers, issues)
                }
            }
            is CompositeLayout -> {
                if (container.regions.isEmpty()) {
                    issues.add(LayoutIssue(IssueSeverity.ERROR, "CompositeLayout ${container.id} must contain at least one region"))
                }
                for (region in container.regions) {
                    if (region.id.isBlank()) {
                        issues.add(LayoutIssue(IssueSeverity.WARNING, "Region id is blank"))
                    }
                    validateContainer(region.container, supportedLayers, issues)
                }
            }
        }
    }
    
    private fun validateKey(
        key: KeyDef,
        supportedLayers: Set<LayoutLayer>,
        issues: MutableList<LayoutIssue>
    ) {
        if (key.id.isBlank()) {
            issues.add(LayoutIssue(IssueSeverity.WARNING, "Key id is blank"))
        }

        for (layerName in key.variants.keys) {
            val layer = enumValueOrNull<LayoutLayer>(layerName)
            when {
                layer == null -> issues.add(
                    LayoutIssue(
                        IssueSeverity.ERROR,
                        "Key ${key.id} variant layer '$layerName' is not a standard LayoutLayer enum name"
                    )
                )
                layer !in supportedLayers -> issues.add(
                    LayoutIssue(
                        IssueSeverity.ERROR,
                        "Key ${key.id} variant layer '${layer.name}' is not listed in supportedLayers"
                    )
                )
            }
        }

        for ((gesture, action) in key.actions.gestures) {
            validateActionPayload(key.id, gesture, action, issues)
        }
    }

    private fun validateActionPayload(
        keyId: String,
        gesture: GestureType,
        action: ActionDef,
        issues: MutableList<LayoutIssue>
    ) {
        val location = "Key $keyId ${gesture.name} ${action.actionType.name}"
        when (action.actionType) {
            LayoutActionType.SWITCH_LAYER ->
                validateEnumPayload<LayoutLayer>(action, "layer", location, issues, required = true)
            LayoutActionType.CYCLE_LAYER ->
                validateEnumListPayload<LayoutLayer>(action, "layers", location, issues)
            LayoutActionType.SWITCH_SCRIPT ->
                validateEnumPayload<Script>(action, "script", location, issues, required = true)
            LayoutActionType.OPEN_PANEL ->
                validateEnumPayload<PanelType>(action, "panel", location, issues, required = true)
            else -> Unit
        }
    }

    private inline fun <reified T : Enum<T>> validateEnumPayload(
        action: ActionDef,
        field: String,
        location: String,
        issues: MutableList<LayoutIssue>,
        required: Boolean
    ) {
        val element = action.payload[field]
        if (element == null) {
            if (required) {
                issues.add(LayoutIssue(IssueSeverity.ERROR, "$location payload.$field is required"))
            }
            return
        }

        val value = (element as? JsonPrimitive)?.content
        if (value == null || enumValueOrNull<T>(value) == null) {
            issues.add(
                LayoutIssue(
                    IssueSeverity.ERROR,
                    "$location payload.$field must be a standard ${T::class.simpleName} enum name"
                )
            )
        }
    }

    private inline fun <reified T : Enum<T>> validateEnumListPayload(
        action: ActionDef,
        field: String,
        location: String,
        issues: MutableList<LayoutIssue>
    ) {
        val element = action.payload[field] ?: return
        val values = element as? JsonArray
        if (values == null) {
            issues.add(LayoutIssue(IssueSeverity.ERROR, "$location payload.$field must be an array"))
            return
        }

        values.forEachIndexed { index, item ->
            val value = (item as? JsonPrimitive)?.content
            if (value == null || enumValueOrNull<T>(value) == null) {
                issues.add(
                    LayoutIssue(
                        IssueSeverity.ERROR,
                        "$location payload.$field[$index] must be a standard ${T::class.simpleName} enum name"
                    )
                )
            }
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? {
        return runCatching { enumValueOf<T>(value) }.getOrNull()
    }
    
    override fun findBuiltIn(id: String): LayoutDoc? {
        return layouts[id]
    }
}
