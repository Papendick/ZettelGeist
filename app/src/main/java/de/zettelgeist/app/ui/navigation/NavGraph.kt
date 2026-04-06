package de.zettelgeist.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.zettelgeist.app.ui.screens.*
import de.zettelgeist.app.ui.theme.Teal
import de.zettelgeist.app.ui.viewmodel.ChatViewModel
import de.zettelgeist.app.ui.viewmodel.NoteViewModel
import de.zettelgeist.app.ui.viewmodel.SearchViewModel
import de.zettelgeist.app.ui.viewmodel.SettingsViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Inbox : Screen("inbox", "Inbox", Icons.Default.Inbox)
    data object Zettel : Screen("zettel", "Zettel", Icons.Default.Lightbulb)
    data object Sources : Screen("sources", "Quellen", Icons.Default.MenuBook)
    data object Search : Screen("search", "Suche", Icons.Default.Search)
    data object Graph : Screen("graph", "Graph", Icons.Default.Hub)
}

val bottomNavScreens = listOf(
    Screen.Inbox,
    Screen.Zettel,
    Screen.Sources,
    Screen.Search,
    Screen.Graph
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZettelGeistNavGraph(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val navController = rememberNavController()
    val noteViewModel: NoteViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val chatViewModel: ChatViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavScreens.map { it.route }

    val inboxCount by noteViewModel.inboxCount.collectAsState()

    Scaffold(
        topBar = {
            if (showBottomBar) {
                TopAppBar(
                    title = {
                        Text(
                            "ZettelGeist",
                            style = MaterialTheme.typography.headlineMedium
                        )
                    },
                    actions = {
                        if (noteViewModel.isLlmAvailable) {
                            IconButton(onClick = { navController.navigate("chat") }) {
                                Icon(Icons.Default.Chat, contentDescription = "Chat")
                            }
                        }
                        IconButton(onClick = onToggleDarkMode) {
                            Icon(
                                if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Dark Mode"
                            )
                        }
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = {
                                if (screen == Screen.Inbox && inboxCount > 0) {
                                    BadgedBox(badge = { Badge { Text("$inboxCount") } }) {
                                        Icon(screen.icon, contentDescription = screen.label)
                                    }
                                } else {
                                    Icon(screen.icon, contentDescription = screen.label)
                                }
                            },
                            label = { Text(screen.label) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Teal,
                                selectedTextColor = Teal,
                                indicatorColor = Teal.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Inbox.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Inbox.route) {
                InboxScreen(
                    viewModel = noteViewModel,
                    onNoteClick = { navController.navigate("editor/$it/inbox") },
                    onNewNote = { navController.navigate("editor/new/inbox") }
                )
            }

            composable(Screen.Zettel.route) {
                ZettelScreen(
                    viewModel = noteViewModel,
                    onNoteClick = { navController.navigate("editor/$it/zettel") },
                    onNewNote = { navController.navigate("editor/new/zettel") }
                )
            }

            composable(Screen.Sources.route) {
                SourceScreen(
                    viewModel = noteViewModel,
                    onNoteClick = { navController.navigate("editor/$it/source") },
                    onNewNote = { navController.navigate("editor/new/source") }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    viewModel = searchViewModel,
                    onNoteClick = { navController.navigate("editor/$it/zettel") }
                )
            }

            composable(Screen.Graph.route) {
                GraphScreen(
                    viewModel = noteViewModel,
                    onNoteClick = { navController.navigate("editor/$it/zettel") }
                )
            }

            composable(
                route = "editor/{noteId}/{workspace}",
                arguments = listOf(
                    navArgument("noteId") { type = NavType.StringType },
                    navArgument("workspace") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId")
                val workspace = backStackEntry.arguments?.getString("workspace") ?: "inbox"
                EditorScreen(
                    viewModel = noteViewModel,
                    noteId = if (noteId == "new") null else noteId,
                    initialWorkspace = workspace,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("chat") {
                ChatScreen(
                    viewModel = chatViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
