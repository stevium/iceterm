package iceterm

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.xmlb.annotations.Property
import java.io.File

@State(name = "IceTermOptionsProvider", storages = [(Storage("plugin.xml"))])
class IceTermOptionsProvider : PersistentStateComponent<IceTermOptionsProvider.State> {
    private var myState = State()

    override fun getState(): State? {
        return myState
    }

    override fun loadState(state: State) {
        myState = state
    }

    fun getShellPath(): String? {
        return myState.myShellPath ?: defaultShellPath()
    }

    fun setShellPath(shellPath: String) {
        myState.myShellPath = shellPath
    }

    fun audibleBell(): Boolean {
        return myState.mySoundBell
    }

    var tabName: String
        get() = myState.myTabName
        set(tabName) {
            myState.myTabName = tabName
        }

    class State {
        var myShellPath: String? = null
        var myTabName: String = "Local"
        var mySoundBell: Boolean = true
        @get:Property(surroundWithTag = false, flat = true)
        var envDataOptions = EnvironmentVariablesDataOptions()
    }

    fun setSoundBell(soundBell: Boolean) {
        myState.mySoundBell = soundBell
    }

    fun getEnvData(): EnvironmentVariablesData {
        return myState.envDataOptions.get()
    }

    fun setEnvData(envData: EnvironmentVariablesData) {
        myState.envDataOptions.set(envData)
    }


    private fun defaultShellPath(): String {
        val shell = System.getenv("SHELL")
        if (shell != null && File(shell).canExecute()) {
            return shell
        }
        if (SystemInfo.isUnix) {
            val bashPath = "/bin/bash"
            if (File(bashPath).exists()) {
                return bashPath
            }
            return "/bin/sh"
        }
        return "cmd.exe"
    }

    companion object {
        val instance: IceTermOptionsProvider
            @JvmStatic
            get() = ServiceManager.getService(IceTermOptionsProvider::class.java)
    }
}
