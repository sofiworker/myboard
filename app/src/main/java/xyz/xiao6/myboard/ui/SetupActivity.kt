package xyz.xiao6.myboard.ui

import android.os.Bundle
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.Secure
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.ime.MyBoardImeService
import xyz.xiao6.myboard.manager.LayoutManager
import xyz.xiao6.myboard.manager.SubtypeManager
import xyz.xiao6.myboard.model.LocaleLayoutProfile
import xyz.xiao6.myboard.store.SettingsStore
import xyz.xiao6.myboard.ui.theme.MyBoardTheme
import xyz.xiao6.myboard.ui.theme.DesignTokens
import java.util.Locale

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** 首次安装引导（Setup Wizard Activity）。 */
class SetupActivity : AppCompatActivity() {
    private lateinit var prefs: SettingsStore
    private lateinit var subtypeManager: SubtypeManager
    private lateinit var layoutManager: LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = SettingsStore(this)
        subtypeManager = SubtypeManager(this).loadAll()
        layoutManager = LayoutManager(this).loadAll()

        setContent {
            MyBoardTheme {
                OnboardingWizard(
                    prefs = prefs,
                    subtypeManager = subtypeManager,
                    layoutManager = layoutManager,
                    isImeEnabled = { isMyBoardEnabled() },
                    isImeSelected = { isMyBoardSelectedAsDefault() },
                    openImeSettings = { openImeSettings() },
                    showImePicker = { showImePicker() },
                    finishActivity = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                        finish()
                    },
                )
            }
        }
    }

    private fun isMyBoardEnabled(): Boolean {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
        val myId = resolveMyBoardImeId(imm) ?: return false
        return imm.enabledInputMethodList.any { it.id == myId }
    }

    private fun isMyBoardSelectedAsDefault(): Boolean {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
        val myId = resolveMyBoardImeId(imm) ?: return false
        val current = Secure.getString(contentResolver, Secure.DEFAULT_INPUT_METHOD)?.trim().orEmpty()
        return current == myId
    }

    private fun resolveMyBoardImeId(imm: InputMethodManager): String? {
        val expectedPackage = packageName
        val expectedServiceName = MyBoardImeService::class.java.name
        val info = imm.inputMethodList.firstOrNull { imi ->
                val si = imi.serviceInfo
                val className = if (si.name.startsWith(".")) si.packageName + si.name else si.name
                si.packageName == expectedPackage && className == expectedServiceName
            }
        return info?.id
    }

    private fun openImeSettings() {
        runCatching {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }.onFailure {
            Toast.makeText(this, getString(R.string.onboarding_error_open_settings_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImePicker() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        if (imm == null) {
            Toast.makeText(this, getString(R.string.onboarding_error_imm_unavailable), Toast.LENGTH_SHORT).show()
            return
        }
        imm.showInputMethodPicker()
    }
}

private enum class WizardStep {
    WELCOME,
    ENABLE_IME,
    PICK_IME,
    PERMISSIONS,
    CONFIGURATION,
    READY
}

@OptIn(ExperimentalPagerApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingWizard(
    prefs: SettingsStore,
    subtypeManager: SubtypeManager,
    layoutManager: LayoutManager,
    isImeEnabled: () -> Boolean,
    isImeSelected: () -> Boolean,
    openImeSettings: () -> Unit,
    showImePicker: () -> Unit,
    finishActivity: () -> Unit,
) {
    val context = LocalContext.current
    val steps = remember { WizardStep.entries.toList() }
    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    var imeEnabled by remember { mutableStateOf(false) }
    var imeSelected by remember { mutableStateOf(false) }
    var selectedLocaleTag by remember { mutableStateOf<String?>(null) }
    var enabledLayoutIds by remember { mutableStateOf<List<String>>(emptyList()) }

    fun refreshImeState() {
        val oldEnabled = imeEnabled
        val oldSelected = imeSelected
        imeEnabled = isImeEnabled()
        imeSelected = isImeSelected()
        
        // Auto-advance logic for smoother flow
        if (pagerState.currentPage == 1 && imeEnabled && !oldEnabled) {
            scope.launch { pagerState.animateScrollToPage(2) }
        } else if (pagerState.currentPage == 2 && imeSelected && !oldSelected) {
            scope.launch { pagerState.animateScrollToPage(3) }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        refreshImeState()
        val obs = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) refreshImeState()
            }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    DisposableEffect(context) {
        val resolver = context.contentResolver
        val handler = Handler(Looper.getMainLooper())
        val observer = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean) {
                    refreshImeState()
                }
            }

        resolver.registerContentObserver(Secure.getUriFor(Secure.DEFAULT_INPUT_METHOD), false, observer)
        resolver.registerContentObserver(Secure.getUriFor(Secure.ENABLED_INPUT_METHODS), false, observer)
        onDispose { resolver.unregisterContentObserver(observer) }
    }

    fun ensureInitialSelections() {
        if (selectedLocaleTag.isNullOrBlank()) {
            selectedLocaleTag = prefs.userLocaleTag
                    ?: subtypeManager.resolve(Locale.getDefault())?.localeTag
                    ?: subtypeManager.listAll().firstOrNull()?.localeTag
        }
        val localeTag = selectedLocaleTag ?: return
        val profile = subtypeManager.get(localeTag) ?: return
        val enabled = prefs.getEnabledLayoutIds(localeTag).takeIf { it.isNotEmpty() } ?: profile.layoutIds
        enabledLayoutIds = enabled.filter { it in profile.layoutIds }

        val preferred = prefs.getPreferredLayoutId(localeTag)
                ?.takeIf { it in enabledLayoutIds }
                ?: profile.defaultLayoutId?.takeIf { it in enabledLayoutIds }
                ?: enabledLayoutIds.firstOrNull()
        
        prefs.userLocaleTag = localeTag
        prefs.setEnabledLocaleTags(listOf(localeTag))
        prefs.setEnabledLayoutIds(localeTag, enabledLayoutIds)
        if (!preferred.isNullOrBlank()) prefs.setPreferredLayoutId(localeTag, preferred)
    }

    DisposableEffect(Unit) {
        ensureInitialSelections()
        onDispose {}
    }

    LaunchedEffect(selectedLocaleTag) {
        val localeTag = selectedLocaleTag ?: return@LaunchedEffect
        prefs.userLocaleTag = localeTag
        prefs.setEnabledLocaleTags(listOf(localeTag))
        val profile = subtypeManager.get(localeTag) ?: return@LaunchedEffect
        val enabled = prefs.getEnabledLayoutIds(localeTag).takeIf { it.isNotEmpty() } ?: profile.layoutIds
        enabledLayoutIds = enabled.filter { it in profile.layoutIds }
        prefs.setEnabledLayoutIds(localeTag, enabledLayoutIds)

        val preferred = prefs.getPreferredLayoutId(localeTag)
                ?.takeIf { it in enabledLayoutIds }
                ?: profile.defaultLayoutId?.takeIf { it in enabledLayoutIds }
                ?: enabledLayoutIds.firstOrNull()
        if (!preferred.isNullOrBlank()) prefs.setPreferredLayoutId(localeTag, preferred)
    }

    fun goNextOrFinish() {
        val step = steps[pagerState.currentPage]
        if (step == WizardStep.READY) {
            prefs.onboardingCompleted = true
            finishActivity()
            return
        }
        val next = (pagerState.currentPage + 1).coerceAtMost(steps.lastIndex)
        scope.launch { pagerState.animateScrollToPage(next) }
    }

    Scaffold(
        bottomBar = {
            val step = steps[pagerState.currentPage]
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step != WizardStep.WELCOME && step != WizardStep.READY) {
                        TextButton(
                            onClick = {
                                val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                                scope.launch { pagerState.animateScrollToPage(prev) }
                            }
                        ) {
                            Text(stringResource(R.string.onboarding_back))
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Button(
                        onClick = { goNextOrFinish() },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                        enabled = when(step) {
                            WizardStep.ENABLE_IME -> imeEnabled
                            WizardStep.PICK_IME -> imeSelected
                            else -> true
                        }
                    ) {
                        Text(
                            text = if (step == WizardStep.WELCOME) "Get Started"
                                   else if (step == WizardStep.READY) "Finish"
                                   else stringResource(R.string.onboarding_next),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            // Progress Indicator
            StepIndicator(
                currentIndex = pagerState.currentPage,
                totalSteps = steps.size,
                modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp)
            )

            HorizontalPager(
                count = steps.size,
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (steps[page]) {
                    WizardStep.WELCOME -> StepWelcome()
                    
                    WizardStep.ENABLE_IME -> StepAction(
                        title = stringResource(R.string.onboarding_enable_title),
                        desc = stringResource(R.string.onboarding_enable_desc),
                        icon = Icons.Default.Settings,
                        actionLabel = stringResource(R.string.onboarding_open_ime_settings),
                        completed = imeEnabled,
                        onAction = openImeSettings
                    )

                    WizardStep.PICK_IME -> StepAction(
                        title = stringResource(R.string.onboarding_pick_title),
                        desc = stringResource(R.string.onboarding_pick_desc),
                        icon = Icons.Default.Check,
                        actionLabel = stringResource(R.string.onboarding_show_ime_picker),
                        completed = imeSelected,
                        onAction = showImePicker
                    )

                    WizardStep.PERMISSIONS -> {
                        val hasMicPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                            if (isGranted) {
                                scope.launch { pagerState.animateScrollToPage(4) }
                            }
                        }
                        
                        StepAction(
                            title = "Microphone Access",
                            desc = "Needed for 100% offline voice-to-text input on the keyboard.",
                            icon = Icons.Default.Settings, // Using Settings icon as placeholder
                            actionLabel = "Grant Permission",
                            completed = hasMicPermission,
                            onAction = { launcher.launch(Manifest.permission.RECORD_AUDIO) }
                        )
                    }

                    WizardStep.CONFIGURATION -> StepConfiguration(
                        profiles = subtypeManager.listAll().filter { it.enabled && it.localeTag.isNotBlank() },
                        selectedLocaleTag = selectedLocaleTag,
                        enabledLayoutIds = enabledLayoutIds,
                        layoutManager = layoutManager,
                        onLocaleSelect = { tag -> selectedLocaleTag = tag },
                        onLayoutToggle = { layoutId ->
                            val tag = selectedLocaleTag ?: return@StepConfiguration
                            val profile = subtypeManager.get(tag)
                            val ordered = profile?.layoutIds.orEmpty()
                            val current = enabledLayoutIds.toMutableSet()
                            if (layoutId in current) current.remove(layoutId) else current.add(layoutId)
                            val nextEnabled = ordered.filter { it in current }.distinct()
                            enabledLayoutIds = nextEnabled
                            prefs.setEnabledLayoutIds(tag, nextEnabled)
                        }
                    )

                    WizardStep.READY -> StepReady()
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(currentIndex: Int, totalSteps: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(totalSteps) { index ->
            val color = if (index <= currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun StepWelcome() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Welcome to MyBoard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "A modern, customizable keyboard designed for speed and elegance.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StepAction(
    title: String,
    desc: String,
    icon: ImageVector,
    actionLabel: String,
    completed: Boolean,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = if (completed) Color(0xFF4CAF50).copy(alpha = 0.1f) else MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    imageVector = if (completed) Icons.Default.Check else icon,
                    contentDescription = null,
                    tint = if (completed) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(28.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = desc,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        
        if (!completed) {
            Button(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(actionLabel)
            }
        } else {
            Surface(
                color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Completed", color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepConfiguration(
    profiles: List<LocaleLayoutProfile>,
    selectedLocaleTag: String?,
    enabledLayoutIds: List<String>,
    layoutManager: LayoutManager,
    onLocaleSelect: (String) -> Unit,
    onLayoutToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Text(
            text = "Personalize",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Select your language and preferred layouts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Primary Language", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(12.dp))
        
        // Language Selection Chips
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            profiles.forEach { profile ->
                val selected = profile.localeTag == selectedLocaleTag
                FilterChip(
                    selected = selected,
                    onClick = { onLocaleSelect(profile.localeTag) },
                    label = { Text(formatLocaleLabelComposable(profile.localeTag)) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Layouts", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(12.dp))
        
        val currentProfile = selectedLocaleTag?.let { tag -> profiles.find { it.localeTag == tag } }
        if (currentProfile != null) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentProfile.layoutIds.distinct()) { id ->
                    val layoutName = runCatching { layoutManager.getLayout(id)?.name }.getOrNull() ?: id
                    val isEnabled = id in enabledLayoutIds
                    
                    Surface(
                        onClick = { onLayoutToggle(id) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(layoutName, fontWeight = FontWeight.Bold)
                                Text(id, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Checkbox(checked = isEnabled, onCheckedChange = { onLayoutToggle(id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepReady() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = Color(0xFF4CAF50).copy(alpha = 0.1f)
        ) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = null,
                tint = Color(0xFF4CAF50),
                modifier = Modifier.padding(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "You're all set!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "MyBoard is ready to use. You can always change your settings later.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun formatLocaleLabelComposable(localeTag: String): String {
    val tag = normalizeLocaleTagComposable(localeTag)
    val display = Locale.forLanguageTag(tag).getDisplayName(Locale.getDefault()).ifBlank { tag }
    return display
}

@Composable
private fun normalizeLocaleTagComposable(tag: String?): String {
    val raw = tag?.trim().orEmpty().replace('_', '-')
    val parts = raw.split('-').filter { it.isNotBlank() }
    if (parts.isEmpty()) return ""
    val language = parts[0].lowercase(Locale.ROOT)
    val region = parts.getOrNull(1)?.uppercase(Locale.ROOT)
    return if (region.isNullOrBlank()) language else "$language-$region"
}