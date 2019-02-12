package conemu;

public class GuiMacroResult {
    public boolean isSuccessful;

    public String response;

    @Override
    public String toString() {
        return response + " (" + (isSuccessful ? "Succeeded" : "Failed") + ")";
    }
}
