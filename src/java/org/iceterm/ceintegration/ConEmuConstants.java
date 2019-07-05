package org.iceterm.ceintegration;

public class ConEmuConstants {
    public static String ConEmuConsoleExtenderExeName = "ConEmuC";

    public static String ConEmuConsoleServerFileNameNoExt = "ConEmuCD";

    public static String ConEmuExeName = "ConEmu";

    public static String ConEmuSubfolderName = "ConEmu";

    public static String DefaultConsoleCommandLine = "{cmd}";

    public static String XmlAttrName = "name";

    public static String XmlElementKey = "key";

    public static String XmlValueConEmu = "ConEmu";

    public static String XmlValueDotVanilla = ".Vanilla";

    public static String XmlValueSoftware = "Software";

    static {
        if (System.getProperty("sun.arch.data.model").equals("64")) {
            ConEmuConstants.ConEmuConsoleExtenderExeName += "64";
            ConEmuConstants.ConEmuConsoleServerFileNameNoExt += "64";
            ConEmuConstants.ConEmuExeName += "64";
        }
        ConEmuConstants.ConEmuExeName += ".exe";
        ConEmuConstants.ConEmuConsoleExtenderExeName += ".exe";
    }
}
