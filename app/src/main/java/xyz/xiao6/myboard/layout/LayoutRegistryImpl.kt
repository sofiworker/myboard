package xyz.xiao6.myboard.layout

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
        
        validateContainer(doc.root, issues)
        
        return issues
    }
    
    private fun validateContainer(container: LayoutContainer, issues: MutableList<LayoutIssue>) {
        if (container.id.isBlank()) {
            issues.add(LayoutIssue(IssueSeverity.WARNING, "Container id is blank"))
        }
        
        when (container) {
            is RowLayout -> {
                for (key in container.keys) {
                    validateKey(key, issues)
                }
            }
            is GridLayout -> {
                if (container.columns <= 0) {
                    issues.add(LayoutIssue(IssueSeverity.ERROR, "Grid columns must be > 0"))
                }
                for (cell in container.cells) {
                    validateKey(cell.key, issues)
                }
            }
            is LinearLayout -> {
                for (child in container.children) {
                    if (child is LayoutNode.KeyNode) {
                        validateKey(child.key, issues)
                    }
                }
            }
            is AbsoluteLayout -> {
                for (item in container.items) {
                    validateKey(item.key, issues)
                }
            }
            is CompositeLayout -> {
                for (region in container.regions) {
                    validateContainer(region.container, issues)
                }
            }
        }
    }
    
    private fun validateKey(key: KeyDef, issues: MutableList<LayoutIssue>) {
        if (key.id.isBlank()) {
            issues.add(LayoutIssue(IssueSeverity.WARNING, "Key id is blank"))
        }
    }
    
    override fun findBuiltIn(id: String): LayoutDoc? {
        return layouts[id]
    }
}
