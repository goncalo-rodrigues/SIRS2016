package pt.ulisboa.tecnico.sirs.droidcipher.Interfaces;

import pt.ulisboa.tecnico.sirs.droidcipher.Services.Connection;

/**
 * Created by goncalo on 10-11-2016.
 */

public interface IAcceptConnectionCallback {
    void onAcceptConnection(Connection toBeAccepted);
    void onRejectConnection(Connection toBeRejected);
}
