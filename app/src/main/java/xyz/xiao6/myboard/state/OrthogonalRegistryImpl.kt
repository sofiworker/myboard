package xyz.xiao6.myboard.state

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
import xyz.xiao6.myboard.engine.EngineRegistry
import xyz.xiao6.myboard.layout.LayoutRegistry
import xyz.xiao6.myboard.engine.DictionaryRegistry
import xyz.xiao6.myboard.engine.EngineResourceResolver

/**
 * 正交能力注册表。
 * 负责加载、校验、合并 Manifest。
 * 
 * 阶段 02 注入 stub Registry，阶段 05 替换真实 Registry 后补齐存在性校验。
 */
class OrthogonalRegistryImpl(
    private val engineRegistry: EngineRegistry,
    private val layoutRegistry: LayoutRegistry,
    private val dictionaryRegistry: DictionaryRegistry,
    private val engineResourceResolver: EngineResourceResolver
) : OrthogonalRegistry {
    
    private val localeCapabilities = mutableMapOf<LocaleTag, LocaleCapability>()
    private val schemaCapabilities = mutableMapOf<OrthogonalState, SchemaCapability>()
    
    override fun register(manifest: LanguageManifest): RegisterResult {
        val errors = mutableListOf<String>()
        
        // 1. 校验 manifestVersion
        if (manifest.manifestVersion != 1) {
            errors.add("Unsupported manifest version: ${manifest.manifestVersion}")
        }
        
        // 2. 校验 defaults
        val defaultScript = manifest.defaults.script
        val defaultSchema = manifest.defaults.schema
        
        if (!manifest.scripts.containsKey(defaultScript)) {
            errors.add("Default script '$defaultScript' not found in scripts")
        }
        
        // 3. 校验每个 Script 和 Schema
        manifest.scripts.forEach { (script, scriptManifest) ->
            if (!scriptManifest.schemas.containsKey(scriptManifest.defaultSchema)) {
                errors.add("Default schema '${scriptManifest.defaultSchema}' not found in script '$script'")
            }
            
            scriptManifest.schemas.forEach { (schema, schemaCap) ->
                // 校验 engineId 存在性（阶段 05 补齐真实存在性校验）
                // 当前只做格式校验
                if (schemaCap.engineId.isBlank()) {
                    errors.add("Schema '$schema' has blank engineId")
                }
                
                // 校验 layoutId 非空
                if (schemaCap.layoutId.isBlank()) {
                    errors.add("Schema '$schema' has blank layoutId")
                }
                
                // 校验 supportsShift 必须显式声明
                // 注意：Boolean 默认值是 false，所以不需要显式检查
                
                // 校验 candidatePolicy 非空
                if (schemaCap.candidatePolicy.isBlank()) {
                    errors.add("Schema '$schema' has blank candidatePolicy")
                }
                
                // 校验引擎特定字段
                when (schemaCap.engineId) {
                    "direct" -> {
                        if (schemaCap.mapping.isNullOrBlank()) {
                            errors.add("Schema '$schema' with 'direct' engine must have mapping")
                        }
                    }
                    "table_composing" -> {
                        if (schemaCap.encoderId.isNullOrBlank()) {
                            errors.add("Schema '$schema' with 'table_composing' engine must have encoderId")
                        }
                        if (schemaCap.dictionary.isNullOrBlank() && !schemaCap.dictionaryOptional) {
                            errors.add("Schema '$schema' with 'table_composing' engine must have dictionary or set dictionaryOptional=true")
                        }
                    }
                    "transliteration" -> {
                        if (schemaCap.fsm.isNullOrBlank()) {
                            errors.add("Schema '$schema' with 'transliteration' engine must have fsm")
                        }
                    }
                }
                
                // 校验 subtype labelKey 非空
                schemaCap.subtype?.let { subtype ->
                    if (subtype.labelKey.isBlank()) {
                        errors.add("Schema '$schema' subtype has blank labelKey")
                    }
                }
            }
        }
        
        if (errors.isNotEmpty()) {
            return RegisterResult.Failed(errors)
        }
        
        // 4. 注册成功，构建能力索引
        val locale = manifest.locale
        
        val scripts = manifest.scripts.mapValues { (script, scriptManifest) ->
            ScriptCapability(
                script = script,
                defaultSchema = scriptManifest.defaultSchema,
                schemas = scriptManifest.schemas
            )
        }
        
        val localeCap = LocaleCapability(
            locale = locale,
            displayName = manifest.displayName,
            scripts = scripts,
            defaults = manifest.defaults
        )
        
        // 5. 构建状态 -> SchemaCapability 映射
        manifest.scripts.forEach { (script, scriptManifest) ->
            scriptManifest.schemas.forEach { (schema, schemaCap) ->
                val state = OrthogonalState(locale, script, schema)
                schemaCapabilities[state] = schemaCap
            }
        }
        
        localeCapabilities[locale] = localeCap
        
        return RegisterResult.Success(manifest.packageId)
    }
    
    override fun unregister(packageId: String): RegisterResult {
        // TODO: 实现按 packageId 清除能力
        return RegisterResult.Success(packageId)
    }
    
    override fun getLocale(locale: LocaleTag): LocaleCapability? {
        return localeCapabilities[locale]
    }
    
    override fun isSupported(state: OrthogonalState): Boolean {
        return schemaCapabilities.containsKey(state)
    }
    
    override fun defaultState(locale: LocaleTag): OrthogonalState? {
        val localeCap = localeCapabilities[locale] ?: return null
        val defaults = localeCap.defaults
        
        return OrthogonalState(
            locale = locale,
            script = defaults.script,
            schema = defaults.schema
        )
    }
    
    override fun defaultSchema(locale: LocaleTag, script: Script): Schema? {
        val localeCap = localeCapabilities[locale] ?: return null
        val scriptCap = localeCap.scripts[script] ?: return null
        return scriptCap.defaultSchema
    }
    
    override fun schemaCapability(state: OrthogonalState): SchemaCapability? {
        return schemaCapabilities[state]
    }
}
