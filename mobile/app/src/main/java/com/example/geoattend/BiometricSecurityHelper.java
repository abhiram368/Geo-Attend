package com.example.geoattend;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.ECGenParameterSpec;
import java.util.concurrent.Executor;

public class BiometricSecurityHelper {

    private static final String TAG = "BiometricSecurityHelper";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "com.example.geoattend.biometric_key_alias";

    public static boolean isBiometricAvailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        int canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static boolean isKeyGenerated() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            return keyStore.containsAlias(KEY_ALIAS);
        } catch (Exception e) {
            Log.e(TAG, "Error checking key presence: " + e.getMessage());
            return false;
        }
    }

    public static void generateKeyPair() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER);

        KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN)
                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true); // Invalidates private key if user registers a new face/fingerprint

        keyPairGenerator.initialize(builder.build());
        keyPairGenerator.generateKeyPair();
        Log.d(TAG, "Key pair generated successfully in AndroidKeyStore.");
    }

    public static String getPublicKeyBase64() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            
            PublicKey publicKey = keyStore.getCertificate(KEY_ALIAS).getPublicKey();
            byte[] encoded = publicKey.getEncoded();
            return Base64.encodeToString(encoded, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get public key base64: " + e.getMessage());
            return null;
        }
    }

    public static Signature getSignatureInstance() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initSign(privateKey);
            return signature;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize signature with private key: " + e.getMessage());
            return null;
        }
    }

    public static void deleteKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS);
                Log.d(TAG, "Biometric key deleted from KeyStore.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to delete key: " + e.getMessage());
        }
    }

    public static void showBiometricPrompt(
            @NonNull Fragment fragment,
            @NonNull Signature signature,
            @NonNull BiometricPrompt.AuthenticationCallback callback) {
        
        Executor executor = ContextCompat.getMainExecutor(fragment.requireContext());
        BiometricPrompt biometricPrompt = new BiometricPrompt(fragment, executor, callback);

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric Verification")
                .setSubtitle("Confirm attendance using your biometrics")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();

        biometricPrompt.authenticate(promptInfo, new BiometricPrompt.CryptoObject(signature));
    }
}
