package org.iceterm

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import org.apache.commons.lang.StringUtils
import org.iceterm.ceintegration.ConEmuStartInfo
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
        if(StringUtils.isNotEmpty(myState.myConEmuPath)) {
            var path: String? = myState.myConEmuPath
            path = path?.replace("^\"|\"$", "")
            path = path?.replace("^\'|\'$", "")
            return path?.trim()
        }
        return defaultConEmuPath()
    }

    fun setConEmuPath(conemuPath: String) {
        myState.myConEmuPath = conemuPath
    }

    fun getConEmuXmlPath(): String? {
        if(StringUtils.isNotEmpty(myState.myConEmuXml)) {
            var path: String? = myState.myConEmuXml
            path = path?.replace("^\"|\"$", "")
            path = path?.replace("^\'|\'$", "")
            return path?.trim()
        }
        return defaultConEmuXmlPath()
    }

    fun setConEmuXmlPath(conEmuXmlPath: String) {
        myState.myConEmuXml = conEmuXmlPath
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
        var myConEmuXml: String? = null
        var myShellTask: String? = null
        var myEscapeKey: String? = null
    }

    fun defaultConEmuPath(): String {
        return defaultStartInfo.getConEmuExecutablePath();
    }

    fun defaultConEmuXmlPath(): String {
        return defaultStartInfo.conEmuXmlPath;
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
