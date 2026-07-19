package xyz.xiao6.myboard.layout

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.xiao6.myboard.contract.input.InputAction
import xyz.xiao6.myboard.contract.layout.ActionDef
import xyz.xiao6.myboard.contract.layout.ActionMap
import xyz.xiao6.myboard.contract.layout.CompositeLayout
import xyz.xiao6.myboard.contract.layout.ContentSpec
import xyz.xiao6.myboard.contract.layout.GestureType
import xyz.xiao6.myboard.contract.layout.GridLayout
import xyz.xiao6.myboard.contract.layout.KeyDef
import xyz.xiao6.myboard.contract.layout.LayoutActionType
import xyz.xiao6.myboard.contract.layout.LayoutPresentationMode
import xyz.xiao6.myboard.contract.layout.MeasuredLayout
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.fail
import xyz.xiao6.myboard.contract.bridge.LayoutHint
import xyz.xiao6.myboard.contract.registry.LayoutSource
import xyz.xiao6.myboard.contract.registry.RegisterResult
import xyz.xiao6.myboard.contract.state.KeyboardContext
import xyz.xiao6.myboard.contract.state.LayoutLayer
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.OrthogonalState
import xyz.xiao6.myboard.contract.state.PanelType
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.contract.state.Script

class LayoutDocParserTest {

    @Test
    fun `parses v2 grid input layout jsonc`() {
        val doc = LayoutDocParser.parse(
            """
            {
              // comments and trailing commas are accepted
              "schemaVersion": "1.0.0",
              "id": "test_layout",
              "root": {
                "type": "grid",
                "id": "keyboard_grid",
                "columns": 2,
                "cells": [
                  {
                    "key": {
                      "id": "q",
                      "content": { "label": "Q" },
                      "actions": {
                        "gestures": {
                          "TAP": {
                            "actionType": "PUSH_TOKEN",
                            "payload": { "token": "q" }
                          }
                        }
                      }
                    },
                    "col": 0,
                    "row": 0,
                  }
                ],
              },
              "supportedLayers": ["NORMAL"],
            }
            """.trimIndent()
        )

        val keyboard = doc.root as GridLayout

        assertEquals("test_layout", doc.id)
        assertEquals(2, keyboard.columns)
        assertEquals("q", keyboard.cells.single().key.id)
        assertEquals(LayoutActionType.PUSH_TOKEN, keyboard.cells.single().key.actions.gestures.getValue(GestureType.TAP).actionType)
    }

