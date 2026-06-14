package xyz.xiao6.myboard.core.layout

import kotlinx.serialization.Serializable

/**
 * 布局 Patch 应用器。
 */
object PatchApplier {
    fun apply(base: KeyboardLayout, patch: LayoutPatch): KeyboardLayout {
        var result = base.copy(
            keys = base.keys.toMutableMap(),
            rows = base.rows.toMutableList()
        )

        for (op in patch.ops) {
            result = when (op) {
                is PatchOp.ReplaceKey -> result.copy(
                    keys = result.keys.toMutableMap().apply { put(op.key, op.data) }
                )
                is PatchOp.InsertKeyAfter -> {
                    val newKeys = result.keys.toMutableMap().apply { put(op.data.label ?: "", op.data) }
                    val newRows = result.rows.map { row ->
                        val idx = row.keys.indexOf(op.after)
                        if (idx >= 0) {
                            val keyId = op.data.label ?: "key_${System.currentTimeMillis()}"
                            val newKeysList = row.keys.toMutableList().apply { add(idx + 1, keyId) }
                            row.copy(keys = newKeysList)
                        } else row
                    }
                    result.copy(keys = newKeys, rows = newRows)
                }
                is PatchOp.InsertKeyBefore -> {
                    val newKeys = result.keys.toMutableMap().apply { put(op.data.label ?: "", op.data) }
                    val newRows = result.rows.map { row ->
                        val idx = row.keys.indexOf(op.before)
                        if (idx >= 0) {
                            val keyId = op.data.label ?: "key_${System.currentTimeMillis()}"
                            val newKeysList = row.keys.toMutableList().apply { add(idx, keyId) }
                            row.copy(keys = newKeysList)
                        } else row
                    }
                    result.copy(keys = newKeys, rows = newRows)
                }
                is PatchOp.RemoveKey -> {
                    val newRows = result.rows.map { row ->
                        row.copy(keys = row.keys.filter { it != op.key })
                    }
                    result.copy(rows = newRows)
                }
                is PatchOp.ReplaceRow -> {
                    val newRows = result.rows.map { row ->
                        if (row.id == op.row) row.copy(keys = op.keys) else row
                    }
                    result.copy(rows = newRows)
                }
            }
        }

        return result
    }
}

@Serializable
data class LayoutPatch(
    val target: String,
    val ops: List<PatchOp>
)

sealed interface PatchOp {
    @Serializable data class ReplaceKey(val key: String, val data: KeyData) : PatchOp
    @Serializable data class InsertKeyAfter(val after: String, val data: KeyData) : PatchOp
    @Serializable data class InsertKeyBefore(val before: String, val data: KeyData) : PatchOp
    @Serializable data class RemoveKey(val key: String) : PatchOp
    @Serializable data class ReplaceRow(val row: String, val keys: List<String>) : PatchOp
}
