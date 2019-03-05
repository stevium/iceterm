package iceterm

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.SystemInfo
import java.io.File
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty

/**
 * @author traff
 */
@State(name = "IceTermProjectOptionsProvider", storages = [(Storage("plugin.xml"))])
class IceTermProjectOptionsProvider(val project: Project) : PersistentStateComponent<IceTermProjectOptionsProvider.State> {

    private val myState = State()

    override fun getState(): State? {
        return myState
    }

    override fun loadState(state: State) {
        myState.myStartingDirectory = state.myStartingDirectory
    }

    class State {
        var myStartingDirectory: String? = null
    }

    var startingDirectory: String? by ValueWithDefault(State::myStartingDirectory, myState) { defaultStartingDirectory }

    val defaultStartingDirectory: String?
        get() {
            var directory: String? = null
            for (customizer in LocalTerminalCustomizer.EP_NAME.extensions) {
                try {

                    if (directory == null) {
                        directory = customizer.getDefaultFolder(project)
                    }
                }
                catch (e: Exception) {
                    LOG.error("Exception during getting default folder", e)
                }
            }

            return directory ?: getDefaultWorkingDirectory()
        }

    private fun getDefaultWorkingDirectory(): String? {
        val roots = ProjectRootManager.getInstance(project).contentRoots
        @Suppress("DEPRECATION")
        val dir = if (roots.size == 1 && roots[0] != null && roots[0].isDirectory) roots[0] else project.baseDir
        return dir?.canonicalPath
    }

    val defaultShellPath: String
        get() {
            val shell = System.getenv("SHELL")

            if (shell != null && File(shell).canExecute()) {
                return shell
            }

            if (SystemInfo.isUnix) {
                if (File("/bin/bash").exists()) {
                    return "/bin/bash"
                }
                else {
                    return "/bin/sh"
                }
            }
            else {
                return "cmd.exe"
            }
        }

    companion object {
        private val LOG = Logger.getInstance(IceTermProjectOptionsProvider::class.java)

        @JvmStatic
        fun getInstance(project: Project): IceTermProjectOptionsProvider {
            return ServiceManager.getService(project, IceTermProjectOptionsProvider::class.java)
        }
    }

}

// TODO: In Kotlin 1.1 it will be possible to pass references to instance properties. Until then we need 'state' argument as a receiver for
// to property to apply
class ValueWithDefault<S>(val prop: KMutableProperty1<S, String?>, val state: S, val default: () -> String?) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String? {
        return if (prop.get(state) !== null) prop.get(state) else default()
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        prop.set(state, if (value == default() || value.isNullOrEmpty()) null else value)
    }
}
