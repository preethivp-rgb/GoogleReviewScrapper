package com.yosi.reviewcomparator.google;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.yosi.reviewcomparator.Config;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class GoogleOAuthHelper {

    private static final List<String> SCOPES = Collections.singletonList(
            "https://www.googleapis.com/auth/business.manage");

    private final Config config;

    public GoogleOAuthHelper(Config config) {
        this.config = config;
    }

    public Credential authorize() throws Exception {
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details()
                .setClientId(config.googleOAuthClientId())
                .setClientSecret(config.googleOAuthClientSecret());
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets().setInstalled(details);

        File tokenStoreDir = new File(config.googleOAuthTokenStorePath()).getParentFile();
        if (tokenStoreDir != null) {
            tokenStoreDir.mkdirs();
        }
        FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(
                new File(tokenStoreDir, "tokens"));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(), new JacksonFactory(), clientSecrets, SCOPES)
                .setDataStoreFactory(dataStoreFactory)
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
}
