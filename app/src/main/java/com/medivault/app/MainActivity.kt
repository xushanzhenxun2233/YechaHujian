package com.medivault.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.medivault.app.ui.screens.InsuranceScreen
import com.medivault.app.ui.screens.MedicationsScreen
import com.medivault.app.ui.screens.RecordsScreen
import com.medivault.app.ui.screens.SettingsBackupScreen
import com.medivault.app.ui.theme.MediVaultTheme
import com.medivault.app.ui.viewmodel.MediVaultViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MediVaultApp
        setContent {
            MediVaultTheme {
                val vm: MediVaultViewModel = viewModel(
                    factory = MediVaultViewModel.Factory(app.repository, app.locationResolver)
                )
                MediVaultRoot(viewModel = vm)
            }
        }
    }
}

private data class NavTab(
    val label: String,
    val icon: ImageVector
)

@Composable
private fun MediVaultRoot(viewModel: MediVaultViewModel) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val tabs = remember {
        listOf(
            NavTab("病例相册", Icons.Default.PhotoLibrary),
            NavTab("开药打卡", Icons.Default.Medication),
            NavTab("医保计算", Icons.Default.Calculate),
            NavTab("设备迁移", Icons.Default.FolderZip)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (selectedTab) {
                0 -> RecordsScreen(viewModel)
                1 -> MedicationsScreen(viewModel)
                2 -> InsuranceScreen(viewModel)
                3 -> SettingsBackupScreen(viewModel)
            }
        }
    }
}
