package com.droidbubble.app.ui.main

import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.foundation.lazy.rememberLazyListState

import com.droidbubble.app.island.M3CookieShape
import com.droidbubble.app.island.M3Cookie6Shape
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.SignalCellularAlt


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Menu
import android.view.HapticFeedbackConstants
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import java.util.Locale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ScreenSearchDesktop
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material3.RadioButton
import com.droidbubble.app.theme.ISLAND_FONTS
import com.droidbubble.app.model.DEFAULT_FEATURE_PRIORITIES
import com.droidbubble.app.model.DesignLanguage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayCircleOutline
import com.droidbubble.app.model.DEFAULT_MEDIA_LAYERS
import com.droidbubble.app.island.ExpandedMediaView
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.island.MediaBottomWaveOverlay
import com.droidbubble.app.island.extractMediaAccentColor
import com.droidbubble.app.island.createBlurredBitmap
import com.droidbubble.app.droidbar.MostUsedAppsHelper
import com.droidbubble.app.droidbar.BarAppInfo
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.VerticalSplit
import com.droidbubble.app.model.MediaInfo
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.droidbubble.app.model.IslandSettings
import com.droidbubble.app.service.IslandOverlayService
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import com.droidbubble.app.ui.liquid.LiquidActivitiesTab
import com.droidbubble.app.ui.liquid.LiquidAnimationSettingsScreen
import com.droidbubble.app.ui.liquid.LiquidAppearanceTab
import com.droidbubble.app.ui.liquid.LiquidGlassNavigationBar
import com.droidbubble.app.ui.liquid.LiquidHomeTab
import com.droidbubble.app.ui.liquid.LiquidNavItem
import com.droidbubble.app.ui.liquid.LiquidSettingsTab
import com.droidbubble.app.ui.liquid.LiquidShortcutsTab
import com.droidbubble.app.ui.liquid.getStringsForLanguage
import com.droidbubble.app.ui.liquid.AppStrings

