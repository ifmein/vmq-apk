package vmq.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.vone.qrcode.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainRoute(
    viewModel: MainViewModel,
    onScanQrCode: () -> Unit,
    onManualInput: () -> Unit,
    onCheckHeartbeat: () -> Unit,
    onCheckPush: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    MainScreen(
        uiState = uiState,
        onScanQrCode = onScanQrCode,
        onManualInput = onManualInput,
        onCheckHeartbeat = onCheckHeartbeat,
        onCheckPush = onCheckPush,
    )
}

@Composable
fun MainScreen(
    uiState: MainUiState,
    onScanQrCode: () -> Unit,
    onManualInput: () -> Unit,
    onCheckHeartbeat: () -> Unit,
    onCheckPush: () -> Unit,
) {
    val config = uiState.config

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        MainToolbar()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            ConfigCard(config)

            Spacer(modifier = Modifier.height(16.dp))

            QuickActionsSection(
                onScanQrCode = onScanQrCode,
                onManualInput = onManualInput,
                onCheckHeartbeat = onCheckHeartbeat,
                onCheckPush = onCheckPush,
            )

            FooterText()
        }
    }
}

@Composable
private fun MainToolbar() {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = stringResource(R.string.main_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = stringResource(R.string.main_subtitle),
                    fontSize = 13.sp,
                    color = ComposeColor(0xFFD6E2FF),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    )
}

@Composable
private fun ConfigCard(config: vmq.data.AppConfig) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.main_config_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.main_config_subtitle),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            ConfigItem(
                label = stringResource(R.string.main_config_host_label),
                value = if (config.host.isBlank()) stringResource(R.string.main_config_empty) else config.host,
                modifier = Modifier.padding(top = 14.dp),
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outline,
            )

            ConfigItem(
                label = stringResource(R.string.main_config_key_label),
                value = if (config.key.isBlank()) stringResource(R.string.main_config_empty) else config.key,
            )
        }
    }
}

@Composable
private fun ConfigItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun QuickActionsSection(
    onScanQrCode: () -> Unit,
    onManualInput: () -> Unit,
    onCheckHeartbeat: () -> Unit,
    onCheckPush: () -> Unit,
) {
    Text(
        text = stringResource(R.string.main_actions_title),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = stringResource(R.string.main_actions_subtitle),
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ActionTile(
            title = stringResource(R.string.main_action_scan_title),
            subtitle = stringResource(R.string.main_action_scan_subtitle),
            modifier = Modifier
                .weight(1f)
                .testTag("scan_config_action"),
            onClick = onScanQrCode,
        )
        ActionTile(
            title = stringResource(R.string.main_action_manual_title),
            subtitle = stringResource(R.string.main_action_manual_subtitle),
            modifier = Modifier
                .weight(1f)
                .testTag("manual_config_action"),
            onClick = onManualInput,
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ActionTile(
            title = stringResource(R.string.main_action_heartbeat_title),
            subtitle = stringResource(R.string.main_action_heartbeat_subtitle),
            modifier = Modifier
                .weight(1f)
                .testTag("heartbeat_action"),
            onClick = onCheckHeartbeat,
        )
        ActionTile(
            title = stringResource(R.string.main_action_push_title),
            subtitle = stringResource(R.string.main_action_push_subtitle),
            modifier = Modifier
                .weight(1f)
                .testTag("push_test_action"),
            onClick = onCheckPush,
        )
    }
}

@Composable
private fun FooterText() {
    Text(
        text = stringResource(R.string.main_footer_hint),
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
fun ActionTile(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .height(82.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = ComposeColor(0xFFDCE6FF),
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}
