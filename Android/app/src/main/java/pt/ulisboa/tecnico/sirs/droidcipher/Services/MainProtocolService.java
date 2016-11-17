package pt.ulisboa.tecnico.sirs.droidcipher.Services;

import android.app.IntentService;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.hardware.camera2.params.BlackLevelPattern;
import android.inputmethodservice.Keyboard;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.util.Arrays;

import javax.crypto.spec.SecretKeySpec;

import pt.ulisboa.tecnico.sirs.droidcipher.Constants;

import pt.ulisboa.tecnico.sirs.droidcipher.Constants;
import pt.ulisboa.tecnico.sirs.droidcipher.Helpers.Asserter;
import pt.ulisboa.tecnico.sirs.droidcipher.Helpers.CipherHelper;
import pt.ulisboa.tecnico.sirs.droidcipher.Helpers.KeyGenHelper;
import pt.ulisboa.tecnico.sirs.droidcipher.Helpers.NotificationsHelper;
import pt.ulisboa.tecnico.sirs.droidcipher.Interfaces.IAcceptConnectionCallback;
import pt.ulisboa.tecnico.sirs.droidcipher.ServerThread;

/**
 * Created by goncalo on 10-11-2016.
 */

public class MainProtocolService extends Service implements IAcceptConnectionCallback {
    private static final String LOG_TAG = MainProtocolService.class.getSimpleName();
    private final IBinder mBinder = new LocalBinder();
    private SecretKeySpec commKey = null;
    private byte[] commIV = null;

    private SecretKeySpec newCommKey = null;
    private byte[] newCommIV = null;

    private PrivateKey privateKey = null;
    private ServerThread serverThread;
    private boolean accepted = false;
    public MainProtocolService() {
        super();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "Created");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "Started");
        if (serverThread == null) {
            serverThread = new ServerThread(this);
            serverThread.start();
        }

        if (!KeyGenHelper.isKeyPairStored(this)) {
            KeyGenHelper.generateNewKeyPair(this);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public byte[] onNewConnection(byte[] message, BluetoothDevice device) {
        accepted = false;
        if (privateKey == null) {
            privateKey = KeyGenHelper.getPrivateKey(this);
        }
        byte[] decrypted = null;
        try {
            decrypted = CipherHelper.RSADecrypt(privateKey, message);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            Log.e(LOG_TAG, "Invalid private key");
        }

        byte[] nonce = Arrays.copyOfRange(decrypted, 0, 4);
        byte[] decryptedIV = Arrays.copyOfRange(decrypted, 4, 20);
        byte[] decryptedKey = Arrays.copyOfRange(decrypted, 20, decrypted.length);

        if (Asserter.AssertAESKey(decryptedKey)) {
            newCommKey = new SecretKeySpec(decryptedKey, Constants.SYMMETRIC_CIPHER_ALGORITHM);
            newCommIV = decryptedIV;
        } else {
            Log.e(LOG_TAG, "Malformed communication key");
        }

        NotificationsHelper.startNewConnectionNotification(this, device);
        synchronized(this) {
            try {
                // Calling wait() will block this thread until another thread
                // calls notify() on the object.
                this.wait();
            } catch (InterruptedException e) {
                // Happens if someone interrupts your thread.
            }
        }
        if (accepted) {
            return nonce;
        } else {
            return null;
        }
    }
    public byte[] onNewMessage(String messageType, byte [] message) {
         if (messageType.equals(Constants.MESSAGE_TYPE_FILEKEY)) {

            // loading to memory
            if (commKey == null) {
                commKey = KeyGenHelper.getLastCommunicationKey(this);
            }
            if (commIV == null) {
                commIV = KeyGenHelper.getLastCommunicationIV(this);
            }
            if (privateKey == null) {
                privateKey = KeyGenHelper.getPrivateKey(this);
            }

            Log.d(LOG_TAG, "IV:" + Base64.encodeToString(commIV, Base64.DEFAULT));
            boolean accepted = commKey != null && commIV != null && privateKey != null;
            if (!accepted) {
                Log.d(LOG_TAG, "Trying to communicate with a rejected session. Ignoring.");
                return null;
            }
            try {
                byte[] decryptedMessage = CipherHelper.AESDecrypt(commKey, commIV, message);
                byte[] fileKey = CipherHelper.RSADecrypt(privateKey, decryptedMessage);
                byte[] encryptedFileKey = CipherHelper.AESEncrypt(commKey, commIV, fileKey);

                return encryptedFileKey;
            } catch (InvalidKeyException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Invalid key!");
            }

        }
        else {
             Log.e(LOG_TAG, "Unknown message type");
         }
        return null;
    }

    public void OnStop() {
        stopSelf();
    }
    @Override
    public void onDestroy() {
        serverThread.cancel();
    }

    @Override
    public void OnAcceptConnection() {
        KeyGenHelper.saveCommuncationKey(this, newCommKey.getEncoded(), newCommIV);
        commKey = newCommKey;
        commIV = newCommIV;
        newCommKey = null;
        newCommIV = null;
        synchronized(this) {
            accepted = true;
            this.notify();
        }
    }

    @Override
    public void OnRejectConnection() {
        newCommKey = null;
        newCommIV = null;
        Log.d(LOG_TAG, "Connection rejected by user.");
        synchronized(this) {
            accepted = false;
            this.notify();
        }
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public MainProtocolService getService() {
            // Return this instance so clients can call public methods
            return MainProtocolService.this;
        }
    }
}
