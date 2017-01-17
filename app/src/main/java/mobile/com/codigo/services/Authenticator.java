package mobile.com.codigo.services;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import mobile.com.codigo.LoginActivity;

import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;
import static mobile.com.codigo.services.AuthenticatorService.ARG_ACCOUNT_NAME;
import static mobile.com.codigo.services.AuthenticatorService.ARG_ACCOUNT_TYPE;
import static mobile.com.codigo.services.AuthenticatorService.ARG_AUTH_TYPE;
import static mobile.com.codigo.services.AuthenticatorService.ARG_IS_ADDING_NEW_ACCOUNT;
import static mobile.com.codigo.services.AuthenticatorService.AUTHTOKEN_TYPE_FULL_ACCESS;
import static mobile.com.codigo.services.AuthenticatorService.AUTHTOKEN_TYPE_FULL_ACCESS_LABEL;
import static mobile.com.codigo.services.AuthenticatorService.AUTHTOKEN_TYPE_READ_ONLY;
import static mobile.com.codigo.services.AuthenticatorService.AUTHTOKEN_TYPE_READ_ONLY_LABEL;
import static mobile.com.codigo.services.AuthenticatorService.sServerAuthenticate;


public class Authenticator extends AbstractAccountAuthenticator {

    private final Context context;

    public Authenticator(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.putExtra(ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        if (!authTokenType.equals(AUTHTOKEN_TYPE_READ_ONLY) && !authTokenType.equals(AUTHTOKEN_TYPE_FULL_ACCESS)) {
            Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }

        AccountManager accountManager = AccountManager.get(context);
        String authToken = accountManager.peekAuthToken(account, authTokenType);
        if (TextUtils.isEmpty(authToken)) {
            String password = accountManager.getPassword(account);
            if (password != null) {
                try {
                    authToken = sServerAuthenticate.userSignIn(account.name, password, authTokenType);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (!TextUtils.isEmpty(authToken)) {
            Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }

        Intent intent = new Intent(context, LoginActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(ARG_ACCOUNT_TYPE, account.type);
        intent.putExtra(ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(ARG_ACCOUNT_NAME, account.name);

        Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }


    @Override
    public String getAuthTokenLabel(String authTokenType) {
        if (AUTHTOKEN_TYPE_FULL_ACCESS.equals(authTokenType)) return AUTHTOKEN_TYPE_FULL_ACCESS_LABEL;
        else if (AUTHTOKEN_TYPE_READ_ONLY.equals(authTokenType)) return AUTHTOKEN_TYPE_READ_ONLY_LABEL;
        else return authTokenType + " (Label)";
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        Bundle result = new Bundle();
        result.putBoolean(KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        return null;
    }
}
