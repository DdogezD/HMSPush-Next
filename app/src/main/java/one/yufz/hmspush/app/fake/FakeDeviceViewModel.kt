package one.yufz.hmspush.app.fake

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import one.yufz.hmspush.app.hms.SupportHmsAppList

data class AppConfig(
    val name: String,
    val packageName: String,
    val enabled: Boolean,
    val skipBuild: Boolean = false,
    val processes: List<String> = emptyList()
)

data class UIState(val configList: List<AppConfig>, val filterKeywords: String = "") {
    val filteredConfigList: List<AppConfig> = if (filterKeywords.isEmpty()) configList
    else configList.filter { it.name.contains(filterKeywords, true) || it.packageName.contains(filterKeywords, true) }
}

class FakeDeviceViewModel(val app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(UIState(emptyList()))
    val uiState: StateFlow<UIState> = _uiState

    private val supportedAppList = SupportHmsAppList(app)
    private val fakeDeviceConfig = FakeDeviceConfig

    init {
        viewModelScope.launch { fakeDeviceConfig.loadConfig(); supportedAppList.init() }

        viewModelScope.launch(Dispatchers.IO) {
            combine(supportedAppList.appListFlow, fakeDeviceConfig.configMapFlow, ::mergeSource)
                .collect { list ->
                    _uiState.update { old ->
                        val appConfigs = if (old.configList.isNotEmpty()) mergeConfigList(old.configList, list)
                        else list.sortedByDescending { it.enabled }
                        old.copy(configList = appConfigs)
                    }
                }
        }
        load()
    }

    private fun mergeConfigList(current: List<AppConfig>, newList: List<AppConfig>): List<AppConfig> {
        val newConfigMap = newList.associateBy { it.packageName }.toMutableMap()
        val merged = current.map { old -> newConfigMap.remove(old.packageName) ?: old }
        return merged + newConfigMap.values
    }

    private fun mergeSource(supportList: List<String>, configs: ConfigMap): List<AppConfig> {
        return supportList.map { pkg ->
            val pair = configs[pkg]
            val processes = pair?.first ?: emptyList()
            val skipBuild = pair?.second ?: false
            AppConfig(loadName(pkg), pkg, configs.containsKey(pkg), skipBuild, processes)
        }
    }

    private fun loadName(packageName: String): String {
        return try { app.packageManager.getApplicationInfo(packageName, 0).loadLabel(app.packageManager).toString() }
        catch (e: PackageManager.NameNotFoundException) { packageName }
    }

    fun load() { viewModelScope.launch { fakeDeviceConfig.loadConfig() } }

    fun update(appConfig: AppConfig) {
        viewModelScope.launch {
            if (appConfig.enabled) fakeDeviceConfig.update(appConfig.packageName, appConfig.processes, appConfig.skipBuild)
            else fakeDeviceConfig.deleteConfig(appConfig.packageName)
        }
    }

    fun setSkipBuild(appConfig: AppConfig, skip: Boolean) {
        viewModelScope.launch { fakeDeviceConfig.update(appConfig.packageName, appConfig.processes, skip) }
    }

    fun addProcess(appConfig: AppConfig, process: String) {
        viewModelScope.launch { fakeDeviceConfig.update(appConfig.packageName, appConfig.processes + process, appConfig.skipBuild) }
    }

    fun removeProcess(appConfig: AppConfig, process: String) {
        viewModelScope.launch { fakeDeviceConfig.update(appConfig.packageName, appConfig.processes - process, appConfig.skipBuild) }
    }

    fun filter(filter: String) { _uiState.update { it.copy(filterKeywords = filter) } }
}