enum class MainScreenPage {
    TOOLS_ROOT,
    ANIMATION_SETTINGS,
    DROIDBUBBLE,
    DIMENSIONS,
    APPEARANCE,
    GENERAL,
    MEDIA_PLAYER,
    DROIDBAR,
    DROIDBAR_APPS,
    TASKS
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateCardItem(
    updateState: MainScreenViewModel.UpdateState,
    onCheck: () -> Unit,
    onOpenSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDownloading = updateState is MainScreenViewModel.UpdateState.Downloading
    val isChecking = updateState is MainScreenViewModel.UpdateState.Checking
    val isAvailable = updateState is MainScreenViewModel.UpdateState.Available

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable(enabled = !isDownloading && !isChecking) {
                if (isAvailable) {
                    onOpenSheet()
                } else {
                    onCheck()
                }
            },
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 2.dp
    ) {
        ListItem(
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent,
                headlineColor = MaterialTheme.colorScheme.onPrimary,
                leadingIconColor = MaterialTheme.colorScheme.onPrimary,
                trailingIconColor = MaterialTheme.colorScheme.onPrimary
            ),
            headlineContent = {
                Text(
                    text = when (updateState) {
                        is MainScreenViewModel.UpdateState.Checking -> "Đang kiểm tra cập nhật..."
                        is MainScreenViewModel.UpdateState.UpToDate -> "Đã là phiên bản mới nhất"
                        is MainScreenViewModel.UpdateState.Available -> {
                            val rel = updateState.release
                            "Có bản cập nhật mới (${rel.version})"
                        }
                        is MainScreenViewModel.UpdateState.Downloading -> {
                            val p = (updateState.progress * 100).roundToInt()
                            "Đang tải bản cập nhật ($p%)..."
                        }
                        is MainScreenViewModel.UpdateState.Error -> "Kiểm tra cập nhật"
                        else -> "Kiểm tra cập nhật"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            leadingContent = {
                when (updateState) {
                    is MainScreenViewModel.UpdateState.Checking -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    }
                    is MainScreenViewModel.UpdateState.UpToDate -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Đã cập nhật",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    is MainScreenViewModel.UpdateState.Available -> {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Có bản cập nhật",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Kiểm tra cập nhật",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            trailingContent = {
                if (updateState is MainScreenViewModel.UpdateState.Available) {
                    val rel = updateState.release
                    if (rel.isBeta) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = "BETA",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    viewModel: MainScreenViewModel,
    onDeactivateLicense: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val settings by viewModel.settings.collectAsState()
    val pendingPermissions by viewModel.pendingPermissions.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val isLoadingApps by viewModel.isLoadingApps.collectAsState()
    val updateState by viewModel.updateState.collectAsState()
    val showUpdateSheet by viewModel.showUpdateSheet.collectAsState()

    val s = remember(settings.appLanguage) { getStringsForLanguage(settings.appLanguage) }
    var selectedBottomTab by remember { mutableIntStateOf(0) }

    var showAppFilterSheet by remember { mutableStateOf(false) }
    var appFilterQuery by remember { mutableStateOf("") }

    var showAnimationSettingsPage by remember { mutableStateOf(false) }
    var showDroidbubblePage by remember { mutableStateOf(false) }
    var showDimensionsPage by remember { mutableStateOf(false) }
    var showAppearancePage by remember { mutableStateOf(false) }
    var showGeneralPage by remember { mutableStateOf(false) }
    var showMediaPlayerPage by remember { mutableStateOf(false) }
    var showDroidbarPage by remember { mutableStateOf(false) }
    var showDroidbarAppsPage by remember { mutableStateOf(false) }
    var showTasksPage by remember { mutableStateOf(false) }

    var showVideoAnnouncementDialog by remember { mutableStateOf(AnnouncementManager.shouldShow(context)) }
    var showFeedbackSheet by remember { mutableStateOf(false) }
    var showConfigManagerDialog by remember { mutableStateOf(false) }
    var configJsonText by remember { mutableStateOf("") }
    var showQuickSearchDialog by remember { mutableStateOf(false) }
    var quickSearchQuery by remember { mutableStateOf("") }

    var feedbackType by remember { mutableStateOf(FeedbackType.BUG) }
    var feedbackTitle by remember { mutableStateOf("") }
    var feedbackDescription by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var attachmentName by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachmentUri = uri
            attachmentName = getAttachmentFileName(context, uri)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissions(context)
                viewModel.refreshUpdateStateOnResume(context)
                if ((settings.isEnabled || settings.droidbarEnabled) && PermissionHelper.isOverlayGranted(context)) {
                    IslandOverlayService.start(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val currentNavPage = when {
        showAnimationSettingsPage -> MainScreenPage.ANIMATION_SETTINGS
        showTasksPage -> MainScreenPage.TASKS
        showDroidbarAppsPage -> MainScreenPage.DROIDBAR_APPS
        showDroidbarPage -> MainScreenPage.DROIDBAR
        showMediaPlayerPage -> MainScreenPage.MEDIA_PLAYER
        showDimensionsPage -> MainScreenPage.DIMENSIONS
        showAppearancePage -> MainScreenPage.APPEARANCE
        showGeneralPage -> MainScreenPage.GENERAL
        showDroidbubblePage -> MainScreenPage.DROIDBUBBLE
        else -> MainScreenPage.TOOLS_ROOT
    }

    AnimatedContent(
        targetState = currentNavPage,
        transitionSpec = {
            val isNavigatingForward = initialState == MainScreenPage.TOOLS_ROOT && targetState != MainScreenPage.TOOLS_ROOT ||
                initialState == MainScreenPage.DROIDBAR && targetState == MainScreenPage.DROIDBAR_APPS ||
                initialState == MainScreenPage.APPEARANCE && targetState == MainScreenPage.MEDIA_PLAYER
            if (isNavigatingForward) {
                (slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.85f)
                ) + fadeIn(animationSpec = tween(220)))
                    .togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth / 3 },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.85f)
                        ) + fadeOut(animationSpec = tween(180))
                    )
            } else {
                (slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth / 3 },
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.85f)
                ) + fadeIn(animationSpec = tween(220)))
                    .togetherWith(
                        slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.85f)
                        ) + fadeOut(animationSpec = tween(180))
                    )
            }
        },
        label = "subPageTransition"
    ) { page ->
        when (page) {
            MainScreenPage.ANIMATION_SETTINGS -> {
                BackHandler(onBack = { showAnimationSettingsPage = false })
                LiquidAnimationSettingsScreen(
                    settings = settings,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onBack = { showAnimationSettingsPage = false }
                )
            }
            MainScreenPage.DIMENSIONS -> {
                DimensionsPage(
                    settings = settings,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onBack = { showDimensionsPage = false },
                    modifier = modifier
                )
            }
            MainScreenPage.APPEARANCE -> {
                AppearancePage(
                    settings = settings,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onNavigateToMediaPlayer = { showMediaPlayerPage = true },
                    onBack = { showAppearancePage = false },
                    modifier = modifier
                )
            }
            MainScreenPage.MEDIA_PLAYER -> {
                MediaPlayerCustomizationPage(
                    settings = settings,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onBack = { showMediaPlayerPage = false },
                    modifier = modifier
                )
            }
            MainScreenPage.DROIDBAR -> {
                DroidbarPage(
                    settings = settings,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onOpenAppsConfig = { showDroidbarAppsPage = true },
                    onBack = { showDroidbarPage = false },
                    modifier = modifier
                )
            }
            MainScreenPage.DROIDBAR_APPS -> {
                DroidbarAppsConfigPage(
                    settings = settings,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onBack = { showDroidbarAppsPage = false },
                    modifier = modifier
                )
            }
            MainScreenPage.TASKS -> {
                com.droidbubble.app.tasks.TasksPage(
                    onBack = { showTasksPage = false },
                    modifier = modifier
                )
            }
            MainScreenPage.GENERAL -> {
                GeneralPage(
                    settings = settings,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onBack = { showGeneralPage = false },
                    modifier = modifier
                )
            }
            MainScreenPage.DROIDBUBBLE -> {
                BackHandler(onBack = { showDroidbubblePage = false })
                DimensionsPage(
                    settings = settings,
                    onUpdateSettings = { viewModel.updateSettings(it) },
                    onBack = { showDroidbubblePage = false },
                    modifier = modifier
                )
            }
            MainScreenPage.TOOLS_ROOT -> {
                if (selectedBottomTab != 0) {
                    BackHandler(onBack = { selectedBottomTab = 0 })
                }
                val isLightTheme = settings.appThemeMode == "LIGHT"
                val navTabs = remember(s) {
                    listOf(
                        LiquidNavItem(s.tabHome, Icons.Default.Home),
                        LiquidNavItem(s.tabActivities, Icons.Default.Explore),
                        LiquidNavItem(s.tabShortcuts, Icons.Default.Apps),
                        LiquidNavItem(s.tabSettings, Icons.Default.Settings),
                        LiquidNavItem(s.tabAppearance, Icons.Default.Palette)
                    )
                }

                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        // Top Header Bar matching video UI
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (selectedBottomTab == 0) {
                                Column {
                                    Text(
                                        text = s.appTitle,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = s.appSubtitle,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.4.sp
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .clickable { showVideoAnnouncementDialog = true },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.PlayCircleOutline,
                                                contentDescription = "TikTok Video",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .clickable { showFeedbackSheet = true },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Send,
                                                contentDescription = "Feedback",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .clickable { showQuickSearchDialog = true },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Search",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                val tabTitle = when (selectedBottomTab) {
                                    1 -> s.tabActivities
                                    2 -> s.tabShortcuts
                                    3 -> s.tabSettings
                                    4 -> s.tabAppearance
                                    else -> s.appTitle
                                }
                                Text(
                                    text = tabTitle,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Surface(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .clickable { showQuickSearchDialog = true },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Tab Body
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            AnimatedContent(
                                targetState = selectedBottomTab,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(160))
                                },
                                label = "liquidTabContent"
                            ) { tabIndex ->
                                when (tabIndex) {
                                    0 -> LiquidHomeTab(
                                        settings = settings,
                                        strings = s,
                                        viewModel = viewModel,
                                        onOpenDroidbar = { showDroidbarPage = true },
                                        onOpenTasks = { showTasksPage = true },
                                        onOpenVideoAnnouncement = { showVideoAnnouncementDialog = true },
                                        onDeactivateLicense = onDeactivateLicense
                                    )
                                    1 -> LiquidActivitiesTab(
                                        settings = settings,
                                        strings = s,
                                        onUpdateSettings = { viewModel.updateSettings(it) },
                                        onOpenMediaPlayerCustomization = { showMediaPlayerPage = true }
                                    )
                                    2 -> LiquidShortcutsTab(
                                        settings = settings,
                                        strings = s,
                                        onUpdateSettings = { viewModel.updateSettings(it) },
                                        onOpenChainedTasks = { showTasksPage = true }
                                    )
                                    3 -> LiquidSettingsTab(
                                        settings = settings,
                                        strings = s,
                                        onToggleEnabled = { enabled -> viewModel.toggleEnabled(context, enabled) },
                                        onUpdateSettings = { viewModel.updateSettings(it) },
                                        onOpenAppFilterSheet = {
                                            viewModel.loadInstalledApps(context)
                                            showAppFilterSheet = true
                                        },
                                        onOpenAnimationSettings = { showAnimationSettingsPage = true },
                                        onOpenConfigManager = {
                                            viewModel.exportConfig { json ->
                                                configJsonText = json
                                                showConfigManagerDialog = true
                                            }
                                        }
                                    )
                                    4 -> LiquidAppearanceTab(
                                        settings = settings,
                                        strings = s,
                                        onUpdateSettings = { viewModel.updateSettings(it) }
                                    )
                                }
                            }
                        }
                    }

                    // Floating Liquid Glass Navigation Bar at Bottom
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        LiquidGlassNavigationBar(
                            items = navTabs,
                            selectedIndex = selectedBottomTab,
                            onItemClick = { idx ->
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                selectedBottomTab = idx
                            },
                            accentColor = MaterialTheme.colorScheme.primary,
                            isDark = !isLightTheme
                        )
                    }
                }
            }
        }
    }

    // Config Manager Dialog (Export / Import JSON)
    if (showConfigManagerDialog) {
        AlertDialog(
            onDismissRequest = { showConfigManagerDialog = false },
            title = {
                Text(
                    text = s.configManager,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = s.configManagerSub,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = configJsonText,
                        onValueChange = { configJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = RoundedCornerShape(14.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("Androidisland Config", configJsonText))
                                Toast.makeText(context, "Đã sao chép JSON vào bộ nhớ tạm!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Xuất JSON", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, configJsonText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ cấu hình"))
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importConfig(configJsonText) { ok ->
                            val msg = if (ok) "Đã nhập cấu hình thành công!" else "JSON không hợp lệ!"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (ok) showConfigManagerDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Nhập JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigManagerDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    // Quick Search Dialog
    if (showQuickSearchDialog) {
        val searchItems = remember(s) {
            listOf<Triple<String, String, Int>>(
                Triple(s.tabHome, s.geometryAndSizes, 0),
                Triple(s.tabActivities, s.actCharging, 1),
                Triple(s.tabActivities, s.actPlayer, 1),
                Triple(s.tabActivities, s.actNotifications, 1),
                Triple(s.tabActivities, s.actVoiceMemo, 1),
                Triple(s.tabActivities, s.actFlashlight, 1),
                Triple(s.tabShortcuts, s.gestureControl, 2),
                Triple(s.tabShortcuts, s.audioEqualizer, 2),
                Triple(s.tabSettings, s.powerSaver, 3),
                Triple(s.tabSettings, s.liquidMaterial, 3),
                Triple(s.tabSettings, s.languageLabel, 3),
                Triple(s.tabSettings, s.configManager, 3),
                Triple(s.tabAppearance, s.multitasking, 4),
                Triple(s.tabAppearance, s.themeLabel, 4),
                Triple(s.tabAppearance, s.accentColorLabel, 4),
                Triple(s.tabAppearance, s.dynamicLight, 4),
                Triple(s.tabAppearance, s.animationSettings, 4)
            )
        }
        val filtered = remember(quickSearchQuery, searchItems) {
            if (quickSearchQuery.isBlank()) searchItems
            else searchItems.filter {
                it.first.contains(quickSearchQuery, ignoreCase = true) ||
                    it.second.contains(quickSearchQuery, ignoreCase = true)
            }
        }
        AlertDialog(
            onDismissRequest = { showQuickSearchDialog = false },
            title = { Text("Tìm kiếm cài đặt", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = quickSearchQuery,
                        onValueChange = { quickSearchQuery = it },
                        placeholder = { Text("Nhập tên tính năng...") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered) { item ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedBottomTab = item.third
                                        showQuickSearchDialog = false
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(item.second, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(item.first, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQuickSearchDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }
    // App Filter Modal Bottom Sheet
    if (showAppFilterSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val coroutineScope = rememberCoroutineScope()

        ModalBottomSheet(
            onDismissRequest = { showAppFilterSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Bộ lọc thông báo theo ứng dụng",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = appFilterQuery,
                    onValueChange = { appFilterQuery = it },
                    placeholder = { Text("Tìm kiếm ứng dụng đã cài...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                val filteredApps = remember(installedApps, appFilterQuery) {
                    if (appFilterQuery.isBlank()) installedApps
                    else installedApps.filter {
                        it.appName.contains(appFilterQuery, ignoreCase = true) ||
                                it.packageName.contains(appFilterQuery, ignoreCase = true)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        ListItem(
                            headlineContent = { Text(app.appName, fontWeight = FontWeight.Medium) },
                            leadingContent = {
                                if (app.icon != null) {
                                    val bitmap = remember(app.icon) { app.icon.toBitmap(96, 96) }
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                } else {
                                    Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(36.dp))
                                }
                            },
                            trailingContent = {
                                Switch(
                                    checked = !app.isBlocked,
                                    onCheckedChange = {
                                        viewModel.toggleAppBlocked(app.packageName)
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Update Available Pop-up Shelf / Dialog
    if (showUpdateSheet) {
        val currentUpdate = (updateState as? MainScreenViewModel.UpdateState.Available)?.release
            ?: (updateState as? MainScreenViewModel.UpdateState.Downloading)?.release
        val downloadingProgress = (updateState as? MainScreenViewModel.UpdateState.Downloading)?.progress

        if (currentUpdate != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissUpdateSheet() },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Phiên bản ${currentUpdate.version}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (currentUpdate.isBeta) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {
                                        Text(
                                            text = "BETA",
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = currentUpdate.name.takeIf { it != currentUpdate.version && it != currentUpdate.tagName } ?: "Bản phát hành mới trên GitHub",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (currentUpdate.changelog.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Có gì mới",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = currentUpdate.changelog,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (downloadingProgress != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Đang tải APK...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${(downloadingProgress * 100).roundToInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { downloadingProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.dismissUpdateSheet() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Để sau")
                        }

                        Button(
                            onClick = {
                                viewModel.downloadAndInstallUpdate(context, currentUpdate)
                            },
                            enabled = downloadingProgress == null,
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (downloadingProgress != null) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Đang tải...")
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cài đặt cập nhật")
                            }
                        }
                    }
                }
            }
        }
    }

    // Video & Changelog Announcement Dialog
    if (showVideoAnnouncementDialog) {
        VideoAnnouncementDialog(
            onDismiss = {
                AnnouncementManager.markDismissed(context)
                showVideoAnnouncementDialog = false
            }
        )
    }

    // Feedback Bottom Sheet Dialog
    if (showFeedbackSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFeedbackSheet = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Gửi phản hồi",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Báo lỗi hoặc đóng góp ý tưởng tính năng cho Androidisland (Tác giả: digitalxiao).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Category Selection: Bug Report vs Feature Suggestion
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = feedbackType == FeedbackType.BUG,
                        onClick = { feedbackType = FeedbackType.BUG },
                        label = { Text("Báo lỗi", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.BugReport,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = feedbackType == FeedbackType.FEATURE,
                        onClick = { feedbackType = FeedbackType.FEATURE },
                        label = { Text("Đề xuất tính năng", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lightbulb,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Subject / Title input
                OutlinedTextField(
                    value = feedbackTitle,
                    onValueChange = { feedbackTitle = it },
                    label = { Text("Tiêu đề") },
                    placeholder = {
                        Text(
                            if (feedbackType == FeedbackType.BUG) "VD: Lỗi thanh trượt trình phát nhạc"
                            else "VD: Thêm hỗ trợ đảo trên màn hình khóa"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Detailed Description input
                OutlinedTextField(
                    value = feedbackDescription,
                    onValueChange = { feedbackDescription = it },
                    label = { Text("Mô tả chi tiết *") },
                    placeholder = {
                        Text(
                            if (feedbackType == FeedbackType.BUG) "Mô tả lỗi đã xảy ra và cách tái hiện lỗi..."
                            else "Mô tả ý tưởng của bạn và cách thức hoạt động..."
                        )
                    },
                    minLines = 4,
                    maxLines = 7,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Attachment Section (optional)
                if (attachmentUri == null) {
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Đính kèm tệp (Ảnh chụp màn hình / Log)", fontSize = 13.sp)
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = attachmentName ?: "Tệp đính kèm",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    attachmentUri = null
                                    attachmentName = null
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Xóa tệp đính kèm",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Buttons: Cancel & Send Email
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showFeedbackSheet = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Hủy")
                    }

                    Button(
                        onClick = {
                            val subject = "[Androidisland ${if (feedbackType == FeedbackType.BUG) "Lỗi" else "Tính năng"}] ${feedbackTitle.ifBlank { if (feedbackType == FeedbackType.BUG) "Báo cáo lỗi" else "Đề xuất tính năng" }}"
                            val body = buildString {
                                appendLine("Loại: ${if (feedbackType == FeedbackType.BUG) "Báo cáo lỗi" else "Đề xuất tính năng"}")
                                if (feedbackTitle.isNotBlank()) {
                                    appendLine("Tiêu đề: $feedbackTitle")
                                }
                                appendLine()
                                appendLine("Mô tả:")
                                appendLine(feedbackDescription.trim())
                                appendLine()
                                appendLine("----------------------------------------")
                                appendLine("Thông tin Thiết bị & Ứng dụng:")
                                appendLine("Phiên bản Androidisland: ${com.droidbubble.app.util.UpdateManager.getCurrentVersion(context)} (${context.packageName}) - Tác giả: digitalxiao")
                                appendLine("Thiết bị: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
                                appendLine("Phiên bản Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                                appendLine("Trạng thái Đảo động: ${settings.isEnabled}")
                                appendLine("----------------------------------------")
                            }

                            val hasAttachment = attachmentUri != null
                            val emailIntent = if (hasAttachment) {
                                Intent(Intent.ACTION_SEND).apply {
                                    type = context.contentResolver.getType(attachmentUri!!) ?: "*/*"
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("digitalxiao@gmail.com"))
                                    putExtra(Intent.EXTRA_SUBJECT, subject)
                                    putExtra(Intent.EXTRA_TEXT, body)
                                    putExtra(Intent.EXTRA_STREAM, attachmentUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            } else {
                                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:digitalxiao@gmail.com")).apply {
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("digitalxiao@gmail.com"))
                                    putExtra(Intent.EXTRA_SUBJECT, subject)
                                    putExtra(Intent.EXTRA_TEXT, body)
                                }
                            }

                            // Copy details to clipboard as backup in case user wants to paste elsewhere
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("Phản hồi Androidisland", body))
                            } catch (_: Exception) {}

                            val gmailIntent = Intent(emailIntent).setPackage("com.google.android.gm").apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                if (hasAttachment) {
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                            }

                            try {
                                if (context.packageManager.resolveActivity(gmailIntent, 0) != null) {
                                    context.startActivity(gmailIntent)
                                    Toast.makeText(context, "Đang mở Gmail để gửi tới digitalxiao@gmail.com", Toast.LENGTH_SHORT).show()
                                } else {
                                    val chooser = Intent.createChooser(emailIntent, "Gửi phản hồi qua Email").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        if (hasAttachment) {
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                    }
                                    context.startActivity(chooser)
                                    Toast.makeText(context, "Chọn ứng dụng email để gửi tới digitalxiao@gmail.com", Toast.LENGTH_SHORT).show()
                                }
                            } catch (_: Exception) {
                                val fallback = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:digitalxiao@gmail.com?subject=" + Uri.encode(subject) + "&body=" + Uri.encode(body))).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                runCatching {
                                    context.startActivity(fallback)
                                    Toast.makeText(context, "Đang mở ứng dụng email...", Toast.LENGTH_SHORT).show()
                                }
                            }

                            feedbackTitle = ""
                            feedbackDescription = ""
                            attachmentUri = null
                            attachmentName = null
                            showFeedbackSheet = false
                        },
                        enabled = feedbackDescription.isNotBlank(),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gửi Email")
                    }
                }
            }
        }
    }


}

@Composable
fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    badge: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (badge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeColor.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
    }
}

enum class GroupPosition {
    TOP,
    MIDDLE,
    BOTTOM,
    SINGLE
}

fun getGroupedShape(position: GroupPosition): RoundedCornerShape {
    val outerRadius = 22.dp
    val innerRadius = 4.dp
    return when (position) {
        GroupPosition.TOP -> RoundedCornerShape(
            topStart = outerRadius,
            topEnd = outerRadius,
            bottomStart = innerRadius,
            bottomEnd = innerRadius
        )
        GroupPosition.MIDDLE -> RoundedCornerShape(innerRadius)
        GroupPosition.BOTTOM -> RoundedCornerShape(
            topStart = innerRadius,
            topEnd = innerRadius,
            bottomStart = outerRadius,
            bottomEnd = outerRadius
        )
        GroupPosition.SINGLE -> RoundedCornerShape(outerRadius)
    }
}

fun getHorizontalSegmentShape(index: Int, total: Int, isSelected: Boolean): RoundedCornerShape {
    val outerRadius = 22.dp
    val innerRadius = 4.dp
    return if (isSelected) {
        RoundedCornerShape(outerRadius)
    } else {
        when {
            index == 0 -> RoundedCornerShape(
                topStart = outerRadius,
                bottomStart = outerRadius,
                topEnd = innerRadius,
                bottomEnd = innerRadius
            )
            index == total - 1 -> RoundedCornerShape(
                topStart = innerRadius,
                bottomStart = innerRadius,
                topEnd = outerRadius,
                bottomEnd = outerRadius
            )
            else -> RoundedCornerShape(innerRadius)
        }
    }
}

@Composable
fun SettingsCardItem(
    position: GroupPosition,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    content: @Composable () -> Unit
) {
    val effectiveShape = if (isActive) {
        RoundedCornerShape(20.dp)
    } else {
        getGroupedShape(position)
    }

    val animatedColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "itemBgColor"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = effectiveShape,
        color = animatedColor,
        tonalElevation = if (isActive) 3.dp else 1.dp
    ) {
        content()
    }
}

@Composable
fun SliderSettingItem(
    label: String,
    value: Float,
    unit: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${if (unit == "x" || unit == "dp") String.format(Locale.US, "%.1f", value).replace(".0", "") else value.toInt()} $unit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.height(24.dp)
        )
    }
}

enum class FeedbackType {
    BUG,
    FEATURE
}

private fun getAttachmentFileName(context: Context, uri: Uri): String {
    var name = "attachment"
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
    } catch (_: Exception) {
    }
    return name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DimensionsPage(
    settings: IslandSettings,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Kích thước & Vị trí Đảo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                val currentCutoutMode = when {
                    settings.isTeardropCutout -> CutoutModeOption.TEARDROP
                    settings.isLeftCutout -> CutoutModeOption.LEFT
                    else -> CutoutModeOption.CENTER
                }
                CutoutPositionSelectorCard(
                    selectedMode = currentCutoutMode,
                    onSelectMode = { mode ->
                        onUpdateSettings { s ->
                            when (mode) {
                                CutoutModeOption.CENTER -> s.copy(isLeftCutout = false, isTeardropCutout = false)
                                CutoutModeOption.LEFT -> s.copy(isLeftCutout = true, isTeardropCutout = false)
                                CutoutModeOption.TEARDROP -> s.copy(isLeftCutout = false, isTeardropCutout = true)
                            }
                        }
                    }
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SettingsCardItem(position = GroupPosition.TOP) {
                        SliderSettingItem(
                            label = "Chiều rộng",
                            value = settings.widthDp.toFloat(),
                            unit = "dp",
                            valueRange = 50f..260f,
                            onValueChange = { onUpdateSettings { s -> s.copy(widthDp = it.roundToInt()) } }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE) {
                        SliderSettingItem(
                            label = "Chiều cao",
                            value = settings.heightDp.toFloat(),
                            unit = "dp",
                            valueRange = 12f..56f,
                            onValueChange = { onUpdateSettings { s -> s.copy(heightDp = it.roundToInt()) } }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE) {
                        SliderSettingItem(
                            label = "Độ bo góc",
                            value = settings.cornerRadiusDp.toFloat(),
                            unit = "dp",
                            valueRange = 10f..28f,
                            onValueChange = { onUpdateSettings { s -> s.copy(cornerRadiusDp = it.roundToInt()) } }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE) {
                        SliderSettingItem(
                            label = "Khoảng cách lề trên (Căn chỉnh Notch)",
                            value = settings.topOffsetDp.toFloat(),
                            unit = "dp",
                            valueRange = 0f..60f,
                            onValueChange = { onUpdateSettings { s -> s.copy(topOffsetDp = it.roundToInt()) } }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE) {
                        SliderSettingItem(
                            label = "Độ lệch ngang",
                            value = settings.horizontalOffsetDp.toFloat(),
                            unit = "dp",
                            valueRange = -50f..50f,
                            onValueChange = { onUpdateSettings { s -> s.copy(horizontalOffsetDp = it.roundToInt()) } }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE) {
                        SliderSettingItem(
                            label = "Tỷ lệ phóng to thẻ mở rộng",
                            value = settings.expandedScale,
                            unit = "x",
                            valueRange = 0.8f..1.3f,
                            steps = 5,
                            onValueChange = { onUpdateSettings { s -> s.copy(expandedScale = (it * 10).roundToInt() / 10f) } }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.BOTTOM) {
                        SliderSettingItem(
                            label = "Tốc độ chuyển động",
                            value = settings.animationSpeed,
                            unit = "x",
                            valueRange = 0.5f..2.0f,
                            steps = 14,
                            onValueChange = { onUpdateSettings { s -> s.copy(animationSpeed = (it * 10).roundToInt() / 10f) } }
                        )
                    }
                }
            }
        }
    }
}

enum class CutoutModeOption {
    CENTER,
    LEFT,
    TEARDROP
}

@Composable
fun CutoutPositionSelectorCard(
    selectedMode: CutoutModeOption,
    onSelectMode: (CutoutModeOption) -> Unit
) {
    SettingsCardItem(position = GroupPosition.SINGLE) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Vị trí khuyết Camera",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CutoutPositionOptionButton(
                    label = "Ở giữa",
                    mode = CutoutModeOption.CENTER,
                    isSelected = selectedMode == CutoutModeOption.CENTER,
                    onClick = { onSelectMode(CutoutModeOption.CENTER) },
                    modifier = Modifier.weight(1f)
                )
                CutoutPositionOptionButton(
                    label = "Bên trái",
                    mode = CutoutModeOption.LEFT,
                    isSelected = selectedMode == CutoutModeOption.LEFT,
                    onClick = { onSelectMode(CutoutModeOption.LEFT) },
                    modifier = Modifier.weight(1f)
                )
                CutoutPositionOptionButton(
                    label = "Giọt nước",
                    mode = CutoutModeOption.TEARDROP,
                    isSelected = selectedMode == CutoutModeOption.TEARDROP,
                    onClick = { onSelectMode(CutoutModeOption.TEARDROP) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CutoutPositionOptionButton(
    label: String,
    mode: CutoutModeOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
        },
        label = "cutoutBtnBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        },
        label = "cutoutBtnBorder"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "cutoutBtnContent"
    )
    val bezelStrokeColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }
    val screenFillColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f)
    }
    val cutoutDotColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        modifier = modifier.height(106.dp),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp, bottom = 10.dp, start = 6.dp, end = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Cropped top phone bezel illustration with just the selfie camera circle (or tear-drop notch)
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .width(74.dp)
                    .height(50.dp)
            ) {
                val strokePx = 2.3.dp.toPx()
                val cornerPx = 13.dp.toPx()
                val padX = 4.dp.toPx()
                val topY = 4.dp.toPx()
                val bottomY = size.height
                val leftX = padX
                val rightX = size.width - padX

                val bezelFillPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(leftX, bottomY)
                    lineTo(leftX, topY + cornerPx)
                    quadraticTo(leftX, topY, leftX + cornerPx, topY)
                    lineTo(rightX - cornerPx, topY)
                    quadraticTo(rightX, topY, rightX, topY + cornerPx)
                    lineTo(rightX, bottomY)
                    close()
                }
                drawPath(path = bezelFillPath, color = screenFillColor)

                val bezelOutlinePath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(leftX, bottomY)
                    lineTo(leftX, topY + cornerPx)
                    quadraticTo(leftX, topY, leftX + cornerPx, topY)
                    lineTo(rightX - cornerPx, topY)
                    quadraticTo(rightX, topY, rightX, topY + cornerPx)
                    lineTo(rightX, bottomY)
                }
                drawPath(
                    path = bezelOutlinePath,
                    color = bezelStrokeColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokePx,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                    )
                )

                val centerX = size.width / 2f
                when (mode) {
                    CutoutModeOption.CENTER -> {
                        val holeY = topY + 12.dp.toPx()
                        val holeRadius = 4.2.dp.toPx()
                        drawCircle(
                            color = cutoutDotColor,
                            radius = holeRadius,
                            center = androidx.compose.ui.geometry.Offset(centerX, holeY)
                        )
                    }
                    CutoutModeOption.LEFT -> {
                        val holeY = topY + 12.dp.toPx()
                        val holeRadius = 4.2.dp.toPx()
                        val holeX = leftX + 14.dp.toPx()
                        drawCircle(
                            color = cutoutDotColor,
                            radius = holeRadius,
                            center = androidx.compose.ui.geometry.Offset(holeX, holeY)
                        )
                    }
                    CutoutModeOption.TEARDROP -> {
                        // Outward-rounded top corners blending from the top bezel into a centered tear-drop semicircle
                        val dropR = 7.dp.toPx()
                        val earR = 5.dp.toPx()
                        val dropBottomY = topY + dropR * 1.45f
                        val teardropPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(centerX - dropR - earR, topY)
                            quadraticTo(
                                centerX - dropR, topY,
                                centerX - dropR * 0.88f, topY + dropR * 0.55f
                            )
                            cubicTo(
                                centerX - dropR * 0.65f, dropBottomY,
                                centerX + dropR * 0.65f, dropBottomY,
                                centerX + dropR * 0.88f, topY + dropR * 0.55f
                            )
                            quadraticTo(
                                centerX + dropR, topY,
                                centerX + dropR + earR, topY
                            )
                            close()
                        }
                        drawPath(path = teardropPath, color = cutoutDotColor)
                    }
                }
            }

            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearancePage(
    settings: IslandSettings,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit,
    onNavigateToMediaPlayer: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var showFontDialog by remember { mutableStateOf(false) }

    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("Chọn phông chữ", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ISLAND_FONTS.forEach { fontOption ->
                        val isSelected = (settings.fontKey == fontOption.key) || (settings.fontKey.isBlank() && fontOption.key == "DEFAULT")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    onUpdateSettings { it.copy(fontKey = fontOption.key) }
                                    showFontDialog = false
                                }
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = fontOption.name,
                                fontFamily = fontOption.family,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    onUpdateSettings { it.copy(fontKey = fontOption.key) }
                                    showFontDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontDialog = false }) {
                    Text("Xong")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Giao diện & Phản hồi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val isGlass = settings.designLanguage == DesignLanguage.GLASS
                    SettingsCardItem(
                        position = if (isGlass) GroupPosition.TOP else GroupPosition.SINGLE,
                        isActive = false
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Phong cách thiết kế", fontWeight = FontWeight.SemiBold)
                                        if (settings.designLanguage == DesignLanguage.GLASS) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                contentColor = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    text = "Thử nghiệm",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                },
                                leadingContent = {
                                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            )

                            // 3-way Segmented Control (3 separate buttons with list-item style rounding)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                val languages = listOf(
                                    DesignLanguage.MATERIAL3,
                                    DesignLanguage.DROIDBUBBLE,
                                    DesignLanguage.GLASS
                                )
                                languages.forEachIndexed { index, lang ->
                                    val isSelected = settings.designLanguage == lang
                                    val shape = getHorizontalSegmentShape(index, languages.size, isSelected)
                                    val backgroundColor by animateColorAsState(
                                        targetValue = if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        },
                                        label = "lang_bg"
                                    )
                                    val contentColor by animateColorAsState(
                                        targetValue = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        label = "lang_text"
                                    )

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(shape)
                                            .clickable {
                                                onUpdateSettings {
                                                    it.copy(
                                                        designLanguage = lang,
                                                        glassEnabled = (lang == DesignLanguage.GLASS),
                                                        showOutline = if (lang == DesignLanguage.MATERIAL3) false else it.showOutline
                                                    )
                                                }
                                            },
                                        shape = shape,
                                        color = backgroundColor
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when (lang) {
                                                    DesignLanguage.DROIDBUBBLE -> "Androidisland"
                                                    DesignLanguage.GLASS -> "Kính (Glass)"
                                                    DesignLanguage.MATERIAL3 -> "Material 3"
                                                },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = contentColor,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isGlass) {
                        SettingsCardItem(position = GroupPosition.BOTTOM, isActive = false) {
                            SliderSettingItem(
                                label = "Độ mờ đục",
                                value = settings.glassOpacity.toFloat(),
                                unit = "%",
                                valueRange = 0f..100f,
                                onValueChange = { onUpdateSettings { s -> s.copy(glassOpacity = it.roundToInt()) } }
                            )
                        }
                    }
                }
            }

            // Font Picker moved directly below Design Language
            item {
                SettingsCardItem(position = GroupPosition.SINGLE) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { showFontDialog = true },
                        headlineContent = { Text("Đổi phông chữ", fontWeight = FontWeight.SemiBold) },
                        supportingContent = {
                            val currentFontName = remember(settings.fontKey) {
                                ISLAND_FONTS.firstOrNull { it.key == settings.fontKey }?.name ?: "Mặc định hệ thống"
                            }
                            Text(currentFontName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        leadingContent = {
                            Surface(
                                modifier = Modifier.size(38.dp),
                                shape = M3Cookie6Shape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.FontDownload, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "Đổi phông chữ",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            if (settings.designLanguage != DesignLanguage.MATERIAL3) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val outlineActive = settings.showOutline
                        SettingsCardItem(
                            position = if (outlineActive) GroupPosition.TOP else GroupPosition.SINGLE,
                            isActive = outlineActive
                        ) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = {
                                    Text(
                                        "Viền Đảo động",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.CropSquare,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingContent = {
                                    Switch(
                                        checked = settings.showOutline,
                                        onCheckedChange = { show ->
                                            onUpdateSettings { it.copy(showOutline = show) }
                                        }
                                    )
                                }
                            )
                        }

                        if (outlineActive) {
                            SettingsCardItem(position = GroupPosition.BOTTOM) {
                                SliderSettingItem(
                                    label = "Độ dày viền",
                                    value = settings.outlineThicknessDp,
                                    unit = "dp",
                                    valueRange = 0.5f..4.0f,
                                    onValueChange = { onUpdateSettings { s -> s.copy(outlineThicknessDp = (it * 10).roundToInt() / 10f) } }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SettingsCardItem(position = GroupPosition.TOP, isActive = settings.bubbleEffectEnabled) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Hiệu ứng bong bóng", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Icon(Icons.Default.RoundedCorner, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.bubbleEffectEnabled,
                                    onCheckedChange = { enabled ->
                                        onUpdateSettings { it.copy(bubbleEffectEnabled = enabled) }
                                    }
                                )
                            }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE, isActive = settings.showDualActivityInCompact) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Hoạt động kép khi thu gọn", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Hiển thị bong bóng phụ; khi tắt sẽ chỉ hiện hoạt động ưu tiên cho đến khi mở rộng", style = MaterialTheme.typography.bodySmall) },
                            leadingContent = {
                                Icon(Icons.Default.Widgets, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.showDualActivityInCompact,
                                    onCheckedChange = { showDual ->
                                        onUpdateSettings { it.copy(showDualActivityInCompact = showDual) }
                                    }
                                )
                            }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.BOTTOM, isActive = settings.alwaysShowPill) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Đảo động đen mặc định (Luôn hiện)", fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("Giữ viên thuốc màu đen thuần khiết trên vùng khuyết camera khi ở trạng thái chờ", style = MaterialTheme.typography.bodySmall) },
                            leadingContent = {
                                Icon(Icons.Default.FitScreen, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.alwaysShowPill,
                                    onCheckedChange = { alwaysShow ->
                                        onUpdateSettings { it.copy(alwaysShowPill = alwaysShow) }
                                    }
                                )
                            }
                        )
                    }
                }
            }

            item {
                SettingsCardItem(position = GroupPosition.SINGLE) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier.clickable { onNavigateToMediaPlayer() },
                        headlineContent = { Text("Tùy chỉnh Trình phát nhạc", fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("Xem trước, căn chỉnh nút, thứ tự lớp & hiệu ứng", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingContent = {
                            Surface(
                                modifier = Modifier.size(38.dp),
                                shape = M3Cookie6Shape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "Mở Tùy chỉnh Trình phát nhạc",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }



            item {
                AnimationCurveEditor(settings = settings, onUpdateSettings = onUpdateSettings)
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralPage(
    settings: IslandSettings,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    var showWeatherDialog by remember { mutableStateOf(false) }
    var weatherCityInput by remember(settings.weatherCity) { mutableStateOf(settings.weatherCity) }

    if (showWeatherDialog) {
        AlertDialog(
            onDismissRequest = { showWeatherDialog = false },
            title = { Text("Vị trí thời tiết") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Nhập tên thành phố hoặc địa điểm để hiển thị thời tiết (VD: Ha Noi, Ho Chi Minh, Da Nang). Để trống để tự động ước tính theo múi giờ.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = weatherCityInput,
                        onValueChange = { weatherCityInput = it },
                        label = { Text("Thành phố hoặc Địa phương") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateSettings { it.copy(weatherCity = weatherCityInput.trim()) }
                        showWeatherDialog = false
                    }
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWeatherDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt chung", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // SECTION: DISPLAY & BEHAVIOR
            item {
                SectionHeader(title = "Hiển thị & Hành vi")
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SettingsCardItem(position = GroupPosition.TOP, isActive = settings.hideOnLockScreen) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Ẩn trên màn hình khóa", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.hideOnLockScreen,
                                    onCheckedChange = { hide ->
                                        onUpdateSettings { it.copy(hideOnLockScreen = hide) }
                                    }
                                )
                            }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE, isActive = settings.showInLandscape) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Hiển thị khi xoay ngang", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.showInLandscape,
                                    onCheckedChange = { inLandscape ->
                                        onUpdateSettings { it.copy(showInLandscape = inLandscape) }
                                    }
                                )
                            }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.BOTTOM, isActive = settings.swapActivitiesOnSwipe) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Vuốt để đổi hoạt động", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Icon(Icons.Default.TouchApp, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.swapActivitiesOnSwipe,
                                    onCheckedChange = { swapEnabled ->
                                        onUpdateSettings { it.copy(swapActivitiesOnSwipe = swapEnabled) }
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // SECTION: INTERACTION & FEEDBACK
            item {
                SectionHeader(title = "Tương tác & Phản hồi")
            }

            item {
                SettingsCardItem(position = GroupPosition.SINGLE, isActive = settings.hapticsEnabled) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text("Phản hồi xúc giác (Rung)", fontWeight = FontWeight.SemiBold) },
                        leadingContent = {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        trailingContent = {
                            Switch(
                                checked = settings.hapticsEnabled,
                                onCheckedChange = { haptics ->
                                        onUpdateSettings { it.copy(hapticsEnabled = haptics) }
                                }
                            )
                        }
                    )
                }
            }

            // SECTION: NOTIFICATIONS
            item {
                SectionHeader(title = "Thông báo")
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SettingsCardItem(position = GroupPosition.TOP, isActive = settings.autoDismissNotifications) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Tự động ẩn thông báo", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.autoDismissNotifications,
                                    onCheckedChange = { autoDismiss ->
                                        onUpdateSettings { it.copy(autoDismissNotifications = autoDismiss) }
                                    }
                                )
                            }
                        )
                    }

                    if (settings.autoDismissNotifications) {
                        SettingsCardItem(position = GroupPosition.MIDDLE) {
                            SliderSettingItem(
                                label = "Thời gian tự động ẩn",
                                value = settings.notificationAutoDismissSeconds.toFloat(),
                                unit = "s",
                                valueRange = 1f..10f,
                                steps = 8,
                                onValueChange = { seconds ->
                                    onUpdateSettings { it.copy(notificationAutoDismissSeconds = seconds.roundToInt().coerceIn(1, 10)) }
                                }
                            )
                        }
                    }

                    SettingsCardItem(position = GroupPosition.BOTTOM) {
                        SliderSettingItem(
                            label = "Số dòng thông báo tối đa",
                            value = settings.notificationMaxLines.toFloat(),
                            unit = "dòng",
                            valueRange = 1f..6f,
                            steps = 4,
                            onValueChange = { lines ->
                                onUpdateSettings { it.copy(notificationMaxLines = lines.roundToInt().coerceIn(1, 6)) }
                            }
                        )
                    }
                }
            }

            // SECTION: SERVICES & FEATURES
            item {
                SectionHeader(title = "Dịch vụ & Tính năng")
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SettingsCardItem(position = GroupPosition.TOP, isActive = settings.dismissMediaOnPause) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Ẩn trình phát nhạc khi tạm dừng", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.dismissMediaOnPause,
                                    onCheckedChange = { dismiss ->
                                        onUpdateSettings { it.copy(dismissMediaOnPause = dismiss) }
                                    }
                                )
                            }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.BOTTOM) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable {
                                weatherCityInput = settings.weatherCity
                                showWeatherDialog = true
                            },
                            headlineContent = {
                                Text(
                                    if (settings.weatherCity.isBlank()) "Vị trí thời tiết" else "Vị trí thời tiết (${settings.weatherCity})",
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingContent = {
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = M3Cookie6Shape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            },
                            trailingContent = {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = "Đặt vị trí",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}


private fun springStep(damping: Float, stiffness: Float, t: Float): Float {
    val w = Math.sqrt(stiffness.toDouble())
    val z = damping.toDouble()
    return if (z < 1.0) {
        val wd = w * Math.sqrt(1.0 - z * z)
        (1.0 - Math.exp(-z * w * t) * (Math.cos(wd * t) + (z * w / wd) * Math.sin(wd * t))).toFloat()
    } else {
        (1.0 - Math.exp(-w * t) * (1.0 + w * t)).toFloat()
    }
}

@Composable
fun AnimationCurveEditor(
    settings: IslandSettings,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit
) {
    val targets = listOf("Mở rộng", "Xuất hiện", "Tách đôi")
    var selected by remember { mutableStateOf(0) }
    val defaults = listOf(0.74f to 380f, 0.60f to 280f, 0.63f to 320f)
    val damping = when (selected) { 0 -> settings.expandDamping; 1 -> settings.appearDamping; else -> settings.splitDamping }
    val stiffness = when (selected) { 0 -> settings.expandStiffness; 1 -> settings.appearStiffness; else -> settings.splitStiffness }
    val isDefault = damping == defaults[selected].first && stiffness == defaults[selected].second

    fun setCurve(d: Float, k: Float) {
        onUpdateSettings {
            when (selected) {
                0 -> it.copy(expandDamping = d, expandStiffness = k)
                1 -> it.copy(appearDamping = d, appearStiffness = k)
                else -> it.copy(splitDamping = d, splitStiffness = k)
            }
        }
    }

    SettingsCardItem(position = GroupPosition.SINGLE) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Animation, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                Text("Đường cong chuyển động", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TextButton(onClick = { setCurve(defaults[selected].first, defaults[selected].second) }, enabled = !isDefault) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Đặt lại")
                }
            }
            Spacer(Modifier.height(10.dp))

            // Expand / Appear / Split segmented buttons matching screen record & live progress cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                targets.forEachIndexed { i, label ->
                    val isSelected = selected == i
                    val shape = when (i) {
                        0 -> RoundedCornerShape(
                            topStart = 14.dp,
                            bottomStart = 14.dp,
                            topEnd = 4.dp,
                            bottomEnd = 4.dp
                        )
                        targets.size - 1 -> RoundedCornerShape(
                            topStart = 4.dp,
                            bottomStart = 4.dp,
                            topEnd = 14.dp,
                            bottomEnd = 14.dp
                        )
                        else -> RoundedCornerShape(4.dp)
                    }

                    FilledTonalButton(
                        onClick = { selected = i },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = shape,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f),
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            val lineColor = MaterialTheme.colorScheme.primary
            val guideColor = MaterialTheme.colorScheme.outlineVariant
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f))
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val padX = 14.dp.toPx()
                    val padY = 22.dp.toPx()
                    val w = size.width - padX * 2
                    val h = size.height - padY * 2
                    val maxV = 1.5f
                    val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    val targetY = padY + h * (1f - 1f / maxV)
                    drawLine(guideColor, androidx.compose.ui.geometry.Offset(padX, targetY), androidx.compose.ui.geometry.Offset(padX + w, targetY), strokeWidth = 1.5f, pathEffect = dash)
                    val baseY = padY + h
                    drawLine(guideColor, androidx.compose.ui.geometry.Offset(padX, baseY), androidx.compose.ui.geometry.Offset(padX + w, baseY), strokeWidth = 1.5f)
                    val path = androidx.compose.ui.graphics.Path()
                    val n = 80
                    for (i in 0..n) {
                        val tt = i / n.toFloat()
                        val v = springStep(damping, stiffness, tt * 1.0f).coerceIn(0f, maxV)
                        val x = padX + w * tt
                        val y = padY + h * (1f - v / maxV)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path, lineColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round
                        )
                    )
                }
            }

            val bounce = ((1f - damping) / 0.7f).coerceIn(0f, 1f)
            SliderSettingItemPct(
                label = "Độ nảy (Bounce)",
                valueText = "${(bounce * 100).toInt()}%",
                value = bounce,
                valueRange = 0f..1f,
                onValueChange = { b -> setCurve((1f - 0.7f * b), stiffness) }
            )
            SliderSettingItemPct(
                label = "Độ nhạy (Snap)",
                valueText = "${stiffness.toInt()}",
                value = stiffness,
                valueRange = 150f..700f,
                onValueChange = { k -> setCurve(damping, k.roundToInt().toFloat()) }
            )
        }
    }
}

