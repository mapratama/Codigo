package mobile.com.codigo.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


public class AuthenticatorService extends Service {

    public static final String ACCOUNT_TYPE = "mobile.com.codigo";
    public static final String AUTHTOKEN_TYPE_READ_ONLY = "Read only";
    public static final String AUTHTOKEN_TYPE_READ_ONLY_LABEL = "Read only access to one account";
    public static final String AUTHTOKEN_TYPE_FULL_ACCESS = "Full access";
    public static final String AUTHTOKEN_TYPE_FULL_ACCESS_LABEL = "Full access to one account";

    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";
    public static final String KEY_ERROR_MESSAGE = "ERR_MSG";
    public final static String PARAM_USER_PASS = "USER_PASS";

    public static final ServerAuthenticate sServerAuthenticate = new ParseComServerAuthenticate();

    @Override
    public IBinder onBind(Intent intent) {
        Authenticator authenticator = new Authenticator(this);
        return authenticator.getIBinder();
    }

    public interface ServerAuthenticate {
        public String userSignUp(final String name, final String email, final String pass, String authType) throws Exception;
        public String userSignIn(final String user, final String pass, String authType) throws Exception;
    }
}
