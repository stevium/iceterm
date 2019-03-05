package iceterm

import com.intellij.configurationStore.Property
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.components.BaseState
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XMap

@Tag("")
class EnvironmentVariablesDataOptions : BaseState() {
    @Property(description = "Environment variables")
    @get:XMap(entryTagName = "env", keyAttributeName = "key")
    var envs: MutableMap<String, String> by property(LinkedHashMap())

    var isPassParentEnvs by property(true)

    fun set(envData: EnvironmentVariablesData) {
        envs = envData.envs
        isPassParentEnvs = envData.isPassParentEnvs
    }

    fun get(): EnvironmentVariablesData {
        return EnvironmentVariablesData.create(envs, isPassParentEnvs)
    }
}
