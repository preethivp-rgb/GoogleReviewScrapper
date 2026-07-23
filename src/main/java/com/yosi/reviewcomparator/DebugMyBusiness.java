package com.yosi.reviewcomparator;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.yosi.reviewcomparator.google.GoogleOAuthHelper;

public class DebugMyBusiness {
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        Credential credential = new GoogleOAuthHelper(config).authorize();
        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory(credential);

        System.out.println("=== Step 1: accounts ===");
        try {
            HttpResponse r1 = requestFactory.buildGetRequest(
                    new GenericUrl("https://mybusinessaccountmanagement.googleapis.com/v1/accounts")).execute();
            System.out.println(r1.parseAsString());
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getMessage());
        }
    }
}
