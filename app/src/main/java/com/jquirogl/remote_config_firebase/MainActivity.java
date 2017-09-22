package com.jquirogl.remote_config_firebase;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    /*
    private static final String NAME_PARAMETER_MSG_LENGTH_ = "msg_length_example" ;
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 0;

    private EditText mIdentificacion;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;*/


    //Variables para la configuracion en RemoteConfig
    private static final String CONFIG_SIGNUP_PROMPT = "signup_prompt";             //mensaje de bienvenida
    private static final String CONFIG_MIN_PASSWORD_LEN = "min_password_length";    //tamaño minimo de la contraseña
    private static final String CONFIG_IS_PROMO_ON = "is_promotion_on";             //activar promoción
    private static final String CONFIG_COLOR_PRY = "color_primary";                 //color primario
    private static final String CONFIG_COLOR_PRY_DARK = "color_primary_dark";       //color primario oscuro

    private Toolbar toolbar;
    private EditText mEditUsername;
    private EditText mEditEmail;
    private EditText mEditPassword;
    private EditText mEditPromoCode;
    private TextView mTextPrompt;
    private Button mButtonSignup;

    //definimos el tamaño minimo de la contraseña
    private int minPasswordLength;

    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        instaceValues();
        firebaseRemoteConfig();
        updateMaxTextLength();*/

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        mEditUsername = (EditText) findViewById(R.id.edit_username);
        mEditEmail = (EditText) findViewById(R.id.edit_email);
        mEditPassword = (EditText) findViewById(R.id.edit_password);
        mTextPrompt = (TextView) findViewById(R.id.textview_signup_prompt);
        mEditPromoCode = (EditText) findViewById(R.id.edit_promo_code);
        mButtonSignup = (Button) findViewById(R.id.button_signup);

        setSupportActionBar(toolbar);

        initRemoteConfig();

        setupView();


    }
    //configuramos Firebase RemoteConfig
    private void initRemoteConfig() {
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        Resources res = getResources();

        HashMap<String,Object> defaults = new HashMap<>();

        //ejemplo: Nombre del parámetro y el valor limite para ingresar datos en el editText

        defaults.put(CONFIG_SIGNUP_PROMPT, getString(R.string.config_signup_prompt));
        defaults.put(CONFIG_MIN_PASSWORD_LEN, res.getInteger(R.integer.config_min_password_len));
        defaults.put(CONFIG_IS_PROMO_ON, res.getBoolean(R.bool.config_promo_on));
        defaults.put(CONFIG_COLOR_PRY, res.getString(R.string.config_color_pry));
        defaults.put(CONFIG_COLOR_PRY_DARK, res.getString(R.string.config_color_pry_dark));

        mFirebaseRemoteConfig.setDefaults(defaults);

        FirebaseRemoteConfigSettings remoteConfigSettings = new FirebaseRemoteConfigSettings.Builder()
                                                                .setDeveloperModeEnabled(true)
                                                                .build();

        mFirebaseRemoteConfig.setConfigSettings(remoteConfigSettings);
        fetchRemoteConfigValues();

    }
    //configuración de la vista
    private void setupView() {

        setToolbarColor();
        setStatusBarColor();
        setSingupPrompt();
        setPromoCode();

        minPasswordLength = (int) mFirebaseRemoteConfig.getLong(CONFIG_MIN_PASSWORD_LEN);

        mButtonSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validateInput()) {
                    Toast.makeText(getApplicationContext(), R.string.toast_sign_up,Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean validateInput() {
        // realizamos la validacion para los otros campos

        //configuramos la caja para ingresar nuestra contraseña
        if (mEditPassword.getText().toString().length() < minPasswordLength) {
            mEditPassword.setError(String.format(getString(R.string.error_short_password), minPasswordLength));
            return false;
        } else {
            mEditPassword.setError(null);
            return true;
        }
    }

    private void fetchRemoteConfigValues() {
        long cacheExpiration = 3600;

        //para el modo desarrollador expira inmediatamente
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }

        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) { //TODO Revisar este NonNull en el ejm no está
                if (task.isSuccessful()) {
                    //tarea exitosa. Activar los datos obtenidos
                    mFirebaseRemoteConfig.activateFetched();
                    setupView();
                } else {
                    Log.e("Firebase-Error", "Error en el Task de Firebase");
                }
            }
        });
    }

    //Establece la solicitud de registro
    private void setSingupPrompt(){
        String pront = mFirebaseRemoteConfig.getString(CONFIG_SIGNUP_PROMPT);
        if(pront !=null){
            mTextPrompt.setText(pront);
        }
    }

    //Establece u oculta el campo del código promocional en función de si la promoción está activada o no
    private void setPromoCode() {
        boolean isPromoOn = mFirebaseRemoteConfig.getBoolean(CONFIG_IS_PROMO_ON);
        mEditPromoCode.setVisibility(isPromoOn ? View.VISIBLE : View.GONE);
    }

    //Establece el valor del color de la barra de herramientas.
    private void setToolbarColor() {
        boolean isPromoOn = mFirebaseRemoteConfig.getBoolean(CONFIG_IS_PROMO_ON);
        int color = isPromoOn ? Color.parseColor(mFirebaseRemoteConfig.getString(CONFIG_COLOR_PRY)) :
                ContextCompat.getColor(this, R.color.colorPrimary);

        toolbar.setBackgroundColor(color);
    }
    //Establece el color de la barra de estado
    private void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            boolean isPromoOn = mFirebaseRemoteConfig.getBoolean(CONFIG_IS_PROMO_ON);
            int color = isPromoOn ? Color.parseColor(mFirebaseRemoteConfig.getString(CONFIG_COLOR_PRY_DARK)) :
                    ContextCompat.getColor(this, R.color.colorPrimaryDark);

            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(color);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflar el menú; esto agrega elementos a la barra de acción si está presente.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        /*
         * El elemento de la barra de acción de la manija hace clic aquí.
         * La barra de acción controlará automáticamente los clics en el botón Inicio / Arriba,
         * siempre y cuando especifique una actividad principal en AndroidManifest.xml.**/
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }





    /*
    private void instaceValues(){
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        mIdentificacion = (EditText)findViewById(R.id.etIdentificacion);
    }
    private void updateMaxTextLength() {
        int max = (int) mFirebaseRemoteConfig.getLong(NAME_PARAMETER_MSG_LENGTH_);
        mIdentificacion.setFilters(new InputFilter[]{new InputFilter.LengthFilter(max)});
    }

    private void firebaseRemoteConfig(){
        //configuramos RemoteConfig, activamos el modo developer
        mFirebaseRemoteConfig.setConfigSettings(new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(true)
                .build());


        HashMap<String, Object> defaults = new HashMap<>();
        defaults.put(NAME_PARAMETER_MSG_LENGTH_,DEFAULT_MSG_LENGTH_LIMIT);
        mFirebaseRemoteConfig.setDefaults(defaults);

        final Task<Void> fetch = mFirebaseRemoteConfig.fetch(0);
        fetch.addOnSuccessListener(this, new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                mFirebaseRemoteConfig.activateFetched();
                updateMaxTextLength();
            }
        });
    }*/
}
