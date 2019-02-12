package conemu.util;

import org.apache.commons.lang.NullArgumentException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandLineBuilder {
    private StringBuilder commandLine = new StringBuilder();
    private boolean quoteHyphens = false;
    private Pattern allowedUnquoted;
    private Pattern definitelyNeedQuotes;

    static private String s_allowedUnquotedRegexNoHyphen =
            "^"                             // Beginning of line
                    + "[a-z\\\\/:0-9\\._+=]*"
            + "$";

    static private String s_definitelyNeedQuotesRegexWithHyphen = "[|><\\s,;\\-\"\"]+";
    // Use if escaping of hyphens is not to take place

    static private String s_allowedUnquotedRegexWithHyphen =
            "^"                             // Beginning of line
                    + "[a-z\\\\/:0-9\\._\\-+=]*"       //  Allow hyphen to be unquoted
            + "$";

    static private String s_definitelyNeedQuotesRegexNoHyphen = "[|><\\s,;\"\"]+";

    public CommandLineBuilder()
    {
    }

    public CommandLineBuilder(boolean quoteHyphensOnCommandLine)
    {
        quoteHyphens = quoteHyphensOnCommandLine;
    }

    @Override
    public String toString() {
        return commandLine.toString();
    }

    public int getLength() {
        return commandLine.length();
    }

    public StringBuilder getCommandLine() {
        return this.commandLine;
    }

    private Pattern getDefinitelyNeedQuotes() {
        if (null == definitelyNeedQuotes)
        {
            if(quoteHyphens)
            {
                definitelyNeedQuotes = Pattern.compile(s_definitelyNeedQuotesRegexWithHyphen);
            }
            else
            {
                definitelyNeedQuotes = Pattern.compile(s_definitelyNeedQuotesRegexNoHyphen);
            }
        }
        return definitelyNeedQuotes;
    }

    private Pattern getAllowedUnqoted()
    {
        if (null == allowedUnquoted)
        {
            allowedUnquoted = Pattern.compile(s_allowedUnquotedRegexNoHyphen);
        }
        else
        {
            allowedUnquoted = Pattern.compile(s_allowedUnquotedRegexWithHyphen);
        }
        return allowedUnquoted;
    }

    private boolean isQuotingRequired(String parameter)
    {
        boolean isQuotingRequired = false;

        if(parameter != null)
        {
            Pattern allowedUnqoted = getAllowedUnqoted();
            Matcher allowedUnquotedMatcher = allowedUnqoted.matcher(parameter);
            boolean hasAllUnquotedCharacters = allowedUnquotedMatcher.matches();

            Pattern definitelyNeedQuotes = getDefinitelyNeedQuotes();
            Matcher matcher = definitelyNeedQuotes.matcher(parameter);
            boolean hasSomeQuotedCharacters = matcher.matches();

            isQuotingRequired = !hasAllUnquotedCharacters;
            isQuotingRequired = isQuotingRequired || hasSomeQuotedCharacters;
        }

        return isQuotingRequired;
    }

    private void AppendSpaceIfNotEmpty()
    {
        if(commandLine.length() != 0 && commandLine.charAt(commandLine.length() - 1) != ' ')
        {
            commandLine.append(" ");
        }
    }

    private void AppendTextWithQuoting(String textToAppend)
    {
        AppendQuotedTextToBuffer(commandLine, textToAppend);
    }

    private void AppendQuotedTextToBuffer(StringBuilder buffer, String unquotedTextToAppend) {
        if(buffer==null) throw new NullArgumentException("buffer");

        if (unquotedTextToAppend != null)
        {
            boolean addQuotes = isQuotingRequired(unquotedTextToAppend);

            if (addQuotes)
            {
                buffer.append('"');
            }

            // Count the number of quotes
            int literalQuotes = 0;
            for (int i = 0; i < unquotedTextToAppend.length(); i++)
            {
                if('"' == unquotedTextToAppend.charAt(i))
                {
                    literalQuotes++;
                }
            }
            if (literalQuotes > 0)
            {
                if(!((literalQuotes % 2) == 0)) throw new IllegalArgumentException("General.StringsCannotContainOddNumberOfDoubleQuotes (" + unquotedTextToAppend + ")");

                unquotedTextToAppend = unquotedTextToAppend.replace("\\\"", "\\\\\"");
                unquotedTextToAppend = unquotedTextToAppend.replace("\"", "\\\"");
            }

            buffer.append(unquotedTextToAppend);

            if (addQuotes && unquotedTextToAppend.endsWith("\\"))
            {
                buffer.append('\\');
            }

            if (addQuotes)
            {
                buffer.append('"');
            }
        }
    }

    public class ErrorUtilites
    {
    }

    public void AppendTextUnquoted(String textToAppend)
    {
        if (textToAppend != null)
        {
            commandLine.append(textToAppend);
        }
    }

    private void appendFileNameWithQuoting(String fileName)
    {
        if (fileName != null) {
            VerifyThrowNoEmbeddedDoubleQuotes("", fileName);

            if ((fileName.length() != 0) && (fileName.charAt(0) == '-')) {
                AppendTextWithQuoting(".\\" + fileName);
            } else {
                AppendTextWithQuoting(fileName);
            }
        }
    }

    public void appendFileNameIfNotNull(String fileName)
    {
        if (fileName != null)
        {
            VerifyThrowNoEmbeddedDoubleQuotes("", fileName);

            AppendSpaceIfNotEmpty();
            appendFileNameWithQuoting(fileName);
        }
    }

    public void appendFileNamesIfNotNull(String[] fileNames, String delimiter)
    {
        if(delimiter == null)
            throw new NullArgumentException("delimiter");

        if ((fileNames != null) && (fileNames.length > 0))
        {
            for (int i = 0; i < fileNames.length; ++i)
            {
                VerifyThrowNoEmbeddedDoubleQuotes("", fileNames[i]);
            }

            AppendSpaceIfNotEmpty();
            for(int i = 0; i < fileNames.length; i++)
            {
                if(i != 0)
                {
                    AppendTextUnquoted(delimiter);
                }

                appendFileNameWithQuoting(fileNames[i]);
            }
        }
    }

    public void appendSwitch(String switchName)
    {
        if(switchName == null) throw new NullArgumentException("switchName");

        AppendSpaceIfNotEmpty();
        AppendTextUnquoted(switchName);
    }

    public void appendSwitchIfNotNull(String switchName, String parameter)
    {
        if(switchName == null) throw new NullArgumentException("switchName");

        if(parameter != null)
        {
            appendSwitch(switchName);
            AppendTextWithQuoting(parameter);
        }
    }

    private void VerifyThrowNoEmbeddedDoubleQuotes(String parameter, String switchName) {
        if (parameter != null)
        {
            if (switchName == null || switchName.isEmpty())
            {
                if(-1 != parameter.indexOf('"')) throw new IllegalArgumentException("General.QuotesNotAllowedInThisKindOfTaskParameterNoSwitchName (" + parameter + ")");
            }
            else
            {
                if(-1 != parameter.indexOf('"'))
throw new IllegalArgumentException("General.QuotesNotAllowedInThisKindOfTaskParameter (" + parameter + ")");
            }
        }
    }

    public void appendSwitchIfNotNull(String switchName, String[] parameters, String delimiter)
    {
        if(switchName == null) throw new NullArgumentException("switchName");
        if(delimiter == null) throw new NullArgumentException("delimiter");

        if ((parameters != null) && (parameters.length > 0))
        {
            appendSwitch(switchName);
            boolean first = true;
            for (String parameter :
                    parameters) {
                if (!first)
                {
                    AppendTextUnquoted(delimiter);
                }
                first = false;
                AppendTextWithQuoting(parameter);
            }
        }
    }

    public void AppendSwitchesUnqotedIfNotNull(String switchName, String parameter)
    {
        if(switchName == null) throw new NullArgumentException("switchName");

        if (parameter != null)
        {
            appendSwitch(switchName);
            AppendTextUnquoted(parameter);
        }
    }

    public void AppendSwitchUnquotedIfNotNull(String switchName, String[] parameters, String delimiter)
    {
        if(switchName == null) throw new NullArgumentException("switchName");
        if(delimiter == null) throw new NullArgumentException("delimiter");

        if ((parameters != null) && (parameters.length > 0))
        {
            appendSwitch(switchName);
            boolean first = true;

            for (String parameter :
                    parameters) {
                if (!first)
                {
                    AppendTextUnquoted(delimiter);
                }
                first = false;
                AppendTextUnquoted(parameter);
            }
        }
    }
}
