package com.priorityalert.app.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.priorityalert.app.Contact
import com.priorityalert.app.Pending
import com.priorityalert.app.R
import com.priorityalert.app.Store

data class SetupState(val access: Boolean, val battery: Boolean, val overlay: Boolean) {
    val done: Int get() = listOf(access, battery, overlay).count { it }
    val complete: Boolean get() = done == 3
}

private val TimeoutPresets = listOf(1, 2, 5, 10, 15, 30)

private fun plural(n: Int, one: String, many: String) = "$n ${if (n > 1) many else one}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    contacts: List<Contact>,
    pending: Map<String, Pending>,
    setup: SetupState,
    now: Long,
    onOpenNotificationAccess: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenAutoStart: () -> Unit,
    onTestAlert: () -> Unit,
    onSave: (index: Int, contact: Contact) -> Unit,
    onDelete: (index: Int) -> Unit,
    onToggle: (index: Int, enabled: Boolean) -> Unit,
) {
    // null = editor closed, -1 = new contact, otherwise the index being edited.
    var editing by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // Opaque strip behind the status bar so the list doesn't scroll under the clock.
        topBar = {
            Spacer(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars)
                    .background(MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = -1 },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Ajouter un contact", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
            )
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 104.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { Header() }
            item { StatusCard(setup, contacts, pending, onTestAlert) }
            if (!setup.complete) {
                item { SetupCard(setup, onOpenNotificationAccess, onOpenBattery, onOpenOverlay) }
            }
            item { SectionTitle("Contacts importants", contacts.size) }
            if (contacts.isEmpty()) {
                item { EmptyContacts(onAdd = { editing = -1 }) }
            } else {
                itemsIndexed(contacts, key = { i, c -> "$i|${c.name}" }) { i, contact ->
                    ContactCard(
                        contact = contact,
                        pending = pending[Store.normalize(contact.name)],
                        now = now,
                        onClick = { editing = i },
                        onToggle = { onToggle(i, it) },
                    )
                }
            }
            item { AutoStartTip(onOpenAutoStart) }
        }
    }

    editing?.let { index ->
        ContactEditor(
            initial = contacts.getOrNull(index),
            onDismiss = { editing = null },
            onSave = {
                onSave(index, it)
                editing = null
            },
            onDelete = if (index >= 0) {
                {
                    onDelete(index)
                    editing = null
                }
            } else {
                null
            },
        )
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        Image(
            painter = painterResource(R.drawable.ic_logo),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                "Priority Alert",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Vos messages importants ne passent plus inaperçus",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusCard(
    setup: SetupState,
    contacts: List<Contact>,
    pending: Map<String, Pending>,
    onTestAlert: () -> Unit,
) {
    val active = contacts.count { it.enabled }
    val waiting = pending.size
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(NavyLight, Navy, NavyDeep)))
            .padding(22.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(ok = setup.complete)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (setup.complete) "Surveillance active" else "Configuration requise",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                when {
                    !setup.complete -> "Encore ${plural(3 - setup.done, "étape", "étapes")} et c'est prêt"
                    active == 0 -> "Ajoutez votre premier contact"
                    else -> "${plural(active, "contact surveillé", "contacts surveillés")}"
                },
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (waiting > 0) {
                    "${plural(waiting, "message attend", "messages attendent")} votre réponse"
                } else {
                    "Aucun message en attente de réponse"
                },
                color = if (waiting > 0) Amber else Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onTestAlert,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White.copy(alpha = 0.12f),
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tester l'alerte")
            }
        }
    }
}

/** Green dot that gently pulses when everything works, amber when setup is missing. */
@Composable
private fun StatusDot(ok: Boolean) {
    val color = if (ok) Mint else Amber
    val transition = rememberInfiniteTransition(label = "status")
    val halo by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "halo",
    )
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(18.dp)) {
        Box(Modifier.size(18.dp).alpha(halo).background(color, CircleShape))
        Box(Modifier.size(9.dp).background(color, CircleShape))
    }
}

@Composable
private fun SetupCard(
    setup: SetupState,
    onOpenNotificationAccess: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenOverlay: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${setup.done}/3",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { setup.done / 3f },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                drawStopIndicator = {},
            )
            Spacer(Modifier.height(8.dp))
            SetupRow(
                Icons.Filled.Notifications,
                "Lire les notifications",
                "Pour voir les messages WhatsApp",
                setup.access,
                onOpenNotificationAccess,
            )
            SetupRow(
                Icons.Filled.Build,
                "Ignorer l'économie de batterie",
                "Pour que la surveillance ne s'endorme pas",
                setup.battery,
                onOpenBattery,
            )
            SetupRow(
                Icons.Filled.Lock,
                "Afficher sur l'écran verrouillé",
                "Pour voir l'alerte même téléphone verrouillé",
                setup.overlay,
                onOpenOverlay,
            )
        }
    }
}

@Composable
private fun SetupRow(icon: ImageVector, title: String, subtitle: String, done: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
    ) {
        IconBadge(icon, tint = if (done) Mint else MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        if (done) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Fait", tint = Mint)
        } else {
            FilledTonalButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) { Text("Activer") }
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector, tint: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(42.dp).background(tint.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SectionTitle(title: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (count > 0) {
            Spacer(Modifier.width(8.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .padding(horizontal = 9.dp, vertical = 2.dp),
            ) {
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ContactCard(
    contact: Contact,
    pending: Pending?,
    now: Long,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val waiting = contact.enabled && pending != null
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (waiting) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Avatar(
                contact.name,
                size = 48.dp,
                modifier = Modifier.alpha(if (contact.enabled) 1f else 0.4f),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (contact.enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(Modifier.height(2.dp))
                when {
                    !contact.enabled -> Text(
                        "En pause",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    pending != null -> {
                        val left = ((pending.deadline - now) / 1000).coerceAtLeast(0)
                        Text(
                            if (left > 0) {
                                "Sans réponse · alerte dans %d:%02d".format(left / 60, left % 60)
                            } else {
                                "Alerte en cours"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    else -> Text(
                        "Alerte après ${contact.timeoutMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = contact.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
    }
}

@Composable
private fun EmptyContacts(onAdd: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(28.dp),
        ) {
            Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Aucun contact important", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Ajoutez les personnes dont vous ne voulez jamais manquer un message.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(18.dp))
            Button(onClick = onAdd, shape = RoundedCornerShape(14.dp)) { Text("Ajouter un contact") }
        }
    }
}

@Composable
private fun AutoStartTip(onOpen: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.padding(top = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Filled.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Infinix, Tecno ou itel ?", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Activez le démarrage automatique de l'app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onOpen) { Text("Ouvrir") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ContactEditor(
    initial: Contact?,
    onDismiss: () -> Unit,
    onSave: (Contact) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var minutes by remember { mutableIntStateOf(initial?.timeoutMinutes ?: 5) }
    val presets = (TimeoutPresets + minutes).distinct().sorted()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp).imePadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(name.ifBlank { "?" }, size = 52.dp)
                Spacer(Modifier.width(14.dp))
                Text(
                    if (initial == null) "Nouveau contact" else "Modifier le contact",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom du contact") },
                supportingText = { Text("Exactement comme il apparaît dans WhatsApp") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Text("Alerte si pas de réponse après", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                presets.forEach { m ->
                    FilterChip(
                        selected = m == minutes,
                        onClick = { minutes = m },
                        label = { Text("$m min") },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Supprimer")
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = { onSave(Contact(name.trim(), minutes, initial?.enabled ?: true)) },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                ) { Text("Enregistrer", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}
