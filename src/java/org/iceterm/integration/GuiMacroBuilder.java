package org.iceterm.integration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.iceterm.util.tasks.Task;
import org.apache.commons.lang.NullArgumentException;
import org.jetbrains.annotations.NotNull;

/**
 * Fluent API for constructing a GUI macro. Start with the running {@link ConEmuSession}, call {@link ConEmuSession#beginGuiMacro(String)}.
 */
public class GuiMacroBuilder {

    @NotNull
    private ConEmuSession owner;

    @NotNull
    private Iterable<String> parameters;

    @NotNull
    private String sMacroName;

    public GuiMacroBuilder(@NotNull ConEmuSession owner, @NotNull String sMacroName, @NotNull Iterable<String> parameters) {
        if(owner == null)
            throw new NullArgumentException("owner");
        if(sMacroName == null)
            throw new NullArgumentException("sMacroName");
        if(parameters == null)
            throw new NullArgumentException("parameters");

        this.owner = owner;
        this.sMacroName = sMacroName;
        this.parameters = parameters;
    }

    /**
     * Renders the macro and executes with ConEmu, asynchronously.
     */
    @NotNull
    public Task<GuiMacroResult> executeAsync()
    {
        return owner.ExecuteGuiMacroTextAsync(RenderMacroCommand(sMacroName, parameters));
    }

    /**
     * Renders the macro and executes with ConEmu, getting the result synchronously.
     */
    @NotNull
    public GuiMacroResult executeSync()
    {
        return owner.ExecuteGuiMacroTextSync(RenderMacroCommand(sMacroName, parameters));
    }

    @NotNull
    private String RenderMacroCommand(@NotNull String sMacroName, @NotNull Iterable<String> parameters) {
        if(sMacroName == null)
            throw new NullArgumentException(("sMacroName"));
        if(parameters == null)
            throw new NullArgumentException(("parameters"));

        StringBuilder sb = new StringBuilder();
        if(!isAlphanumeric(sMacroName))
            throw new IllegalStateException("The macro name must be alphanumeric.");
        sb.append(sMacroName);

        for (String parameter: parameters) {
            sb.append(' ');

            if(isAlphanumeric(sMacroName))
                sb.append(parameter);
            else
                sb.append('@').append('"').append(parameter.replace("\"", "\"\"")).append('"');
        }
        return sb.toString();
    }

    /**
     * Adds a parameter.
     */
    @NotNull
    public GuiMacroBuilder withParam(@NotNull String value)
    {
        if(value == null)
            throw new NullArgumentException("value");
        return new GuiMacroBuilder(owner, sMacroName, Iterables.concat(parameters, ImmutableList.of(value)));
    }

    /**
     * Adds a parameter.
     */
    @NotNull
    public GuiMacroBuilder withParam(int value)
    {
        return withParam(String.valueOf(value));
    }

    private static boolean isAlphanumeric(@NotNull String s)
    {
        if(s == null)
            throw new NullArgumentException("s");
        for (char ch: s.toCharArray()) {
            if(!Character.isLetterOrDigit(ch) && (ch != '_'))
                return false;
        }
        return true;
    }
}
