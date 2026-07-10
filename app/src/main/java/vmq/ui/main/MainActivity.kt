package vmq.ui.main

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vone.qrcode.R
import vmq.di.AppContainer
import vmq.ui.common.UiText
import vmq.ui.scan.CaptureActivity
import vmq.parser.PaymentNotificationParser
import vmq.ui.notification.NeNotificationService
import vmq.ui.theme.VmqTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var scanLauncher: ActivityResultLauncher<Intent>
    private lateinit var viewModel: MainViewModel
    private var notificationId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register scan launcher
        scanLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val scanResult = result.data?.getStringExtra("SCAN_RESULT")
                if (scanResult.isNullOrEmpty()) {
                    Toast.makeText(this, getString(R.string.invalid_qr_configuration), Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.handleConfigurationPayload(
                        scanResult,
                        UiText.StringResource(R.string.invalid_qr_configuration),
                    )
                }
            }
        }

        setupViewModel()

        toggleNotificationListenerService(this)
        viewModel.loadSavedConfiguration()

        Toast.makeText(this, getString(R.string.app_version_toast, appVersionName()), Toast.LENGTH_SHORT).show()

        setContent {
            VmqTheme {
                MainRoute(
                    viewModel = viewModel,
                    onScanQrCode = { startCameraScan() },
                    onManualInput = { showManualInputDialog() },
                    onCheckHeartbeat = { viewModel.startHeartbeat() },
                    onCheckPush = { checkPushAndPostNotification() }
                )
            }
        }
    }

    private fun appVersionName(): String {
        @Suppress("DEPRECATION")
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        return packageInfo.versionName.orEmpty()
    }

    private fun setupViewModel() {
        val factory = MainViewModelFactory(AppContainer.configRepository(applicationContext))
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effects.collect { effect ->
                    when (effect) {
                        is MainViewEffect.ShowToast -> showToast(effect.message)
                    }
                }
            }
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraScanner()
        } else {
            Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_LONG).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            postTestNotification()
        } else {
            Toast.makeText(this, getString(R.string.notification_permission_required), Toast.LENGTH_LONG).show()
        }
    }

    private fun startCameraScan() {
        if (!isNotificationListenersEnabled()) {
            gotoNotificationAccessSetting()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        launchCameraScanner()
    }

    private fun launchCameraScanner() {
        val intent = Intent(this, CaptureActivity::class.java)
        scanLauncher.launch(intent)
    }

    private fun showManualInputDialog() {
        val inputServer = EditText(this)
        AlertDialog.Builder(this)
            .setTitle(R.string.manual_input_title)
            .setView(inputServer)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_confirm) { _, _ ->
                viewModel.handleConfigurationPayload(
                    inputServer.text.toString(),
                    UiText.StringResource(R.string.invalid_manual_configuration),
                )
            }
            .show()
    }

    private fun checkPushAndPostNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    postTestNotification()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showToast(UiText.StringResource(R.string.notification_permission_needed_for_test))
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            postTestNotification()
        }
    }

    private fun postTestNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        if (notificationManager == null) {
            showToast(UiText.StringResource(R.string.notification_service_unavailable))
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TEST_NOTIFICATION_CHANNEL_ID,
                "VMQ Test",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                enableLights(true)
                lightColor = Color.GREEN
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, TEST_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker(PaymentNotificationParser.SELF_TEST_MESSAGE)
            .setContentTitle(getString(R.string.notification_test_title))
            .setContentText(PaymentNotificationParser.SELF_TEST_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(notificationId++, notification)
        }
    }

    private fun showToast(message: UiText) {
        runOnUiThread {
            Toast.makeText(this@MainActivity, message.resolve(this@MainActivity), Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleNotificationListenerService(context: Context) {
        val packageManager = context.packageManager
        val componentName = ComponentName(context, NeNotificationService::class.java)

        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
        packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )

        Toast.makeText(this, getString(R.string.notification_listener_starting), Toast.LENGTH_SHORT).show()
    }

    fun isNotificationListenersEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!flat.isNullOrEmpty()) {
            val names = flat.split(':')
            for (name in names) {
                val componentName = ComponentName.unflattenFromString(name)
                if (componentName != null && TextUtils.equals(pkgName, componentName.packageName)) {
                    return true
                }
            }
        }
        return false
    }

    protected fun gotoNotificationAccessSetting(): Boolean {
        return try {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            true
        } catch (error: ActivityNotFoundException) {
            try {
                val intent = Intent()
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val componentName = ComponentName(
                    "com.android.settings",
                    "com.android.settings.Settings\$NotificationAccessSettingsActivity",
                )
                intent.component = componentName
                intent.putExtra(":settings:show_fragment", "NotificationAccessSettings")
                startActivity(intent)
                true
            } catch (fallbackError: Exception) {
                Toast.makeText(this, getString(R.string.notification_listener_settings_unsupported), Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    companion object {
        private const val TEST_NOTIFICATION_CHANNEL_ID = "vmq_test_channel"
    }
}
