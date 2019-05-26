package org.iceterm.ceintegration;

public class GuiMacroResult {
    public boolean isSuccessful;

    public String response;

    public GuiMacroResult(boolean isSuccessful, String response) {
        this.isSuccessful = isSuccessful;
        this.response = response;
    }

    public GuiMacroResult(boolean isSuccessful) {
        this.isSuccessful = isSuccessful;
    }

    @Override
    public String toString() {
        return response + " (" + (isSuccessful ? "Succeeded" : "Failed") + ")";
    }
}
