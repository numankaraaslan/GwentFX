package com.numankaraaslan.gwent;

import com.numankaraaslan.gwent.ui.GwentApp;
import javafx.application.Application;

/**
 * Launcher. Kept separate from the JavaFX Application class so the shaded (fat) jar starts without module-path setup.
 */
public final class Main
{

    private Main()
    {
    }

    public static void main(String[] args)
    {
        Application.launch(GwentApp.class, args);
    }
}
