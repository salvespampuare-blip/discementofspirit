package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SpiritualLog
import com.example.viewmodel.SpiritualViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DashboardScreen(
    viewModel: SpiritualViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Observe flows from ViewModel
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val activePersona by viewModel.activePersona.collectAsStateWithLifecycle()
    val subscriptionStatus by viewModel.subscriptionStatus.collectAsStateWithLifecycle()
    val stripeMessage by viewModel.stripeMessage.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()
    val alertThresholdDays by viewModel.alertThresholdDays.collectAsStateWithLifecycle()
    val notifyOnSecretHiding by viewModel.notifyOnSecretHiding.collectAsStateWithLifecycle()
    val alerts by viewModel.spiritualSecurityAlerts.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("LOGS") } // Seeker: "LOGS" vs "ALERTS" | Director: "ANALYTICS" vs "SYNC" vs "STRIPE"

    // Stripe checkout inputs
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    // CRM synchronization inputs
    var apiEndpoint by remember { mutableStateOf("https://crm.diocese.org/api/v1/spiritual-logs") }

    // Toast feedback for Stripe
    LaunchedEffect(stripeMessage) {
        stripeMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStripeMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                // Header with Premium Brand
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "REVERENT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Spiritual Discernment",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.5).sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Badge for active premium subscription tier
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (subscriptionStatus == "PREMIUM") {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (subscriptionStatus == "PREMIUM") Icons.Default.Star else Icons.Default.Lock,
                                        contentDescription = "Subscription Level",
                                        tint = if (subscriptionStatus == "PREMIUM") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (subscriptionStatus == "PREMIUM") "PREMIUM ADMIN" else "FREE SEEKER",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (subscriptionStatus == "PREMIUM") MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ROLE-BASED ACCESS SEGMENTED CONTROL (Persona Customization)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (activePersona == "EXERCITANT") MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable {
                                        viewModel.setPersona("EXERCITANT")
                                        activeTab = "LOGS"
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Exercitant Profile",
                                    tint = if (activePersona == "EXERCITANT") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Directee View",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activePersona == "EXERCITANT") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (activePersona == "DIRECTOR") MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable {
                                        viewModel.setPersona("DIRECTOR")
                                        activeTab = "ANALYTICS"
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Spiritual Director Profile",
                                    tint = if (activePersona == "DIRECTOR") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Director Dashboard",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activePersona == "DIRECTOR") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // ALERTS BANNER FOR ANOMALIES (Rule 12, 13, 14 warnings)
            if (alerts.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Security Alert",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SPIRITUAL SECURITY THREAT DETECTED",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        alerts.forEachIndexed { index, alert ->
                            Text(
                                text = "• $alert",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        TextButton(
                            onClick = { viewModel.clearAlerts() },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Acknowledge Warnings", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Tab bar dependent on active Persona selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activePersona == "EXERCITANT") {
                    // SEEKER TABS
                    AssistChip(
                        onClick = { activeTab = "LOGS" },
                        label = { Text("Daily Journal Entries") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = "Daily Log", modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (activeTab == "LOGS") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    )
                    AssistChip(
                        onClick = { activeTab = "ALERTS" },
                        label = { Text("Custom Security Alerts") },
                        leadingIcon = { Icon(Icons.Default.Warning, contentDescription = "Alert Rules", modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (activeTab == "ALERTS") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    )
                } else {
                    // DIRECTOR TABS
                    AssistChip(
                        onClick = { activeTab = "ANALYTICS" },
                        label = { Text("Statistical Analytics") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = "Analytics", modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (activeTab == "ANALYTICS") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    )
                    AssistChip(
                        onClick = { activeTab = "SYNC" },
                        label = { Text("Parish REST API") },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (activeTab == "SYNC") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    )
                    AssistChip(
                        onClick = { activeTab = "STRIPE" },
                        label = { Text("Stripe Premium Plan") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Billing", modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (activeTab == "STRIPE") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // MAIN CONTENT BODY SWITCHER WITH TRANSITIONS
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (activePersona == "EXERCITANT") {
                    when (activeTab) {
                        "LOGS" -> SeekerLogsView(
                            logs = logs,
                            isAnalyzing = isAnalyzing,
                            onAddLog = { title, content, intensity, state ->
                                viewModel.addLogEntry(title, content, intensity, state)
                            },
                            onDeleteLog = { id -> viewModel.deleteLog(id) }
                        )
                        "ALERTS" -> CustomAlertsView(
                            thresholdDays = alertThresholdDays,
                            notifyOnSecret = notifyOnSecretHiding,
                            onThresholdChange = { viewModel.updateThresholdDays(it) },
                            onNotifyChange = { viewModel.setNotifyOnSecretHiding(it) }
                        )
                    }
                } else {
                    // DIRECTOR / SUPERVISOR VIEWS
                    when (activeTab) {
                        "ANALYTICS" -> AdvisorAnalyticsView(
                            logs = logs,
                            viewModel = viewModel,
                            subscriptionStatus = subscriptionStatus,
                            clipboardManager = clipboardManager,
                            context = context
                        )
                        "SYNC" -> ParishApiSyncView(
                            syncState = syncState,
                            syncLogs = syncLogs,
                            endpoint = apiEndpoint,
                            onEndpointChange = { apiEndpoint = it },
                            onTriggerSync = { viewModel.triggerCrmSync(apiEndpoint) }
                        )
                        "STRIPE" -> StripeBillingView(
                            status = subscriptionStatus,
                            cardNumber = cardNumber,
                            expiry = expiry,
                            cvv = cvv,
                            onCardChange = { cardNumber = it },
                            onExpiryChange = { expiry = it },
                            onCvvChange = { cvv = it },
                            onPay = { viewModel.processStripePayment(cardNumber, expiry, cvv) },
                            onCancel = { viewModel.cancelSubscription() }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. SEEKER LOGS VIEW (Directee space)
// ==========================================
@Composable
fun SeekerLogsView(
    logs: List<SpiritualLog>,
    isAnalyzing: Boolean,
    onAddLog: (String, String, Int, String) -> Unit,
    onDeleteLog: (Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var stateEstimate by remember { mutableStateOf("NEUTRAL") } // "CONSOLATION", "DESOLATION", "NEUTRAL"
    var intensity by remember { mutableStateOf(5) }

    var isAddingNew by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Examen Diary",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ignatian rules recommend reviewing daily interior movements to identify what spirit is moving you. Keep your logs detailed to find pattern anomalies.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!isAddingNew) {
                        Button(
                            onClick = { isAddingNew = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add log")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Record Today's Seeker Log")
                        }
                    }
                }
            }
        }

        if (isAddingNew) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "New Examen entry",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { isAddingNew = false }) {
                                Icon(Icons.Default.Delete, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Log Title (e.g. Morning Prayer distraction)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text("What inner thoughts, feelings, or distractions did you experience?") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            maxLines = 5
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Estimated State Selection (Consolation vs Desolation)
                        Text(
                            text = "Estimated Spirit State:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("CONSOLATION", "NEUTRAL", "DESOLATION").forEach { mood ->
                                val isSelected = stateEstimate == mood
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) {
                                                when (mood) {
                                                    "CONSOLATION" -> Color(0xFF2E7D32)
                                                    "DESOLATION" -> Color(0xFFC62828)
                                                    else -> MaterialTheme.colorScheme.outline
                                                }
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            }
                                        )
                                        .clickable { stateEstimate = mood }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mood,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Intensity level
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Interior Intensity: $intensity/10",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Slider(
                            value = intensity.toFloat(),
                            onValueChange = { intensity = it.toInt() },
                            valueRange = 1f..10f,
                            steps = 8,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (isAnalyzing) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Ignatian ML Engine dissecting movements...")
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (title.isNotBlank() && content.isNotBlank()) {
                                        onAddLog(title, content, intensity, stateEstimate)
                                        // Reset inputs
                                        title = ""
                                        content = ""
                                        stateEstimate = "NEUTRAL"
                                        intensity = 5
                                        isAddingNew = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Process Log with AI & Save")
                            }
                        }
                    }
                }
            }
        }

        // Logs Lists Header
        item {
            Text(
                text = "Log History",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        if (logs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Empty",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No recorded logs yet.\nYour Ignatian diary is empty.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        items(logs) { log ->
            var expanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = SimpleDateFormat("MMM dd, yyyy - HH:mm").format(Date(log.timestamp)),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (log.state) {
                                    "CONSOLATION" -> Color(0xFFE8F5E9)
                                    "DESOLATION" -> Color(0xFFFFEBEE)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                            ) {
                                Text(
                                    text = log.state,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (log.state) {
                                        "CONSOLATION" -> Color(0xFF2E7D32)
                                        "DESOLATION" -> Color(0xFFC62828)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${log.intensity}/10",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Expand details
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "DIARY RECORD:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = log.content,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            log.ruleApplied?.let { rule ->
                                Text(
                                    text = "IGNATIAN ANALYSIS CLASSIFICATION:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = rule,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            log.analysis?.let { insight ->
                                Text(
                                    text = "AI INSIGHT & ACTIONABLE LESSON:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = insight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { onDeleteLog(log.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    modifier = Modifier.height(36.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Discard", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. CUSTOM ALERTS VIEW
// ==========================================
@Composable
fun CustomAlertsView(
    thresholdDays: Int,
    notifyOnSecret: Boolean,
    onThresholdChange: (Int) -> Unit,
    onNotifyChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Alert Customization Console",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Configure parameters to detect vulnerabilities. Ignatian military tactics (Rule 14) teach that structural weaknesses must be guarded from assault.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Prolonged Desolation Slider
                Text(
                    text = "Detect Prolonged Desolation Threshold",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Consecutive logs of spiritual desolations required to trigger a high-severity warning.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = thresholdDays.toFloat(),
                        onValueChange = { onThresholdChange(it.toInt()) },
                        valueRange = 1f..7f,
                        steps = 5,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$thresholdDays days",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                // Secrecy checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Monitor Rule 13 Secrecy Behaviors",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Examines log logs for keywords matching hiding or secrecy, simulating standard defense scans.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Switch(
                        checked = notifyOnSecret,
                        onCheckedChange = { onNotifyChange(it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Warning information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Alert advice",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Defensive Strategy: In times of intense desolation, do not execute architectural updates or new pipelines. Maintain constant prayer habits instead (Rule 5 & 6).",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

// ==========================================
// 3. ADVISOR ANALYTICS VIEW
// ==========================================
@Composable
fun AdvisorAnalyticsView(
    logs: List<SpiritualLog>,
    viewModel: SpiritualViewModel,
    subscriptionStatus: String,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    context: android.content.Context
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Real-Time Spiritual Analytics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Identify trends and patterns of the enemy's assault routes and moments of grace over history.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }

        if (logs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Waiting for logs to compile metrics.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            // Stats card
            item {
                val total = logs.size
                val consolationCount = logs.count { it.state == "CONSOLATION" }
                val desolationCount = logs.count { it.state == "DESOLATION" }
                val neutralCount = logs.count { it.state == "NEUTRAL" }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Consolations", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Text("$consolationCount", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
                            Text("${(consolationCount * 100 / total)}%", fontSize = 10.sp, color = Color(0xFF2E7D32))
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Desolations", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                            Text("$desolationCount", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFB71C1C))
                            Text("${(desolationCount * 100 / total)}%", fontSize = 10.sp, color = Color(0xFFC62828))
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Neutral Logs", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$neutralCount", fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${(neutralCount * 100 / total)}%", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            // Glowing Analytics Chart (Canvas Drawing for Visual Polish)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "State Fluctuations Timeline (Recent Logs)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val recentLogs = logs.take(6).reversed()
                        Canvas(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            val width = size.width
                            val height = size.height
                            val spacing = width / 6f

                            // Draw grid lines
                            drawLine(Color.LightGray.copy(alpha = 0.5f), Offset(0f, 0f), Offset(width, 0f), strokeWidth = 1f)
                            drawLine(Color.LightGray.copy(alpha = 0.5f), Offset(0f, height / 2f), Offset(width, height / 2f), strokeWidth = 1f)
                            drawLine(Color.LightGray.copy(alpha = 0.5f), Offset(0f, height), Offset(width, height), strokeWidth = 1f)

                            val points = recentLogs.mapIndexed { idx, log ->
                                val x = idx * spacing + (spacing / 2f)
                                val scaleFactor = log.intensity / 10f
                                val y = when (log.state) {
                                    "CONSOLATION" -> height / 2f - (height / 2f * scaleFactor)
                                    "DESOLATION" -> height / 2f + (height / 2f * scaleFactor)
                                    else -> height / 2f
                                }
                                Offset(x, y)
                            }

                            // Draw lines between points
                            val linePath = Path()
                            if (points.isNotEmpty()) {
                                linePath.moveTo(points.first().x, points.first().y)
                                for (i in 1 until points.size) {
                                    linePath.lineTo(points[i].x, points[i].y)
                                }
                                drawPath(
                                    path = linePath,
                                    color = Color(33, 150, 243),
                                    style = Stroke(width = 6f)
                                )
                            }

                            // Draw point circles with custom colors matching spiritual state
                            recentLogs.forEachIndexed { idx, log ->
                                if (idx < points.size) {
                                    val pointColor = when (log.state) {
                                        "CONSOLATION" -> Color(0xFF2E7D32)
                                        "DESOLATION" -> Color(0xFFC62828)
                                        else -> Color.Gray
                                    }
                                    drawCircle(
                                        color = pointColor,
                                        radius = 12f,
                                        center = points[idx]
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 6f,
                                        center = points[idx]
                                    )
                                }
                            }
                        }

                        // Labels
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Consolation (Fervor ↑)", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            Text("Neutral State", fontSize = 10.sp, color = Color.Gray)
                            Text("Desolation (Trial ↓)", fontSize = 10.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Trigger anomalies analysis
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Detected Trigger Patterns",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val rulesUsed = logs.mapNotNull { it.ruleApplied }.groupingBy { it }.eachCount()
                        if (rulesUsed.isEmpty()) {
                            Text("Insufficient classification records to compute patterns.", fontSize = 12.sp, color = Color.Gray)
                        } else {
                            rulesUsed.forEach { (rule, count) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = rule, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "$count occurrences",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // REPORT EXPORT CENTER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Spiritual Audit Export Center",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Export comprehensive reports for pipeline inputs, bishop/directors audits, and historic files preservation.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val reportStr = viewModel.generateExportableReport()
                                clipboardManager.setText(AnnotatedString(reportStr))
                                Toast.makeText(context, "Full Soul Report Copied to Clipboard!", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate & Copy Exportable Report")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. PARISH API SYNC VIEW (CRM)
// ==========================================
@Composable
fun ParishApiSyncView(
    syncState: String,
    syncLogs: List<String>,
    endpoint: String,
    onEndpointChange: (String) -> Unit,
    onTriggerSync: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Parish REST CRM Sync Integration",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Configure outbound REST requests to synchronize spiritual logs securely with Church database networks.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = endpoint,
            onValueChange = onEndpointChange,
            label = { Text("Sacramental REST End-point") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (syncState == "SYNCING") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Syncing with ecclesiastical server...", fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onTriggerSync,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Sync")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Establish Hook & Synchronize Data")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Terminal Output
        Text(
            text = "API REST INTERACTION LOGGER",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline
        )

        Spacer(modifier = Modifier.height(6.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            color = Color.Black,
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                if (syncLogs.isEmpty()) {
                    item {
                        Text(
                            text = "[IDLE] Console awaiting sync initiation...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFF00FF00)
                        )
                    }
                } else {
                    items(syncLogs) { line ->
                        Text(
                            text = ">> $line",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFF00FF00),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. STRIPE BILLING VIEW
// ==========================================
@Composable
fun StripeBillingView(
    status: String,
    cardNumber: String,
    expiry: String,
    cvv: String,
    onCardChange: (String) -> Unit,
    onExpiryChange: (String) -> Unit,
    onCvvChange: (String) -> Unit,
    onPay: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Secured Stripe Billing Hub",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Manage subscription credentials securely. Processing is compliant under modern Stripe security guidelines.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (status == "PREMIUM") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, contentDescription = "Active", tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PREMIUM PLANNED ACTIVE",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color(0xFF1B5E20)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Your account is linked with Premium Spiritual Mentor status. Advanced AI analytics, trigger anomaly alerts, and full reports export are active.",
                        fontSize = 13.sp,
                        color = Color(0xFF2E7D32)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Downgrade Plan", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Premium Spiritual Mentor Tier",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• Advanced Ignatian 14-Rules AI Diagnosis\n• Customizable Spiritual Secrecy Alerts\n• Exportable XML/JSON summaries for Spiritual Directors",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = "${'$'}9.99 / monthly",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated Stripe Card Element Form
                    Text(
                        text = "STRIPE SECURE CARD ELEMENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = onCardChange,
                        label = { Text("Credit Card Number (16-digits)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = expiry,
                            onValueChange = onExpiryChange,
                            label = { Text("MM/YY") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        OutlinedTextField(
                            value = cvv,
                            onValueChange = onCvvChange,
                            label = { Text("CVC") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onPay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Purchase")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Process Payment on Stripe Gateway")
                    }
                }
            }
        }
    }
}
