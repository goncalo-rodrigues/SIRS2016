package pt.ulisboa.tecnico.sirs.droidcipher.Services;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by goncalo on 17-11-2016.
 */

public class ServiceState implements Parcelable {
    private boolean isOn;
    private boolean isWaitingUser;
    private boolean isConnected;
    private Connection currentConnection;
    private Connection incomingConnection;

    public boolean isOn() {
        return isOn;
    }

    public void setOn(boolean on) {
        isOn = on;
    }

    public boolean isWaitingUser() {
        return isWaitingUser;
    }

    public void setWaitingUser(boolean waitingUser) {
        isWaitingUser = waitingUser;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public Connection getCurrentConnection() {
        return currentConnection;
    }

    public void setCurrentConnection(Connection currentConnection) {
        this.currentConnection = currentConnection;
    }

    public Connection getIncomingConnection() {
        return incomingConnection;
    }

    public void setIncomingConnection(Connection incomingConnection) {
        this.incomingConnection = incomingConnection;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.isOn ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isWaitingUser ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isConnected ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.currentConnection, flags);
        dest.writeParcelable(this.incomingConnection, flags);
    }

    public ServiceState() {
    }

    protected ServiceState(Parcel in) {
        this.isOn = in.readByte() != 0;
        this.isWaitingUser = in.readByte() != 0;
        this.isConnected = in.readByte() != 0;
        this.currentConnection = in.readParcelable(Connection.class.getClassLoader());
        this.incomingConnection = in.readParcelable(Connection.class.getClassLoader());
    }

    public static final Creator<ServiceState> CREATOR = new Creator<ServiceState>() {
        @Override
        public ServiceState createFromParcel(Parcel source) {
            return new ServiceState(source);
        }

        @Override
        public ServiceState[] newArray(int size) {
            return new ServiceState[size];
        }
    };
}
