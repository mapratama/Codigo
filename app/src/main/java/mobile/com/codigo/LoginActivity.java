package mobile.com.codigo;

import android.*;
import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountAuthenticatorActivity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import mobile.com.codigo.core.Alert;
import mobile.com.codigo.core.LoadingDialog;
import mobile.com.codigo.services.AuthenticatorService;

import static mobile.com.codigo.services.AuthenticatorService.KEY_ERROR_MESSAGE;
import static mobile.com.codigo.services.AuthenticatorService.PARAM_USER_PASS;
import static mobile.com.codigo.services.AuthenticatorService.sServerAuthenticate;

public class LoginActivity extends AccountAuthenticatorActivity {

    @BindView(R.id.email) EditText emailEditText;
    @BindView(R.id.password) EditText passwordEditText;
    @BindView(R.id.name) EditText nameEditText;
    @BindView(R.id.confirm_password) EditText confirmPasswordEditText;
    @BindView(R.id.submit) Button submitButton;
    @BindView(R.id.checkbox) CheckBox loginModeCheckBox;
    @BindView(R.id.name_layout) TextInputLayout nameLayout;
    @BindView(R.id.confirm_password_layout) TextInputLayout confirmPasswordLayout;

    private AccountManager accountManager;
    private String authTokenType, accountType;
    private final int GET_ACCOUNTS_PERMISSIONS_REQUEST = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        loginModeCheckBox.setChecked(true);
        accountManager = AccountManager.get(this);

        authTokenType = AuthenticatorService.AUTHTOKEN_TYPE_FULL_ACCESS;
        accountType = AuthenticatorService.ACCOUNT_TYPE;

        isAuthenticated();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) isAuthenticated();
        else Alert.alertDialog(this, getResources().getString(R.string.ignore_accounts_permission));
    }

    @OnCheckedChanged(R.id.checkbox)
    public void checkboxOnCheckedChanged(boolean isChecked) {
        if (isChecked)  {
            nameLayout.setVisibility(View.GONE);
            confirmPasswordLayout.setVisibility(View.GONE);
            submitButton.setText(getResources().getString(R.string.login));
        }
        else {
            nameLayout.setVisibility(View.VISIBLE);
            confirmPasswordLayout.setVisibility(View.VISIBLE);
            submitButton.setText(getResources().getString(R.string.signup));
        }

        setupButtonStyle();
    }

    @OnTextChanged(R.id.email)
    public void emailEditTextOnTextChanged() {
        setupButtonStyle();
    }

    @OnTextChanged(R.id.name)
    public void nameEditTextOnTextChanged() {
        setupButtonStyle();
    }

    @OnTextChanged(R.id.password)
    public void passwordEditTextOnTextChanged() {
        setupButtonStyle();
    }

    @OnTextChanged(R.id.confirm_password)
    public void confirmPasswordEditTextOnTextChanged() {
        setupButtonStyle();
    }

    @OnClick(R.id.submit)
    public void submitButtonOnClick() {
        final String email = emailEditText.getText().toString();
        final String name = nameEditText.getText().toString();
        final String password = passwordEditText.getText().toString();
        final boolean loginMode = loginModeCheckBox.isChecked();

        if (!email.contains("@")) {
            Alert.alertDialog(this, "Please fill your valid email");
            return;
        }
        if (password.length() < 6) {
            Alert.alertDialog(this, "Minimum password is 6 character");
            return;
        }

        if (!loginMode) {
            if (name.length() < 3) {
                Alert.alertDialog(this, "Minimum name is 3 character");
                return;
            }
            if (!password.equals(confirmPasswordEditText.getText().toString())) {
                Alert.alertDialog(this, "Password is mismatch");
                return;
            }
        }

        final Dialog loadingDialog = LoadingDialog.build(this);
        new AsyncTask<String, Void, Intent>() {

            @Override
            protected void onPreExecute() {
                loadingDialog.show();
            }

            @Override
            protected Intent doInBackground(String... params) {
                String authtoken;
                Bundle data = new Bundle();
                try {
                    if (loginMode)
                        authtoken = sServerAuthenticate.userSignIn(email, password, authTokenType);
                    else
                        authtoken = sServerAuthenticate.userSignUp(email, name, password, authTokenType);

                    data.putString(AccountManager.KEY_ACCOUNT_NAME, name);
                    data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
                    data.putString(AccountManager.KEY_AUTHTOKEN, authtoken);
                    data.putString(PARAM_USER_PASS, password);
                } catch (Exception e) {
                    data.putString(KEY_ERROR_MESSAGE, e.getMessage());
                }

                final Intent intent = new Intent();
                intent.putExtras(data);
                return intent;
            }

            @Override
            protected void onPostExecute(Intent intent) {
                loadingDialog.dismiss();

                if (intent.hasExtra(KEY_ERROR_MESSAGE)) {
                    Toast.makeText(getBaseContext(), intent.getStringExtra(KEY_ERROR_MESSAGE), Toast.LENGTH_SHORT).show();
                    return;
                }

                String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                String accountPassword = intent.getStringExtra(PARAM_USER_PASS);
                String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
                Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));

                accountManager.addAccountExplicitly(account, accountPassword, null);
                accountManager.setAuthToken(account, authTokenType, authtoken);

                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            }
        }.execute();
    }

    private void setupButtonStyle() {
        String password = passwordEditText.getText().toString();
        if (emailEditText.getText().toString().contains("@") && password.length() > 5) {
            if (!loginModeCheckBox.isChecked()) {
                if (nameEditText.getText().toString().length() > 2 && confirmPasswordEditText.getText().toString().equals(password))
                    submitButton.setBackground(ContextCompat.getDrawable(this, R.drawable.orange_button));
                else
                    submitButton.setBackground(ContextCompat.getDrawable(this, R.drawable.gray_button));
            }
            else
                submitButton.setBackground(ContextCompat.getDrawable(this, R.drawable.orange_button));
        }
        else
            submitButton.setBackground(ContextCompat.getDrawable(this, R.drawable.gray_button));
    }

    private void isAuthenticated() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.GET_ACCOUNTS}, GET_ACCOUNTS_PERMISSIONS_REQUEST);

        Account[] accountsByType = accountManager.getAccountsByType(accountType);
        if (accountsByType.length >= 1) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}
