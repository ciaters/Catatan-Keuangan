package com.example.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.state.FinanceViewModel
import com.example.ui.navigation.AppDestination
import com.example.ui.screens.BerandaScreen
import com.example.ui.screens.IncomeScreen
import com.example.ui.screens.PengaturanScreen
import com.example.ui.screens.PosisiSaldoScreen
import com.example.ui.screens.RingkasanKeuanganScreen
import com.example.ui.screens.StatistikScreen
import com.example.ui.screens.TagihanKewajibanScreen
import com.example.ui.screens.TransaksiScreen
import com.example.ui.theme.CanvasBackground
import com.example.ui.theme.CardSurface
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.InkMuted
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.OnEmeraldContainer
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SurfaceTinted
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatatanKeuanganApp(
  financeViewModel: FinanceViewModel = viewModel(),
) {
  val uiState by financeViewModel.uiState.collectAsStateWithLifecycle()
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        financeViewModel.refreshTodayDate()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  val currentDestination = AppDestination.fromRoute(currentRoute)

  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()

  var showMoreSheet by remember { mutableStateOf(false) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  fun navigateTo(destination: AppDestination) {
    if (destination.route == currentRoute) return
    navController.navigate(destination.route) {
      popUpTo(navController.graph.findStartDestination().id) {
        saveState = true
      }
      launchSingleTop = true
      restoreState = true
    }
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet(
        drawerContainerColor = CardSurface,
        modifier = Modifier.width(304.dp),
      ) {
        Column(
          modifier =
            Modifier
              .fillMaxHeight()
              .verticalScroll(rememberScrollState())
              .padding(vertical = 16.dp)
        ) {
          Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
              text = stringResource(R.string.app_name),
              style = MaterialTheme.typography.headlineSmall,
              color = EmeraldPrimary,
              fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = stringResource(R.string.drawer_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = InkMuted,
            )
          }
          HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
          )

          AppDestination.entries.forEachIndexed { index, dest ->
            val isSelected = currentDestination == dest
            NavigationDrawerItem(
              label = {
                Text(
                  text = "${index + 1}. ${dest.label}",
                  style = MaterialTheme.typography.labelLarge,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              selected = isSelected,
              onClick = {
                scope.launch { drawerState.close() }
                navigateTo(dest)
              },
              icon = {
                Icon(
                  imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                  contentDescription = null,
                )
              },
              modifier =
                Modifier
                  .padding(horizontal = 12.dp, vertical = 2.dp)
                  .testTag("drawer_item_${dest.route}"),
              colors =
                NavigationDrawerItemDefaults.colors(
                  selectedContainerColor = EmeraldContainer,
                  selectedIconColor = EmeraldPrimary,
                  selectedTextColor = OnEmeraldContainer,
                  unselectedIconColor = InkSecondary,
                  unselectedTextColor = InkPrimary,
                ),
            )
          }
        }
      }
    },
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(CanvasBackground)) {
      val isWideScreen = maxWidth >= 600.dp

      Row(modifier = Modifier.fillMaxSize()) {
        if (isWideScreen) {
          NavigationRail(
            containerColor = CardSurface,
            header = {
              IconButton(
                onClick = { scope.launch { drawerState.open() } },
                modifier = Modifier.testTag("rail_menu_button"),
              ) {
                Icon(
                  imageVector = Icons.Outlined.Menu,
                  contentDescription = stringResource(R.string.nav_buka_menu),
                  tint = InkPrimary,
                )
              }
            },
          ) {
            AppDestination.entries.forEach { dest ->
              val selected = currentDestination == dest
              NavigationRailItem(
                selected = selected,
                onClick = { navigateTo(dest) },
                icon = {
                  Icon(
                    imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                    contentDescription = dest.label,
                  )
                },
                label = {
                  Text(
                    text = dest.shortLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                },
                modifier = Modifier.testTag("rail_item_${dest.route}"),
                colors =
                  NavigationRailItemDefaults.colors(
                    selectedIconColor = EmeraldPrimary,
                    selectedTextColor = EmeraldPrimary,
                    indicatorColor = EmeraldContainer,
                    unselectedIconColor = InkMuted,
                    unselectedTextColor = InkMuted,
                  ),
              )
            }
          }
        }

        Scaffold(
          modifier = Modifier.weight(1f).fillMaxHeight(),
          containerColor = CanvasBackground,
          contentWindowInsets = WindowInsets(0, 0, 0, 0),
          topBar = {
            TopAppBar(
              title = {
                Column {
                  Text(
                    text = stringResource(currentDestination.titleRes),
                    style = MaterialTheme.typography.titleLarge,
                    color = InkPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = currentDestination.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              },
              navigationIcon = {
                if (currentDestination.isPrimaryBottomTab) {
                  IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier.testTag("topbar_menu_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Menu,
                      contentDescription = stringResource(R.string.nav_buka_menu),
                      tint = InkPrimary,
                    )
                  }
                } else {
                  IconButton(
                    onClick = {
                      val popped = navController.popBackStack()
                      if (!popped) navigateTo(AppDestination.BERANDA)
                    },
                    modifier = Modifier.testTag("topbar_back_button"),
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                      contentDescription = stringResource(R.string.nav_kembali),
                      tint = InkPrimary,
                    )
                  }
                }
              },
              actions = {
                if (!currentDestination.isPrimaryBottomTab) {
                  IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier.testTag("topbar_drawer_action"),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Menu,
                      contentDescription = stringResource(R.string.nav_buka_menu),
                      tint = InkPrimary,
                    )
                  }
                } else {
                  IconButton(
                    onClick = { navigateTo(AppDestination.PENGATURAN) },
                    modifier = Modifier.testTag("topbar_settings_button"),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Settings,
                      contentDescription = stringResource(R.string.page_pengaturan),
                      tint = InkPrimary,
                    )
                  }
                }
              },
              colors =
                TopAppBarDefaults.topAppBarColors(
                  containerColor = CanvasBackground,
                  titleContentColor = InkPrimary,
                ),
            )
          },
          bottomBar = {
            if (!isWideScreen) {
              Surface(
                color = CardSurface,
                tonalElevation = 0.dp,
              ) {
                Column {
                  HorizontalDivider(
                    thickness = 0.75.dp,
                    color = OutlineSubtle.copy(alpha = 0.65f),
                  )
                  NavigationBar(
                    containerColor = CardSurface,
                    tonalElevation = 0.dp,
                  ) {
                  AppDestination.primaryTabs.forEach { dest ->
                    val selected = currentDestination == dest
                    NavigationBarItem(
                      selected = selected,
                      onClick = { navigateTo(dest) },
                      icon = {
                        Icon(
                          imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                          contentDescription = dest.label,
                        )
                      },
                      label = {
                        Text(
                          text = dest.shortLabel,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      },
                      modifier = Modifier.testTag("bottom_nav_${dest.route}"),
                      colors =
                        NavigationBarItemDefaults.colors(
                          selectedIconColor = EmeraldPrimary,
                          selectedTextColor = EmeraldPrimary,
                          indicatorColor = EmeraldContainer,
                          unselectedIconColor = InkMuted,
                          unselectedTextColor = InkMuted,
                        ),
                    )
                  }

                  val isSecondaryActive = !currentDestination.isPrimaryBottomTab
                  NavigationBarItem(
                    selected = isSecondaryActive,
                    onClick = { showMoreSheet = true },
                    icon = {
                      Icon(
                        imageVector =
                          if (isSecondaryActive) Icons.Filled.GridView else Icons.Outlined.GridView,
                        contentDescription = stringResource(R.string.nav_menu_lain),
                      )
                    },
                    label = {
                      Text(
                        text = stringResource(R.string.nav_menu_lain),
                        maxLines = 1,
                      )
                    },
                    modifier = Modifier.testTag("bottom_nav_lainnya"),
                    colors =
                      NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = EmeraldContainer,
                        unselectedIconColor = InkMuted,
                        unselectedTextColor = InkMuted,
                      ),
                  )
                  }
                }
              }
            }
          },
        ) { innerPadding ->
          if (!uiState.isDatabaseLoaded) {
            Box(
              modifier = Modifier.fillMaxSize().padding(innerPadding),
              contentAlignment = Alignment.Center,
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                CircularProgressIndicator(color = EmeraldPrimary)
                Text(
                  text = "Memuat database lokal...",
                  style = MaterialTheme.typography.bodySmall,
                  color = InkSecondary,
                )
              }
            }
          } else {
          NavHost(
            navController = navController,
            startDestination = AppDestination.BERANDA.route,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(180)) },
            exitTransition = { fadeOut(animationSpec = tween(140)) },
          ) {
            composable(AppDestination.BERANDA.route) {
              BerandaScreen(
                uiState = uiState,
                onToggleHideAmounts = financeViewModel::toggleHideAmounts,
                onNavigate = ::navigateTo,
              )
            }
            composable(AppDestination.INCOME.route) {
              IncomeScreen(
                uiState = uiState,
                viewModel = financeViewModel,
                onNavigateToRingkasan = { navigateTo(AppDestination.RINGKASAN_KEUANGAN) },
              )
            }
            composable(AppDestination.TRANSAKSI.route) {
              TransaksiScreen(
                uiState = uiState,
                viewModel = financeViewModel,
                onNavigateToStatistik = { navigateTo(AppDestination.STATISTIK) },
              )
            }
            composable(AppDestination.STATISTIK.route) {
              StatistikScreen(
                uiState = uiState,
                onNavigateToRingkasan = { navigateTo(AppDestination.RINGKASAN_KEUANGAN) },
              )
            }
            composable(AppDestination.POSISI_SALDO.route) {
              PosisiSaldoScreen(
                uiState = uiState,
                viewModel = financeViewModel,
                onNavigateToRingkasan = { navigateTo(AppDestination.RINGKASAN_KEUANGAN) },
              )
            }
            composable(AppDestination.RINGKASAN_KEUANGAN.route) {
              RingkasanKeuanganScreen(
                uiState = uiState,
                onNavigate = ::navigateTo,
              )
            }
            composable(AppDestination.TAGIHAN_KEWAJIBAN.route) {
              TagihanKewajibanScreen(
                uiState = uiState,
                viewModel = financeViewModel,
              )
            }
            composable(AppDestination.PENGATURAN.route) {
              PengaturanScreen(
                uiState = uiState,
                viewModel = financeViewModel,
              )
            }
          }
          }
        }
      }
    }
  }

  if (showMoreSheet) {
    ModalBottomSheet(
      onDismissRequest = { showMoreSheet = false },
      sheetState = sheetState,
      containerColor = CardSurface,
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = stringResource(R.string.sheet_title_lainnya),
          style = MaterialTheme.typography.titleMedium,
          color = InkPrimary,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 4.dp),
        )
        Text(
          text = "Pilih halaman yang ingin dibuka",
          style = MaterialTheme.typography.bodySmall,
          color = InkMuted,
          modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))

        AppDestination.secondaryPages.forEach { dest ->
          val isSelected = currentDestination == dest
          Surface(
            modifier =
              Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable {
                  showMoreSheet = false
                  navigateTo(dest)
                }
                .testTag("sheet_item_${dest.route}"),
            shape = MaterialTheme.shapes.medium,
            color = if (isSelected) EmeraldContainer.copy(alpha = 0.6f) else SurfaceTinted,
            border =
              if (isSelected) BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f))
              else null,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Box(
                modifier =
                  Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) EmeraldPrimary else EmeraldContainer),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = dest.selectedIcon,
                  contentDescription = null,
                  tint = if (isSelected) Color.White else EmeraldPrimary,
                  modifier = Modifier.size(20.dp),
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = dest.label,
                  style = MaterialTheme.typography.titleSmall,
                  color = InkPrimary,
                  fontWeight = FontWeight.SemiBold,
                )
                Text(
                  text = dest.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = InkMuted,
                )
              }
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = InkMuted,
              )
            }
          }
        }
      }
    }
  }
}
