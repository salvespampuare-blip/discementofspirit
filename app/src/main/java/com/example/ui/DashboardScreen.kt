package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Send
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
                        label = { Text("Daily Journal") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = "Daily Log", modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (activeTab == "LOGS") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    )
                    AssistChip(
                        onClick = { activeTab = "GUIDE" },
                        label = { Text("Discernment Guide") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = "Discernment Guide", modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (activeTab == "GUIDE") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                        )
                    )
                    AssistChip(
                        onClick = { activeTab = "ALERTS" },
                        label = { Text("Security Alerts") },
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
                        "GUIDE" -> DiscernmentGuideView()
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
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Sync")
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

// ==========================================
// 6. DISCERNMENT GUIDE & INSTRUCTIONS VIEW
// ==========================================
@Composable
fun DiscernmentGuideView() {
    var guideTab by remember { mutableStateOf("STEPS") } // "STEPS", "RULES", "STORIES"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Spiritual Guide Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Interior Compass Manual",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Learn Ignatian rules and structured steps to make inspired, balanced choices under the Holy Spirit.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        item {
            // Guide sub tab selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(4.dp)
            ) {
                listOf(
                    "STEPS" to "4 Steps",
                    "RULES" to "14 Ignatian Rules",
                    "STORIES" to "Saints' Wisdom"
                ).forEach { (tabId, tabName) ->
                    val isSelected = guideTab == tabId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { guideTab = tabId }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        when (guideTab) {
            "STEPS" -> {
                item {
                    Text(
                        text = "The 4 Steps of Discernment",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    StepCard(
                        stepNumber = "Step 1",
                        title = "Be Aware of Yourself",
                        icon = Icons.Default.Person,
                        description = "Sincere discernment begins with quiet self-knowledge and acceptance of how you stand before Creator.",
                        details = listOf(
                            "Talk to Someone: God often speaks through the wisdom of mature counselors, pastors, or parish guides.",
                            "Find Solitude: carves out quiet times to invite God directly into your decision process.",
                            "Know Yourself: assess your likes, dislikes, dreams, limitations, and how you behave under sudden stress.",
                            "Accept Yourself: rest in the peace that God created you and loves you exactly as you are."
                        )
                    )
                }

                item {
                    StepCard(
                        stepNumber = "Step 2",
                        title = "Be in Touch with God",
                        icon = Icons.Default.Favorite,
                        description = "Create a sincere, honest relationship with God by hiding nothing and developing steady prayer.",
                        details = listOf(
                            "Pour out EVERYTHING: Tell God what you desire and what you fear without holding back.",
                            "Learn to Listen: pay attention to interior thoughts, memories, and especially feelings of love, joy, or deep peace.",
                            "Develop steady habits: experiment with different prayer forms until you feel connected with the divine.",
                            "Seek 'Thy Will': explicitly pray for the strength and detachment to follow what God desires most for you."
                        )
                    )
                }

                item {
                    StepCard(
                        stepNumber = "Step 3",
                        title = "The Decision-Making Process",
                        icon = Icons.Default.Edit,
                        description = "Analyze choices systematically based on reason first, then examine your emotional resonance.",
                        details = listOf(
                            "Start with known facts: list out pros and cons of morally acceptable options on a checklist.",
                            "Eliminate the illicit: ensure plans do not contradict scripture or the Church’s moral teachings.",
                            "Examine deathbed and judgment day perspectives: ask what you would wish you had chosen when looking back.",
                            "Look at the big picture: project what effect your decision will have 5 and 10 years from now."
                        )
                    )
                }

                item {
                    StepCard(
                        stepNumber = "Step 4",
                        title = "Confirm Your Decision",
                        icon = Icons.Default.CheckCircle,
                        description = "Discernment is an ongoing loop of action, monitoring outcomes, and finding persistent peace.",
                        details = listOf(
                            "Check out the fruits: examine if the outcomes (words, emotions, actions) are good, or if they bring anxiety and division.",
                            "Identify deep peace: a correct choice always leads to a feeling of peace and satisfaction, not just temporary relief.",
                            "Get involved in service: share your gifts and talents within a community of service.",
                            "Keep iterating: personal vocation unfolds throughout life; adapt plans when new situations arise."
                        )
                    )
                }
            }

            "RULES" -> {
                item {
                    Text(
                        text = "St. Ignatius's 14 First Week Rules",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    // Quick stats summary
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "CORE DEFINITIONS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "• Spiritual Consolation (Rule 3): An interior movement where the soul is inflamed with divine love, showing tears, joy, faith, hope, and peace.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = "• Spiritual Desolation (Rule 4): Darkness of soul, disturbance, low earthly movements, sadness, doubts, tepidity, and isolation.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                item {
                    RuleAccordion(
                        number = "Rule 1",
                        title = "Sensual Delights vs Sting of Conscience",
                        originalSummary = "In souls going from mortal sin to mortal sin, the enemy proposes apparent pleasure, while the good spirit bites and stings their conscience through moral judgment.",
                        guidanceText = "If you are sliding downward, realize that feelings of comfort in bad habits come from the enemy, while the painful bite of guilt is actually God inviting you back."
                    )
                }

                item {
                    RuleAccordion(
                        number = "Rule 2",
                        title = "Obstacles vs Courage of the progressive soul",
                        originalSummary = "In souls actively purifying sin and rising from good to better, the enemy bites and saddens with false reasons, while the good spirit gives courage, tearful consolations, and quiet peace.",
                        guidanceText = "If you are trying to do good, expect obstacles and sadness to be from the enemy trying to arrest your progress. Trust that sweetness, inspirations, and ease of heart are from God."
                    )
                }

                item {
                    RuleAccordion(
                        number = "Rule 3",
                        title = "Spiritual Consolation",
                        originalSummary = "An interior movement caused by God, leaving the soul inflamed with love, sheding tears of devotion, and experiencing an increase of hope, faith, charity, and quiet.",
                        guidanceText = "Praise God for this trial-free season. Use this time with deep gratitude to store up fuel and plan defenses for the desolation that will inevitably follow."
                    )
                }

                item {
                    RuleAccordion(
                        number = "Rule 4",
                        title = "Spiritual Desolation",
                        originalSummary = "A state of soul opposite to consolation: darkness, disturbance, attraction to cheap or base things, sadness, tepidity, and a feeling of separation from your Creator.",
                        guidanceText = "Do not panic or despair. Desolation is a normal part of spiritual life. God is testing your love to build stamina and humility."
                    )
                }

                item {
                    RuleAccordion(
                        number = "Rule 5",
                        title = "Never make a change or resolution",
                        originalSummary = "In time of desolation, never alter any proposal or choice made during consolation. The bad spirit counsels in desolation, whose advice leads to ruin.",
                        guidanceText = "CRITICAL RULE: If you feel separated, dry, or frustrated, freeze all big decisions! Keep executing old plans exactly. The dark clouds will pass soon, bringing clear light."
                    )
                }

                item {
                    RuleAccordion(
                        number = "Rule 6",
                        title = "React Intensely against Desolation",
                        originalSummary = "Although we must not alter our proposals, we can and should change ourselves against desolation by doubling prayer, meditation, self-examination, and penance.",
                        guidanceText = "Be proactive! Fight dryness by spending more time on scripture prayer or journaling. Sitting in passive gloom feeds the enemy."
                    )
                }

                item {
                    RuleAccordion(
                        number = "Rule 7",
                        title = "Expect God is refining you",
                        originalSummary = "Consider that the Lord has left you to be tested in your natural powers to resist temptation, but always leaves sufficient grace for your salvation.",
                        guidanceText = "You are not abandoned. God simply pulled back His sensible feeling of warmth so that you learn to stand on your own feet under grace."
                    )
                }

                item {
                    RuleAccordion(
                        number = "Rule 8",
                        title = "Cultivate Patience",
                        originalSummary = "He who is desolated must work to be in patience, which opposes the vexations of desolation, and remember that he will soon be consoled.",
                        guidanceText = "Maintain a patient view of history. Say to yourself: 'This is temporary. Happiness will return shortly.'"
                    )
                }

                item {
                    RuleAccordion(
                        number = "Rule 9",
                        title = "Three Causes of Desolation",
                        originalSummary = "Desolation occurs: 1) because we are tepid or negligent in exercises; 2) as a trial to see how much we serve without rewards; 3) to give us humility and realize consolation is a gift.",
                        guidanceText = "Do an audit! Ask: Have I skipped daily prayer (Rule 9.1)? Am I being tested (Rule 9.2)? Or do I need to learn that I cannot produce joy on my own command (Rule 9.3)?"
                    )
                }

                item {
                    RuleAccordion(
                        number = "Rule 10",
                        title = "Store consolation strength",
                        originalSummary = "Let the one in consolation think how he will conduct himself when desolation comes after, gathering new strength for that trial.",
                        guidanceText = "Write down insights, experiences, and promises while you are happy. You will need to read these letters to yourself when darkness visits."
                    )
                }

                item {
                    RuleAccordion(
                        number = "Rule 11",
                        title = "Humility vs Courageous Confidence",
                        originalSummary = "Let the consoled humble himself, remembering how small he is without grace. Let the desolated take strength that God's sufficient grace supports him.",
                        guidanceText = "Keep your balance. In joy, don't get arrogant. In sorrow, don't get despairing."
                    )
                }

                item {
                    RuleAccordion(
                        number = "Rule 12",
                        title = "Stand Firm (The Weapon of Weakness)",
                        originalSummary = "The enemy acts like a weak opponent when confronted firmly, but becomes fierce if you lose heart. Confront temptations directly to make him flee.",
                        guidanceText = "Tackle fear instantly! The enemy's power is an illusion that crumbles when you act with courage and turn your face to God."
                    )
                }

                item {
                    RuleAccordion(
                        number = "Rule 13",
                        title = "Expose Secrets (Tactic of the False Lover)",
                        originalSummary = "The enemy desires his suggestions to remain secret, in the manner of a false lover. When revealed to a trusted confessor or counselor, his wiles are neutralized.",
                        guidanceText = "Break the spell of isolation! If you are hiding thoughts or feel too embarrassed to talk, go and tell your spiritual director immediately. Light completely destroys his hold."
                    )
                }

                item {
                    RuleAccordion(
                        number = "Rule 14",
                        title = "Secure Weak Links (Tactic of the Commander)",
                        originalSummary = "The enemy acts like a military leader examining a fortress to attack at its weakest point of virtue.",
                        guidanceText = "Do a vulnerability scan. Find your weakest link—be it anger, screens, gossip, or sloth—and double your guards there to prevent surprise breach."
                    )
                }
            }

            "STORIES" -> {
                item {
                    Text(
                        text = "Historical Struggles of the Saints",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                item {
                    SaintStoryCard(
                        saintName = "St. Ignatius of Loyola",
                        struggleTitle = "The Origin of Rules: Recovery at Loyola",
                        quote = "\"When he thought of the things of the world, he took much delight in them, but afterwards, he was dry and discontented. When he thought of the holy saints, he remained content and happy...\"",
                        analysisText = "While recovering from a cannonball wound, Ignatius noticed a lingering effect: worldly fantasies left him dry, while sacred desires left him peaceful and satisfied. This is the seed of the rules of discernment: tracking how feelings behave AFTER the thoughts pass."
                    )
                }

                item {
                    SaintStoryCard(
                        saintName = "St. Augustine",
                        struggleTitle = "Intense Indecision: The Garden Dialogue",
                        quote = "\"I was held back by mere trifles... whispering 'Are you going to dismiss us?' But on the other side stood Continence, modest and smiling: 'Can you not do what these men and women do?'\"",
                        analysisText = "Augustine felt the bite of conscience as he prepared to turn his life around. Rule 1 warns that the enemy proposes sensual attachments to delay conversion, but radical surrender to God's love breaks the chains completely."
                    )
                }

                item {
                    SaintStoryCard(
                        saintName = "St. Thérèse of Lisieux",
                        struggleTitle = "The Wedding Eve Storm: False Vocation Doubt",
                        quote = "\"Unbelievable doubts entered my mind... Carmel was a dream, a chimera. The devil assured me it wasn't for me... I made the Novice Mistress come out and told her. Instantly, the doubts fled.\"",
                        analysisText = "Right before pronouncing her vows, St. Thérèse went through a terrible spiritual dark night (Rule 13). By breaking the secret pattern and immediately exposing the thoughts to her guide, she put the enemy to flight and entered supreme peace."
                    )
                }

                item {
                    SaintStoryCard(
                        saintName = "Thomas Merton",
                        struggleTitle = "The Physical Blockade: Father Philotheus's Door",
                        quote = "\"I walked bravely to his door, but within 6 feet, someone stopped me. Something jammed in my will. I couldn't walk a step... but after intense prayer, I went, asked, and the scales fell of my eyes.\"",
                        analysisText = "Merton describes a paralyzing blockade right before speaking to a counselor. This demonstrates how the enemy resists exposure. Yet, acting against this resistance with a single act of willpower, he discovered peace."
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StepCard(
    stepNumber: String,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    details: List<String>
) {
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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stepNumber.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand Detail",
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            Text(
                text = description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))
                    details.forEach { detail ->
                        Row(modifier = Modifier.padding(bottom = 8.dp)) {
                            Text(
                                text = "•",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = detail,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RuleAccordion(
    number: String,
    title: String,
    originalSummary: String,
    guidanceText: String
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand Rules Detail",
                    tint = MaterialTheme.colorScheme.outline
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "THE RULE TEXT:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = originalSummary,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "PRACTICAL SPIRITUAL TACTIC:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = guidanceText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SaintStoryCard(
    saintName: String,
    struggleTitle: String,
    quote: String,
    analysisText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = saintName.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Text(
                text = struggleTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = quote,
                    fontSize = 12.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "PRACTICAL INSIGHT:",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = analysisText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