    @Test
    fun `rejects legacy action type aliases`() {
        try {
            LayoutDocParser.parse(
                """
                {
                  "schemaVersion": "1.0.0",
                  "id": "legacy_alias_layout",
                  "root": {
                    "type": "grid",
                    "id": "keyboard_grid",
                    "columns": 1,
                    "cells": [
                      {
                        "key": {
                          "id": "q",
                          "actions": {
                            "gestures": {
                              "TAP": {
                                "actionType": "commitToken",
                                "payload": { "token": "q" }
                              }
                            }
                          }
                        },
                        "col": 0,
                        "row": 0
                      }
                    ]
                  }
                }
                """.trimIndent()
            )
            fail("legacy actionType commitToken should be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected: JSONC must use standard enum names only.
        }
    }

    @Test
    fun `rejects non standard supported layer enum names`() {
        try {
            LayoutDocParser.parse(
                """
                {
                  "schemaVersion": "1.0.0",
                  "id": "lowercase_layer_layout",
                  "root": {
                    "type": "grid",
                    "id": "keyboard_grid",
                    "columns": 1,
                    "cells": []
                  },
                  "supportedLayers": ["normal"]
                }
                """.trimIndent()
            )
            fail("lowercase supportedLayers value should be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected: JSONC enum fields must use standard enum names only.
        }
    }

    @Test
    fun `dispatches tap action from key definition`() {
        val doc = LayoutDocParser.parse(
            """
            {
              "schemaVersion": "1.0.0",
              "id": "test_layout",
              "root": {
                "type": "grid",
                "id": "keyboard_grid",
                "columns": 1,
                "cells": [
                  {
                    "key": {
                      "id": "q",
                      "content": { "label": "Q" },
                      "actions": {
                        "gestures": {
                          "TAP": {
                            "actionType": "PUSH_TOKEN",
                            "payload": { "token": "q" }
                          }
                        }
                      }
                    },
                    "col": 0,
                    "row": 0
                  }
                ]
              }
            }
            """.trimIndent()
        )
        val key = (doc.root as GridLayout).cells.single().key

        val action = ActionDispatcher().dispatch(key, GestureType.TAP, keyboardContext())

        assertTrue(action is InputAction.PushToken)
        assertEquals("q", (action as InputAction.PushToken).token)
    }

    @Test
    fun `dispatches editor action as enter`() {
        val key = KeyDef(
            id = "enter",
            content = ContentSpec(label = "Enter"),
            actions = ActionMap(
                gestures = mapOf(
                    GestureType.TAP to ActionDef(actionType = LayoutActionType.ENTER)
                )
            )
        )

        val action = ActionDispatcher().dispatch(key, GestureType.TAP, keyboardContext())

        assertEquals(InputAction.Enter, action)
    }

    @Test
    fun `dispatches symbol panel enum payload`() {
        val key = KeyDef(
            id = "symbols",
            content = ContentSpec(label = "?123"),
            actions = ActionMap(
                gestures = mapOf(
                    GestureType.TAP to ActionDef(
                        actionType = LayoutActionType.OPEN_PANEL,
                        payload = mapOf("panel" to JsonPrimitive("SYMBOL"))
                    )
                )
            )
        )

        val action = ActionDispatcher().dispatch(key, GestureType.TAP, keyboardContext())

        assertEquals(InputAction.OpenPanel(PanelType.SYMBOL), action)
    }

    @Test
    fun `dispatches lowercase unknown script identifiers as normalized script actions`() {
        val key = KeyDef(
            id = "switch_script",
            content = ContentSpec(label = "Script"),
            actions = ActionMap(
                gestures = mapOf(
                    GestureType.TAP to ActionDef(
                        actionType = LayoutActionType.SWITCH_SCRIPT,
                        payload = mapOf("script" to JsonPrimitive("qaaa"))
                    )
                )
            )
        )

        val action = ActionDispatcher().dispatch(key, GestureType.TAP, keyboardContext())

        assertEquals(InputAction.SwitchScript(Script("QAAA")), action)
    }

    @Test
    fun `dispatches invalid script payloads as noop`() {
        listOf("ABC", "AB1D", "ÁBCD").forEach { invalidScript ->
            val key = KeyDef(
                id = "switch_script_$invalidScript",
                actions = ActionMap(
                    gestures = mapOf(
                        GestureType.TAP to ActionDef(
                            actionType = LayoutActionType.SWITCH_SCRIPT,
                            payload = mapOf("script" to JsonPrimitive(invalidScript))
                        )
                    )
                )
            )

            assertEquals(
                invalidScript,
                InputAction.Noop,
                ActionDispatcher().dispatch(key, GestureType.TAP, keyboardContext())
            )
        }

        val missingPayloadKey = KeyDef(
            id = "switch_script_missing_payload",
            actions = ActionMap(
                gestures = mapOf(
                    GestureType.TAP to ActionDef(actionType = LayoutActionType.SWITCH_SCRIPT)
                )
            )
        )

        assertEquals(
            InputAction.Noop,
            ActionDispatcher().dispatch(missingPayloadKey, GestureType.TAP, keyboardContext())
        )
    }

    @Test
    fun `all bundled layout jsonc files parse and register with standard enum names`() {
        layoutAssets().forEach { file ->
            val doc = LayoutDocParser.parse(file.readText())
            val result = LayoutRegistryImpl().register(doc, LayoutSource.BUILT_IN)

            assertTrue(file.name, doc.id.isNotBlank())
            assertTrue(file.name, result is RegisterResult.Success)
        }
    }

    @Test
    fun `bundled primary input layouts do not embed toolbar or inline candidate chrome`() {
        layoutAssets().forEach { file ->
            val text = file.readText()
            val doc = LayoutDocParser.parse(text)

            assertTrue(
                "${file.name} should not embed toolbar or inline candidate chrome",
                "candidate_region" !in text &&
                    "toolbar_region" !in text
            )
            assertTrue(file.name, doc.root.id.isNotBlank())
        }
    }

    @Test
    fun `parses full surface composite regions for complex candidate page`() {
        val doc = LayoutDocParser.parse(
            """
            {
              "schemaVersion": "1.0.0",
              "id": "candidate_words_page",
              "presentationMode": "FULL_SURFACE",
              "root": {
                "type": "composite",
                "id": "candidate_words_root",
                "orientation": "HORIZONTAL",
                "regions": [
                  {
                    "id": "pinyin_rail",
                    "role": "pinyinRail",
                    "tags": ["rail", "scrollable", "pinyin"],
                    "container": {
                      "type": "linear",
                      "id": "pinyin_rail_list",
                      "orientation": "VERTICAL",
                      "children": [],
                      "width": { "type": "dp", "value": 48 }
                    }
                  },
                  {
                    "id": "candidate_grid_region",
                    "role": "candidateGrid",
                    "tags": ["content", "candidate", "scrollable"],
                    "container": {
                      "type": "grid",
                      "id": "candidate_grid",
                      "columns": 3,
                      "cells": [],
                      "width": { "type": "weight", "value": 1 }
                    }
                  },
                  {
                    "id": "candidate_actions",
                    "role": "fixedActions",
                    "tags": ["fixed", "actions"],
                    "container": {
                      "type": "grid",
                      "id": "candidate_actions_grid",
                      "columns": 1,
                      "cells": [],
                      "width": { "type": "dp", "value": 56 }
                    }
                  }
                ]
              },
              "supportedLayers": ["NORMAL"]
            }
            """.trimIndent()
        )

        assertEquals(LayoutPresentationMode.FULL_SURFACE, doc.presentationMode)

        val root = doc.root as CompositeLayout
        assertEquals(
            listOf("pinyinRail", "candidateGrid", "fixedActions"),
            root.regions.map { it.role }
        )
        assertEquals(listOf("content", "candidate", "scrollable"), root.regions[1].tags)
    }

    @Test
    fun `bundled full surface candidate symbol and emoji layouts parse as composite pages`() {
        listOf(
            "candidate_words_page.jsonc" to "candidate_words_page",
            "symbols_full_surface.jsonc" to "symbols_full_surface",
            "emoji_full_surface.jsonc" to "emoji_full_surface"
        ).forEach { (fileName, expectedId) ->
            val text = layoutAsset(fileName).readText()
            val doc = LayoutDocParser.parse(text)
            val result = LayoutRegistryImpl().register(doc, LayoutSource.BUILT_IN)

            assertEquals(fileName, expectedId, doc.id)
            assertEquals(fileName, LayoutPresentationMode.FULL_SURFACE, doc.presentationMode)
            assertTrue(fileName, doc.root is CompositeLayout)
            assertTrue(fileName, (doc.root as CompositeLayout).regions.size >= 2)
            assertTrue(fileName, result is RegisterResult.Success)
            assertTrue("$fileName must not introduce special-key behavior", "specialKey" !in text)
        }
    }

    @Test
    fun `bundled candidate layout uses indexed candidate actions and paging controls`() {
        val root = readAssetLayout("candidate_words_page.jsonc").root as CompositeLayout
        val grid = root.regions.single { it.id == "candidate_grid_region" }.container as GridLayout
        val actions = root.regions.single { it.id == "candidate_actions" }.container as GridLayout

        val firstCandidateAction = grid.cells
            .first()
            .key
            .actions
            .gestures
            .getValue(GestureType.TAP)
        val actionTypes = actions.cells.map {
            it.key.actions.gestures.getValue(GestureType.TAP).actionType
        }.toSet()

        assertEquals(LayoutActionType.COMMIT_CANDIDATE, firstCandidateAction.actionType)
        assertEquals("0", firstCandidateAction.payload.getValue("index").toString())
        assertTrue(LayoutActionType.PAGE_PREV in actionTypes)
        assertTrue(LayoutActionType.PAGE_NEXT in actionTypes)
        assertTrue(LayoutActionType.CLOSE_PANEL in actionTypes)
    }

    @Test
    fun `number layout asset backs number layout hint`() {
        val resolvedLayoutId = LayoutHintResolverImpl().resolve(LayoutHint.NUMBER, "qwerty")
        val doc = readAssetLayout("$resolvedLayoutId.jsonc")
        val grid = doc.root as GridLayout
        val byId = grid.cells.associateBy { it.key.id }

        val result = LayoutRegistryImpl().register(doc, LayoutSource.BUILT_IN)

        assertEquals("number", resolvedLayoutId)
        assertEquals("number", doc.id)
        assertTrue(result is RegisterResult.Success)
        assertEquals(LayoutActionType.PUSH_TOKEN, byId.getValue("num_1").key.actions.gestures.getValue(GestureType.TAP).actionType)
        assertEquals(LayoutActionType.DELETE, byId.getValue("backspace").key.actions.gestures.getValue(GestureType.TAP).actionType)
        assertEquals(LayoutActionType.ENTER, byId.getValue("enter").key.actions.gestures.getValue(GestureType.TAP).actionType)
    }

    @Test
    fun `measured layout model exposes flat key area`() {
        val doc = readAssetLayout("qwerty.jsonc")

        val measured = MeasuredLayout(
            doc = doc,
            keys = emptyList(),
            viewWidth = 1000,
            viewHeight = 400,
            layer = LayoutLayer.NORMAL
        )

        assertTrue(measured.keys.isEmpty())
    }

    @Test
    fun `registry rejects non standard panel payload enum names`() {
        val doc = singleActionLayout(
            """
            {
              "actionType": "OPEN_PANEL",
              "payload": { "panel": "symbol" }
            }
            """.trimIndent()
        )

        assertFailedRegistrationContains(doc, "panel")
    }

    @Test
    fun `registry rejects non standard layer payload enum names`() {
        val doc = singleActionLayout(
            """
            {
              "actionType": "CYCLE_LAYER",
              "payload": { "layers": ["NORMAL", "shifted"] }
            }
            """.trimIndent()
        )

        assertFailedRegistrationContains(doc, "layers")
    }

    @Test
    fun `registry accepts unknown valid script identifiers`() {
        val doc = singleActionLayout(
            """
            {
              "actionType": "SWITCH_SCRIPT",
              "payload": { "script": "QAAA" }
            }
            """.trimIndent()
        )

        assertTrue(LayoutRegistryImpl().register(doc, LayoutSource.USER) is RegisterResult.Success)
    }

    @Test
    fun `registry rejects malformed or missing script payloads`() {
        listOf(
            """{ "actionType": "SWITCH_SCRIPT", "payload": { "script": "ABC" } }""",
            """{ "actionType": "SWITCH_SCRIPT", "payload": { "script": "AB1D" } }""",
            """{ "actionType": "SWITCH_SCRIPT", "payload": { "script": "ÁBCD" } }""",
            """{ "actionType": "SWITCH_SCRIPT", "payload": {} }"""
        ).forEach { actionJson ->
            assertFailedRegistrationContains(singleActionLayout(actionJson), "script")
        }
    }

    @Test
    fun `qwerty uses common mobile keyboard geometry`() {
        val keyboard = readAssetLayout("qwerty.jsonc").keyboardGrid()

        assertEquals(10, keyboard.columns)

        val byId = keyboard.cells.associateBy { it.key.id }
        assertGridCell(byId.getValue("a"), col = 0.5f, row = 1f)
        assertGridCell(byId.getValue("shift"), col = 0f, row = 2f, colSpan = 1.5f)
        assertGridCell(byId.getValue("backspace"), col = 8.5f, row = 2f, colSpan = 1.5f)
        assertGridCell(byId.getValue("space"), col = 3.5f, row = 3f, colSpan = 3.5f)
        assertGridCell(byId.getValue("enter"), col = 8f, row = 3f, colSpan = 2f)
    }

    @Test
    fun `all mobile alphabet layouts parse and share common function key geometry`() {
        listOf(
            "qwerty.jsonc",
            "shuangpin_ziran.jsonc",
            "qwerty_dvorak.jsonc",
            "qwerty_colemak.jsonc",
            "qwerty_abc.jsonc"
        ).forEach { fileName ->
            val keyboard = readAssetLayout(fileName).keyboardGrid()
            val byId = keyboard.cells.associateBy { it.key.id }

            assertEquals(fileName, 10, keyboard.columns)
            assertEquals(fileName, 9, keyboard.cells.count { it.row == 1f })
            assertGridCell(fileName, byId.getValue("shift"), col = 0f, row = 2f, colSpan = 1.5f)
            assertGridCell(fileName, byId.getValue("backspace"), col = 8.5f, row = 2f, colSpan = 1.5f)
            assertGridCell(fileName, byId.getValue("sym"), col = 0f, row = 3f, colSpan = 1.5f)
            assertGridCell(fileName, byId.getValue("space"), col = 3.5f, row = 3f, colSpan = 3.5f)
            assertGridCell(fileName, byId.getValue("enter"), col = 8f, row = 3f, colSpan = 2f)
        }
    }

    @Test
    fun `resolves string resource labels from layout data`() {
        val label = LayoutTextResolver.resolve("@string/keyboard_key_space") { key ->
            if (key == "keyboard_key_space") "Space" else null
        }

        assertEquals("Space", label)
    }

    private fun keyboardContext(): KeyboardContext =
        KeyboardContext(
            orthogonal = OrthogonalState(
                locale = LocaleTag("en-US"),
                script = Script.LATN,
                schema = Schema("latin_direct")
            ),
            layoutId = "test_layout",
            layer = LayoutLayer.NORMAL,
            activePanel = PanelType.NONE
        )

    private fun readAssetLayout(fileName: String) =
        LayoutDocParser.parse(layoutAsset(fileName).readText())

    private fun layoutAsset(fileName: String): File =
        listOf(
            File("app/src/main/assets/layouts/$fileName"),
            File("src/main/assets/layouts/$fileName")
        ).first { it.exists() }

    private fun layoutAssets(): List<File> =
        listOf(
            File("app/src/main/assets/layouts"),
            File("src/main/assets/layouts")
        ).first { it.exists() }
            .listFiles { file -> file.extension == "jsonc" }
            ?.sortedBy { it.name }
            ?: emptyList()

    private fun singleActionLayout(actionJson: String) =
        LayoutDocParser.parse(
            """
            {
              "schemaVersion": "1.0.0",
              "id": "single_action_layout",
              "root": {
                "type": "grid",
                "id": "keyboard_grid",
                "columns": 1,
                "cells": [
                  {
                    "key": {
                      "id": "action_key",
                      "actions": {
                        "gestures": {
                          "TAP": $actionJson
                        }
                      }
                    },
                    "col": 0,
                    "row": 0
                  }
                ]
              }
            }
            """.trimIndent()
        )

    private fun assertFailedRegistrationContains(
        doc: xyz.xiao6.myboard.contract.layout.LayoutDoc,
        expectedMessagePart: String
    ) {
        val result = LayoutRegistryImpl().register(doc, LayoutSource.USER)

        assertTrue(result is RegisterResult.Failed)
        val errors = (result as RegisterResult.Failed).errors
        assertTrue(errors.joinToString(), errors.any { expectedMessagePart in it })
    }

    private fun xyz.xiao6.myboard.contract.layout.LayoutDoc.keyboardGrid(): GridLayout {
        return root as GridLayout
    }

    private fun assertGridCell(
        cell: GridLayout.GridCell,
        col: Float,
        row: Float,
        colSpan: Float = 1f
    ) {
        assertEquals(col, cell.col, 0.001f)
        assertEquals(row, cell.row, 0.001f)
        assertEquals(colSpan, cell.colSpan, 0.001f)
    }

    private fun assertGridCell(
        message: String,
        cell: GridLayout.GridCell,
        col: Float,
        row: Float,
        colSpan: Float = 1f
    ) {
        assertEquals(message, col, cell.col, 0.001f)
        assertEquals(message, row, cell.row, 0.001f)
        assertEquals(message, colSpan, cell.colSpan, 0.001f)
    }
}
