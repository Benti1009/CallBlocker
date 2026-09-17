package com.example.callblocker

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.callblocker.ui.theme.CallBlockerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ----------------------
// Affiche un message avec Snackbar
// ----------------------
fun showAppMessage(scope: CoroutineScope, snackbarHostState: SnackbarHostState, message: String) {
    scope.launch {
        snackbarHostState.showSnackbar(
            message = message,
            duration = SnackbarDuration.Short
        )
    }
}

// ----------------------
// Vérifie si l'app est active comme application de filtrage
// ----------------------
fun isCallScreeningAppActive(context: Context): Boolean {
    val roleManager = context.getSystemService(RoleManager::class.java)
    return roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
}

// ----------------------
// MainActivity
// ----------------------
class MainActivity : ComponentActivity() {

    // Launcher de permissions déclaré au niveau de l'activité
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* géré via Snackbar */ }

    // Launcher pour la demande du rôle "application de filtrage d'appels"
    private val requestRoleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { /* résultat vérifiable via le bouton "Vérifier" */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CallBlockerTheme {

                val context = LocalContext.current
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                // État de l'écran courant
                var currentScreen by remember { mutableStateOf("main") }

                // Liste observable pour la blacklist (numéros déjà normalisés)
                val blacklist = remember { mutableStateListOf<String>() }

                // Charger la blacklist en toute sécurité
                LaunchedEffect(Unit) {
                    try {
                        blacklist.addAll(loadBlacklist(this@MainActivity))
                    } catch (e: Exception) {
                        showAppMessage(scope, snackbarHostState, "Erreur chargement blacklist")
                    }
                }

                // Intercepte le bouton Retour physique
                BackHandler {
                    if (currentScreen != "main") {
                        currentScreen = "main"
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { paddingValues ->

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .padding(paddingValues),
                        verticalArrangement = Arrangement.Top,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (currentScreen == "main") {
                            MainScreen(
                                onRequestPermissions = {
                                    requestPermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                                    requestPermissionLauncher.launch(Manifest.permission.ANSWER_PHONE_CALLS)
                                    showAppMessage(scope, snackbarHostState, "Demande de permissions lancée")
                                },
                                onOpenCallScreeningSettings = {
                                    val roleManager = context.getSystemService(RoleManager::class.java)
                                    if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
                                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                                        requestRoleLauncher.launch(intent)
                                    } else {
                                        showAppMessage(
                                            scope,
                                            snackbarHostState,
                                            "Rôle de filtrage d'appels indisponible sur cet appareil"
                                        )
                                    }
                                },
                                onCheckCallScreeningApp = {
                                    val isActive = isCallScreeningAppActive(context)
                                    showAppMessage(
                                        scope,
                                        snackbarHostState,
                                        if (isActive) "App active ✅" else "App inactive ❌"
                                    )
                                },
                                onNavigateBlacklist = { currentScreen = "blacklist" }
                            )
                        } else if (currentScreen == "blacklist") {
                            BlacklistScreen(
                                blacklist = blacklist,
                                onAddNumber = { rawNumber ->
                                    val normalized = addToBlacklist(this@MainActivity, rawNumber)
                                    if (normalized != null) {
                                        if (!blacklist.contains(normalized)) blacklist.add(normalized)
                                        showAppMessage(scope, snackbarHostState, "Numéro ajouté ✅")
                                    } else {
                                        showAppMessage(scope, snackbarHostState, "Numéro invalide")
                                    }
                                },
                                onRemoveNumber = {
                                    removeFromBlacklist(this@MainActivity, it)
                                    blacklist.remove(it)
                                    showAppMessage(scope, snackbarHostState, "Numéro supprimé ✅")
                                },
                                onBack = { currentScreen = "main" }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ----------------------
// Composable MainScreen
// ----------------------
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onRequestPermissions: () -> Unit,
    onOpenCallScreeningSettings: () -> Unit,
    onCheckCallScreeningApp: () -> Unit,
    onNavigateBlacklist: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Cette application bloque les appels de démarchage (préfixes ARCEP).\n\n" +
                    "1️⃣ Étape 1 : Autorisez les permissions Téléphone\n" +
                    "2️⃣ Étape 2 : Activez CallBlocker comme application de filtrage d’appels\n" +
                    "3️⃣ Étape 3 : Vérifiez si votre app est bien active",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth()) {
            Text("Demander les permissions")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onOpenCallScreeningSettings, modifier = Modifier.fillMaxWidth()) {
            Text("Configurer l'app de filtrage d'appels")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onCheckCallScreeningApp, modifier = Modifier.fillMaxWidth()) {
            Text("Vérifier app de filtrage")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onNavigateBlacklist, modifier = Modifier.fillMaxWidth()) {
            Text("Gérer la blacklist")
        }
    }
}

// ----------------------
// Composable BlacklistScreen
// ----------------------
@Composable
fun BlacklistScreen(
    modifier: Modifier = Modifier,
    blacklist: List<String>,
    onAddNumber: (String) -> Unit,
    onRemoveNumber: (String) -> Unit,
    onBack: () -> Unit
) {
    var newNumber by remember { mutableStateOf("") }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Blacklist", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        blacklist.forEach { number ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(number)
                Button(onClick = { onRemoveNumber(number) }) {
                    Text("Supprimer")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = newNumber,
                onValueChange = { newNumber = it },
                placeholder = { Text("Numéro à ajouter") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                if (newNumber.isNotBlank()) {
                    onAddNumber(newNumber)
                    newNumber = ""
                }
            }) {
                Text("Ajouter")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Retour")
        }
    }
}
