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

    var escapeKey: KeyStroke
        get() = if (myState.myEscapeKey != null) {
            try {
                KeyStroke.getKeyStroke(myState.myEscapeKey)
            } catch (e: Exception) {
                defaultEscapeKey()
            }
        }else {
            defaultEscapeKey()
        }
        set(escapeKey) {
            myState.myEscapeKey = escapeKey.toString()
        }

    class State {
        var myConEmuPath: String? = null
        var myShellTask: String? = null
        var myEscapeKey: String? = null
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

    fun defaultEscapeKey(): KeyStroke {
        return KeyStroke.getKeyStroke("ESCAPE")
    }

    companion object {
        val instance: IceTermOptionsProvider
            @JvmStatic
            get() = ServiceManager.getService(IceTermOptionsProvider::class.java)
    }
}
