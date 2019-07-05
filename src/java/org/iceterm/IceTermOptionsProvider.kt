package org.iceterm

import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.annotations.Property
import org.iceterm.ceintegration.ConEmuStartInfo
import java.lang.Exception
import javax.swing.KeyStroke

@State(name = "IceTermOptionsProvider", storages = [(Storage("plugin.xml"))])
class IceTermOptionsProvider : PersistentStateComponent<IceTermOptionsProvider.State> {
    private var defaultStartInfo:  ConEmuStartInfo = ConEmuStartInfo()
    private var myState = State()

    override fun getState(): State? {
        return myState
    }

    override fun loadState(state: State) {
        myState = state
    }

    fun getConEmuPath(): String? {
        return myState.myConEmuPath ?: defaultConEmuPath()
    }

    fun setConEmuPath(conemuPath: String) {
        myState.myConEmuPath = conemuPath
    }

    fun getShellTask(): String? {
        return myState.myShellTask ?: defaultShellTask()
    }

    fun setShellTask(task: String) {
        myState.myShellTask = task
    }

    var prefixKey: KeyStroke
        get() = if (myState.myPrefixKey != null) {
            try {
                KeyStroke.getKeyStroke(myState.myPrefixKey)
            } catch (e: Exception) {
                defaultPrefixKey()
            }
        }else {
            defaultPrefixKey()
        }
        set(prefixKey) {
            myState.myPrefixKey = prefixKey.toString()
        }

    class State {
        var myConEmuPath: String? = null
        var myShellTask: String? = null
        var myPrefixKey: String? = null
        @get:Property(surroundWithTag = false, flat = true)
        var envDataOptions = EnvironmentVariablesDataOptions()
    }

    fun getEnvData(): EnvironmentVariablesData {
        return myState.envDataOptions.get()
    }

    fun setEnvData(envData: EnvironmentVariablesData) {
        myState.envDataOptions.set(envData)
    }


    fun defaultConEmuPath(): String {
//        val shell = System.getenv("SHELL")
//        if (shell != null && File(shell).canExecute()) {
//            return shell
//        }
//        if (SystemInfo.isUnix) {
//            val bashPath = "/bin/bash"
//            if (File(bashPath).exists()) {
//                return bashPath
//            }
//            return "/bin/sh"
//        }
//        return "cmd.exe"

//        return defaultStartInfo.getConEmuExecutablePath();
        return defaultStartInfo.getConEmuExecutablePath();
    }

    fun defaultShellTask(): String {
        return defaultStartInfo.getConsoleProcessCommandLine();
    }

    fun defaultPrefixKey(): KeyStroke {
        return KeyStroke.getKeyStroke("control SPACE")
    }

    companion object {
        val instance: IceTermOptionsProvider
            @JvmStatic
            get() = ServiceManager.getService(IceTermOptionsProvider::class.java)
    }
}
