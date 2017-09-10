package com.example.nancy.fbcognito;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity {
    private Button btnLoginFacebook;

    private Button btnLogoutFacebook;

    private CallbackManager callbackManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CognitoSyncClientManager.init(this);

        btnLoginFacebook = (Button) findViewById(R.id.btnLoginFacebook);
        btnLogoutFacebook = (Button) findViewById(R.id.btnLogoutFacebook);
        btnLogoutFacebook.setVisibility(View.GONE);
     //
        // 4I/yRjHn/A+CIQ82+eKukhcgmkM
        ObtainHashKey();
        FacebookSdk.sdkInitialize(getApplicationContext());

        callbackManager = CallbackManager.Factory.create();
     //   If access token is already here, set fb session
        final AccessToken[] fbAccessToken = {AccessToken.getCurrentAccessToken()};
        if (fbAccessToken[0] != null) {
            setFacebookSession(fbAccessToken[0]);
            btnLoginFacebook.setVisibility(View.GONE);
            btnLogoutFacebook.setVisibility(View.VISIBLE);
        }
        /**
         * Initializes the sync client. This must be call before you can use it.
         */

        btnLoginFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // start Facebook Login
                LoginManager.getInstance().logInWithReadPermissions(MainActivity.this, Arrays.asList("email","public_profile"));

                LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        fbAccessToken[0] =loginResult.getAccessToken();
                       new GetFbName(loginResult).execute();
                       setFacebookSession(loginResult.getAccessToken());
                       btnLoginFacebook.setVisibility(View.GONE);
                        btnLogoutFacebook.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(MainActivity.this, "Facebook login cancelled",
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Toast.makeText(MainActivity.this, "Error in Facebook login " +
                                error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        btnLogoutFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("Nancy abc "+ fbAccessToken[0]);
                if (fbAccessToken[0] != null) {

                    LoginManager.getInstance().logOut();
                    fbAccessToken[0] =null;
                }
                btnLoginFacebook
                        .setVisibility(View.VISIBLE);
                // wipe data
                CognitoSyncClientManager.getInstance()
                        .wipeData();
            }
        });
    }


    public void ObtainHashKey(){
        try{
            PackageInfo info=getPackageManager().getPackageInfo("com.example.nancy.fbcognito",PackageManager.GET_SIGNATURES);
            for(Signature sign:info.signatures){
                MessageDigest md=MessageDigest.getInstance("SHA");
                System.out.println("Keyhash"+Base64.encodeToString(md.digest(),Base64.DEFAULT));
            }
        }
        catch(PackageManager.NameNotFoundException e){

        }
        catch(NoSuchAlgorithmException e){

        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void setFacebookSession(AccessToken accessToken) {
        CognitoSyncClientManager.addLogins("graph.facebook.com",
                accessToken.getToken());
    }
    private class GetFbName extends AsyncTask<Void, Void, String> {
        private final LoginResult loginResult;
        private ProgressDialog dialog;

        public GetFbName(LoginResult loginResult) {
            this.loginResult = loginResult;
        }

        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(MainActivity.this, "Wait", "Getting user name");
        }

        @Override
        protected String doInBackground(Void... params) {
            GraphRequest request = GraphRequest.newMeRequest(
                    loginResult.getAccessToken(),
                    new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(
                                JSONObject object,
                                GraphResponse response) {
                            // Application code
                            Log.v("LoginActivity", response.toString());
                        }
                    });
            Bundle parameters = new Bundle();
            parameters.putString("fields", "name");
            request.setParameters(parameters);
            GraphResponse graphResponse = request.executeAndWait();
            try {
                return graphResponse.getJSONObject().getString("name");
            } catch (JSONException e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(String response) {
            dialog.dismiss();
            if (response != null) {
                Toast.makeText(MainActivity.this, "Hello " + response, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "Unable to get user name from Facebook",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

}