@Composable
private fun SliderSettingItemPct(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(valueText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, modifier = Modifier.height(28.dp))
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPlayerCustomizationPage(
    settings: IslandSettings,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val activeMedia by IslandStateManager.activeMedia.collectAsState()
    val sampleArt = remember {
        val b = android.graphics.Bitmap.createBitmap(240, 240, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(b)
        val paint = android.graphics.Paint()
        paint.shader = android.graphics.LinearGradient(
            0f, 0f, 240f, 240f,
            intArrayOf(
                android.graphics.Color.parseColor("#E91E63"),
                android.graphics.Color.parseColor("#9C27B0"),
                android.graphics.Color.parseColor("#3F51B5")
            ),
            null,
            android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, 240f, 240f, paint)
        b
    }
    val dummyMedia = remember(sampleArt) {
        MediaInfo(
            packageName = "com.spotify.music",
            title = "Starboy",
            artist = "The Weeknd, Daft Punk",
            isPlaying = true,
            durationMs = 230000L,
            positionMs = 85000L,
            albumArt = sampleArt
        )
    }
    val currentMedia = activeMedia ?: dummyMedia
    val mediaArt = currentMedia.albumArt ?: currentMedia.appIcon ?: sampleArt
    val mediaAccentColor = remember(mediaArt) { extractMediaAccentColor(mediaArt) }
    val blurredMediaArt = remember(mediaArt, settings.blurredAlbumArtBackground) {
        if (settings.blurredAlbumArtBackground && mediaArt != null) createBlurredBitmap(mediaArt) else null
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Trình phát nhạc",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // SECTION 1: PREVIEW
            item {
                SectionHeader(title = "Xem trước")
            }

            item {
                val previewShape = RoundedCornerShape(28.dp)
                val cardBg = when {
                    settings.isMaterial3 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    settings.isGlass -> Color.Black.copy(alpha = (settings.glassOpacity / 100f).coerceIn(0.15f, 0.95f))
                    else -> Color(0xFF121214)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(188.dp)
                        .clip(previewShape)
                        .background(cardBg)
                ) {
                    if (settings.blurredAlbumArtBackground && mediaArt != null) {
                        Image(
                            bitmap = (blurredMediaArt ?: mediaArt).asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = 1.35f
                                    scaleY = 1.35f
                                    alpha = 0.78f
                                }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.38f))
                        )
                    }

                    if (settings.mediaWaveAnimation) {
                        MediaBottomWaveOverlay(
                            accentColor = if (mediaAccentColor != Color.Transparent) mediaAccentColor else Color(0xFF64D2FF),
                            isPlaying = currentMedia.isPlaying,
                            alphaMultiplier = 1f,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        ExpandedMediaView(
                            media = currentMedia,
                            onCollapse = {},
                            scale = 1.0f,
                            spinningAlbumArt = settings.spinningAlbumArt,
                            wavyProgressBar = settings.wavyProgressBar,
                            designLanguage = settings.designLanguage,
                            mediaControlAlignment = settings.mediaControlAlignment,
                            mediaLayersOrder = settings.mediaLayersOrder
                        )
                    }
                }
            }

            // SECTION 2: CONTROLS ALIGNMENT
            item {
                SectionHeader(title = "Căn chỉnh nút điều khiển")
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    val alignments = listOf(
                        "LEFT" to "Trái",
                        "CENTER" to "Giữa",
                        "RIGHT" to "Phải"
                    )
                    alignments.forEachIndexed { index, (key, label) ->
                        val isSelected = settings.mediaControlAlignment == key
                        val shape = getHorizontalSegmentShape(index, alignments.size, isSelected)
                        val backgroundColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            label = "align_bg"
                        )
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            label = "align_text"
                        )

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(shape)
                                .clickable {
                                    onUpdateSettings { it.copy(mediaControlAlignment = key) }
                                },
                            shape = shape,
                            color = backgroundColor
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = contentColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 3: LAYER ORDER
            item {
                SectionHeader(title = "Thứ tự hiển thị")
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val currentOrder = if (settings.mediaLayersOrder.isNotEmpty()) settings.mediaLayersOrder else DEFAULT_MEDIA_LAYERS
                    currentOrder.forEachIndexed { index, layerKey ->
                        val position = when {
                            currentOrder.size == 1 -> GroupPosition.SINGLE
                            index == 0 -> GroupPosition.TOP
                            index == currentOrder.size - 1 -> GroupPosition.BOTTOM
                            else -> GroupPosition.MIDDLE
                        }

                        val (title, icon) = when (layerKey) {
                            "COVER_AND_TITLE" -> "Ảnh bìa và tên bài hát" to Icons.Default.Album
                            "SLIDER" -> "Thanh trượt thời gian" to Icons.Default.LinearScale
                            "CONTROLS" -> "Các nút điều khiển" to Icons.Default.PlayCircleOutline
                            else -> layerKey to Icons.Default.Layers
                        }

                        SettingsCardItem(position = position) {
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
                                leadingContent = {
                                    Surface(
                                        modifier = Modifier.size(38.dp),
                                        shape = M3Cookie6Shape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ) {
                                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    val newOrder = currentOrder.toMutableList()
                                                    val temp = newOrder[index]
                                                    newOrder[index] = newOrder[index - 1]
                                                    newOrder[index - 1] = temp
                                                    onUpdateSettings { it.copy(mediaLayersOrder = newOrder) }
                                                }
                                            },
                                            enabled = index > 0
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Di chuyển lên",
                                                tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (index < currentOrder.size - 1) {
                                                    val newOrder = currentOrder.toMutableList()
                                                    val temp = newOrder[index]
                                                    newOrder[index] = newOrder[index + 1]
                                                    newOrder[index + 1] = temp
                                                    onUpdateSettings { it.copy(mediaLayersOrder = newOrder) }
                                                }
                                            },
                                            enabled = index < currentOrder.size - 1
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Di chuyển xuống",
                                                tint = if (index < currentOrder.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // SECTION 4: OPTIONS & ANIMATIONS
            item {
                SectionHeader(title = "Tùy chọn & Hiệu ứng")
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SettingsCardItem(position = GroupPosition.TOP, isActive = settings.mediaWaveAnimation) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Sóng nhạc theo màu ảnh bìa", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.mediaWaveAnimation,
                                    onCheckedChange = { enabled ->
                                        onUpdateSettings { it.copy(mediaWaveAnimation = enabled) }
                                    }
                                )
                            }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE, isActive = settings.wavyProgressBar) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Thanh tiến trình dạng sóng", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Icon(Icons.Default.LinearScale, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.wavyProgressBar,
                                    onCheckedChange = { enabled ->
                                        onUpdateSettings { it.copy(wavyProgressBar = enabled) }
                                    }
                                )
                            }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE, isActive = settings.spinningAlbumArt) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Ảnh bìa đĩa than xoay tròn", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Icon(Icons.Default.Album, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.spinningAlbumArt,
                                    onCheckedChange = { enabled ->
                                        onUpdateSettings { it.copy(spinningAlbumArt = enabled) }
                                    }
                                )
                            }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.BOTTOM, isActive = settings.blurredAlbumArtBackground) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Làm mờ ảnh bìa làm nền", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.blurredAlbumArtBackground,
                                    onCheckedChange = { blurred ->
                                        onUpdateSettings { it.copy(blurredAlbumArtBackground = blurred) }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroidbarPage(
    settings: IslandSettings,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit,
    onOpenAppsConfig: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Thanh cạnh Droidbar", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // MASTER ENABLE SWITCH
            item {
                SettingsCardItem(position = GroupPosition.SINGLE, isActive = settings.droidbarEnabled) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text("Bật Thanh cạnh Droidbar", fontWeight = FontWeight.SemiBold) },
                        leadingContent = {
                            Surface(
                                modifier = Modifier.size(38.dp),
                                shape = M3Cookie6Shape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.VerticalSplit, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        trailingContent = {
                            Switch(
                                checked = settings.droidbarEnabled,
                                onCheckedChange = { enabled ->
                                    onUpdateSettings { it.copy(droidbarEnabled = enabled) }
                                    if (enabled && PermissionHelper.isOverlayGranted(context)) {
                                        IslandOverlayService.start(context)
                                    }
                                }
                            )
                        }
                    )
                }
            }

            // CHEVRON TAB TO CONFIGURE APPS
            item {
                SettingsCardItem(position = GroupPosition.SINGLE) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text("Cấu hình ứng dụng hiển thị", fontWeight = FontWeight.SemiBold) },
                        supportingContent = {
                            Text(
                                if (settings.droidbarUseMostUsedApps) "6 ứng dụng dùng nhiều nhất (tự động)" else "Tùy chọn ứng dụng thủ công",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Surface(
                                modifier = Modifier.size(38.dp),
                                shape = M3Cookie6Shape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.primary
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = "Cấu hình",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.clickable { onOpenAppsConfig() }
                    )
                }
            }

            // OVERLAY PERMISSION WARNING IF NEEDED
            if (!PermissionHelper.isOverlayGranted(context)) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    ) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Cần quyền Hiển thị trên ứng dụng khác", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                            supportingContent = { Text("Cần thiết để hiển thị thanh công cụ cạnh màn hình trên các ứng dụng khác", style = MaterialTheme.typography.bodySmall) },
                            trailingContent = {
                                Button(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(
                                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    android.net.Uri.parse("package:${context.packageName}")
                                                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Cấp quyền", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        )
                    }
                }
            }

            // CONTROLS SECTION (NO DESCRIPTIONS)
            item {
                SectionHeader(title = "Điều khiển nhanh")
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SettingsCardItem(position = GroupPosition.TOP) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Đèn pin", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = M3Cookie6Shape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.FlashlightOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.droidbarShowFlashlight,
                                    onCheckedChange = { checked ->
                                        onUpdateSettings { it.copy(droidbarShowFlashlight = checked) }
                                    }
                                )
                            }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Chế độ chuông", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = M3Cookie6Shape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.droidbarShowRingMode,
                                    onCheckedChange = { checked ->
                                        onUpdateSettings { it.copy(droidbarShowRingMode = checked) }
                                    }
                                )
                            }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Chụp màn hình", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = M3Cookie6Shape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Screenshot,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.droidbarShowScreenshot,
                                    onCheckedChange = { checked ->
                                        onUpdateSettings { it.copy(droidbarShowScreenshot = checked) }
                                    }
                                )
                            }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Wi-Fi", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = M3Cookie6Shape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Wifi,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.droidbarShowWifi,
                                    onCheckedChange = { checked ->
                                        onUpdateSettings { it.copy(droidbarShowWifi = checked) }
                                    }
                                )
                            }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.BOTTOM) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Dữ liệu di động", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = M3Cookie6Shape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.SignalCellularAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.droidbarShowMobileData,
                                    onCheckedChange = { checked ->
                                        onUpdateSettings { it.copy(droidbarShowMobileData = checked) }
                                    }
                                )
                            }
                        )
                    }
                }
            }

            // APPEARANCE & FEEDBACK SECTION (NO DESCRIPTIONS)
            item {
                SectionHeader(title = "Giao diện & Phản hồi", icon = Icons.Default.Palette)
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    SettingsCardItem(position = GroupPosition.TOP) {
                        SliderSettingItem(
                            label = "Độ mờ nền",
                            value = (settings.droidbarBackgroundOpacity * 100f).roundToInt().toFloat(),
                            unit = "%",
                            valueRange = 0f..100f,
                            onValueChange = { pct ->
                                onUpdateSettings { it.copy(droidbarBackgroundOpacity = (pct / 100f).coerceIn(0f, 1f)) }
                            }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE) {
                        SliderSettingItem(
                            label = "Chiều cao tối đa",
                            value = settings.droidbarMaxHeightDp.toFloat(),
                            unit = "dp",
                            valueRange = 160f..600f,
                            onValueChange = { h ->
                                onUpdateSettings { it.copy(droidbarMaxHeightDp = h.roundToInt()) }
                            }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.MIDDLE) {
                        SliderSettingItem(
                            label = "Vị trí dọc (Y)",
                            value = settings.droidbarYOffsetDp.toFloat(),
                            unit = "dp",
                            valueRange = -250f..250f,
                            onValueChange = { y ->
                                onUpdateSettings { it.copy(droidbarYOffsetDp = y.roundToInt()) }
                            }
                        )
                    }

                    SettingsCardItem(position = GroupPosition.BOTTOM, isActive = settings.droidbarHapticsEnabled) {
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            headlineContent = { Text("Phản hồi xúc giác (Rung)", fontWeight = FontWeight.SemiBold) },
                            leadingContent = {
                                Surface(
                                    modifier = Modifier.size(38.dp),
                                    shape = M3Cookie6Shape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Vibration,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            },
                            trailingContent = {
                                Switch(
                                    checked = settings.droidbarHapticsEnabled,
                                    onCheckedChange = { checked ->
                                        onUpdateSettings { it.copy(droidbarHapticsEnabled = checked) }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroidbarAppsConfigPage(
    settings: IslandSettings,
    onUpdateSettings: ((IslandSettings) -> IslandSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var slotToEdit by remember { mutableIntStateOf(-1) }
    var showAppPicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Cấu hình ứng dụng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // TOGGLE: USE MOST USED APPS
            item {
                SettingsCardItem(position = GroupPosition.SINGLE) {
                    ListItem(
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        headlineContent = { Text("Dùng ứng dụng mở nhiều nhất", fontWeight = FontWeight.SemiBold) },
                        supportingContent = {
                            Text(
                                "Tự động hiển thị 6 ứng dụng bạn sử dụng thường xuyên nhất trên Droidbar",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = settings.droidbarUseMostUsedApps,
                                onCheckedChange = { checked ->
                                    onUpdateSettings { it.copy(droidbarUseMostUsedApps = checked) }
                                }
                            )
                        }
                    )
                }
            }

            // WHEN OFF: MANUALLY CHANGE EACH APP
            if (!settings.droidbarUseMostUsedApps) {
                item {
                    SectionHeader(title = "Vị trí ứng dụng (1 - 6)")
                }

                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val pm = context.packageManager
                        for (slotIndex in 0 until 6) {
                            val pkg = settings.droidbarCustomApps.getOrNull(slotIndex)
                            val appInfo = remember(pkg) {
                                if (!pkg.isNullOrBlank()) {
                                    runCatching {
                                        val ai = pm.getApplicationInfo(pkg, 0)
                                        BarAppInfo(
                                            packageName = pkg,
                                            label = pm.getApplicationLabel(ai).toString(),
                                            iconBitmap = MostUsedAppsHelper.drawableToBitmap(pm.getApplicationIcon(ai))
                                        )
                                    }.getOrNull()
                                } else null
                            }

                            val position = when (slotIndex) {
                                0 -> GroupPosition.TOP
                                5 -> GroupPosition.BOTTOM
                                else -> GroupPosition.MIDDLE
                            }

                            SettingsCardItem(position = position) {
                                ListItem(
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                    headlineContent = {
                                        Text(
                                            text = appInfo?.label ?: "Vị trí ${slotIndex + 1}: Chọn ứng dụng",
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = if (appInfo != null) "Vị trí ${slotIndex + 1} • Nhấn để đổi" else "Vị trí ${slotIndex + 1} (Trống)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    leadingContent = {
                                        Surface(
                                            modifier = Modifier.size(38.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                if (appInfo?.iconBitmap != null) {
                                                    Image(
                                                        bitmap = appInfo.iconBitmap.asImageBitmap(),
                                                        contentDescription = appInfo.label,
                                                        modifier = Modifier.size(28.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Thêm",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    trailingContent = {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                            contentDescription = "Chọn",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    modifier = Modifier.clickable {
                                        slotToEdit = slotIndex
                                        showAppPicker = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAppPicker && slotToEdit >= 0) {
        AppPickerDialog(
            onDismiss = {
                showAppPicker = false
                slotToEdit = -1
            },
            onSelectApp = { selectedApp ->
                val current = settings.droidbarCustomApps.toMutableList()
                while (current.size <= slotToEdit) {
                    current.add("")
                }
                current[slotToEdit] = selectedApp.packageName
                onUpdateSettings { it.copy(droidbarCustomApps = current) }
                showAppPicker = false
                slotToEdit = -1
            }
        )
    }
}

@Composable
fun AppPickerDialog(
    onDismiss: () -> Unit,
    onSelectApp: (BarAppInfo) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var allApps by remember { mutableStateOf<List<BarAppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val loaded = withContext(Dispatchers.IO) {
            MostUsedAppsHelper.getAllInstalledApps(context)
        }
        allApps = loaded
        isLoading = false
    }

    val filteredApps = remember(allApps, searchQuery) {
        if (searchQuery.isBlank()) allApps
        else allApps.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Chọn một ứng dụng", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 450.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm kiếm ứng dụng...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectApp(app) }
                                    .padding(vertical = 8.dp, horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (app.iconBitmap != null) {
                                    Image(
                                        bitmap = app.iconBitmap.asImageBitmap(),
                                        contentDescription = app.label,
                                        modifier = Modifier.size(36.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Apps,
                                        contentDescription = app.label,
                                        modifier = Modifier.size(36.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
