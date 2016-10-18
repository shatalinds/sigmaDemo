package ru.sigmacall.sigma;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.sigmacall.sigma.tools.BtnCallBack;
import ru.sigmacall.sigma.tools.RefRequest;
import ru.sigmacall.sigma.tools.ServerCallback;

public class SharingActivity extends AppCompatActivity {
    AppConf conf;
    android.support.v7.widget.Toolbar toolbar;

    EditText edSharingCode;
    SharingActivity activity;

    ImageView ivShareCode;
    ImageView ivShareStatus;

    Button btnUseBonuses;
    Button btnComeon;

    float currentBonus;

    TextView tvShareCode;
    TextView tvSignup;
    TextView tvEarn;
    TextView tvBonus;
    TextView tvStatusValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sharing);

        conf = new AppConf(this);

        activity = this;
        toolbar = (android.support.v7.widget.Toolbar)
                findViewById(R.id.toolbar_sharing);
        try {
            setSupportActionBar(toolbar);
        } catch (Throwable t) {
            // We got Samsung 4.2.2
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(true);
        toolbar.setNavigationIcon(R.drawable.ic_back);

        tvStatusValue = (TextView) findViewById(R.id.tv_status_value);
        tvStatusValue.setText(getString(R.string.ui_status_beginer));

        tvSignup = (TextView) findViewById(R.id.tv_signup);
        tvEarn = (TextView) findViewById(R.id.tv_earn);
        tvBonus = (TextView) findViewById(R.id.tv_bonus);
        tvShareCode = (TextView) findViewById(R.id.tvShareCode);

        btnComeon = (Button) findViewById(R.id.btn_comeon);
        btnComeon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mySharingCode) {
                    AppConf.alertDialog(activity, activity.getString(R.string.ui_comeon),
                            activity.getString(R.string.ui_comeon_text),
                            R.drawable.ic_info, null, 0,
                            activity.getString(R.string.ui_continue), new BtnCallBack() {
                                @Override
                                public void onBtnClick() {
                                    inventCode();
                                }
                            }, activity.getString(R.string.ui_cancel), null, null, null);
                } else {
                    AppConf.alertDialog(activity, activity.getString(R.string.ui_information),
                            activity.getString(R.string.ui_code_already_created),
                            R.drawable.ic_info, null, 0,
                            activity.getString(R.string.ui_ok),
                            null, null, null, null, null);
                }
            }
        });

        edSharingCode = (EditText)findViewById(R.id.ed_sharing_code);
        edSharingCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        String sharedCode = conf.getSharedCode();
        if (sharedCode != null) {
            edSharingCode.setText(sharedCode);
        }

        edSharingCode.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (event != null&& (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    in.hideSoftInputFromWindow(edSharingCode.getApplicationWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
                }
                return false;
            }
        });


        ivShareCode = (ImageView)findViewById(R.id.ivShareCode);
        ivShareStatus = (ImageView)findViewById(R.id.ivShareStatus);

        Drawable myIcon = getResources().getDrawable(R.drawable.abc_ic_menu_share_mtrl_alpha);
        myIcon.setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP);

        ((ImageView)findViewById(R.id.ivShareCode)).setImageDrawable(myIcon);
        ((ImageView)findViewById(R.id.ivShareStatus)).setImageDrawable(myIcon);

        View.OnClickListener listenerShareCode = new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                final EditText inputText = new EditText(SharingActivity.this);
                inputText.setInputType(InputType.TYPE_CLASS_TEXT);

                AppConf.alertDialog(activity, activity.getString(R.string.ui_information),
                        activity.getString(R.string.ui_shared_text),
                        R.drawable.ic_contacts,
                        inputText, 0, activity.getString(R.string.ui_ok), new BtnCallBack() {
                            @Override
                            public void onBtnClick() {
                                final String userText = inputText.getText().toString();
                                final String promoCode = edSharingCode.getText().toString();
                                String sharingText = String.format(getString(R.string.ui_shared_code),
                                        userText, promoCode);
                                AppConf.sharingInActivity(SharingActivity.this, sharingText);
                            }
                        }, activity.getString(R.string.ui_cancel), null, null, null);
            }
        };

        tvShareCode.setOnClickListener(listenerShareCode);
        ivShareCode.setOnClickListener(listenerShareCode);

        ivShareStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String promoCode = edSharingCode.getText().toString();
                final String sharedStatus = tvStatusValue.getText().toString();
                String sharedText = String.format(getString(R.string.ui_shared_status),
                        sharedStatus, promoCode);
                AppConf.sharingInActivity(SharingActivity.this, sharedText);
            }
        });

        btnUseBonuses = (Button) findViewById(R.id.btn_use_bonuses);
        btnUseBonuses.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                AppConf.alertDialog(activity, activity.getString(R.string.ui_use_bonuses),
                        formatFloat(String.format(activity.getString(R.string.ui_bonus_text), currentBonus)),
                        R.drawable.ic_contacts, null, 12,
                        activity.getString(R.string.ui_balans_trans), new BtnCallBack() {
                            @Override
                            public void onBtnClick() {
                                if (checkBalance(false)) {
                                    bonusTransfer();
                                }
                            }
                        }, activity.getString(R.string.ui_request_cash), new BtnCallBack() {
                            @Override
                            public void onBtnClick() {
                                if (checkBalance(true)) {
                                    requestCash();
                                }
                            }
                        }, activity.getString(R.string.ui_cancel), null);
            }
        });

        updateBonusInfo();
    }

    private void updateBonusInfo() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                currentBonus();
            }
        });
    }


    /**
     * Проверка баланса при переводе бонусов на счет
     * @return
     */
    private boolean checkBalance(boolean lessH) {
        boolean ret = true;

        String message = null;

        if (currentBonus == 0) {
            message = activity.getString(R.string.ui_bonus_zero);
        } else if (lessH && currentBonus < 100){
            message = activity.getString(R.string.ui_bonus_less_hundred);
        }

        if (message != null) {
            AppConf.alertDialog(activity, activity.getString(R.string.ui_warning),
                message, R.drawable.ic_contacts, null, 0,
                activity.getString(R.string.ui_ok), null, null, null, null, null);

            ret = false;
        }

        return ret;
    }

    /**
     *
     */
    private void currentBonus() {
        RefRequest ref = new RefRequest(activity);
        AppConf appConf = new AppConf(getApplicationContext());
        final String login = appConf.getSipUser();
        final String sessId = appConf.getSessId();

        final ProgressDialog pd = MainActivity.createAndShowProgressDialog(activity);

        ref.currentBonus(login, sessId, new ServerCallback() {
            @Override
            public void onSuccess(String[] result) {
                try {
                    if (result.length > 0) {
                        if (result[0] != null) {
                            currentBonus = Float.valueOf(result[0]);
                        }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pd.cancel();
                            setSharingInfo();
                        }
                    });
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                    currentBonus = 0.f;
                }
            };

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.cancel();
                        if (message != null) {
                            AppConf.alertDialog(activity, activity.getString(R.string.ui_warning),
                                    message, R.drawable.ic_info, null, 0, activity.getString(R.string.ui_ok),
                                    null, null, null, null, null);
                        }
                        currentBonus();
                    }
                });
            }
        });
    }
    /**
     * Добавляем свой шаринг код
     */
    private void addMySharingCode(String code) {
        RefRequest ref = new RefRequest(activity);
        AppConf appConf = new AppConf(getApplicationContext());
        final String login = appConf.getSipUser();
        final String sessId = appConf.getSessId();

        final ProgressDialog pd = MainActivity.createAndShowProgressDialog(activity);

        ref.addCustomCode(login, sessId, code, new ServerCallback() {
            @Override
            public void onSuccess(String[] result) {
                if (result.length > 0) {
                    if (result[0] != null && result[1] != null) {
                        if (Boolean.valueOf(result[1])) {
                            mySharingCode = true;
                        }
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.cancel();
                    }
                });
            };

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.cancel();
                        if (message != null) {
                            AppConf.alertDialog(activity, activity.getString(R.string.ui_warning),
                                    message, R.drawable.ic_info, null, 0, activity.getString(R.string.ui_ok),
                                    null, null, null, null, null);
                            edSharingCode.setText(referalCode[0]);
                        }
                    }
                });
            }
        });
    }

    /**
     * Запрос и установка кода
     */
    public static boolean mySharingCode = false;

    private final String[] referalCode = new String[2];

    private void setSharingInfo() {
        RefRequest ref = new RefRequest(activity);
        AppConf appConf = new AppConf(getApplicationContext());
        final String login = appConf.getSipUser();
        final String sessId = appConf.getSessId();

        final ProgressDialog pd = MainActivity.createAndShowProgressDialog(activity);

        ref.getInfo(login, sessId, new ServerCallback() {
            @Override
            public void onSuccess(final String[] result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result[0] != null && result[1] !=  null) {
                            referalCode[0] = result[0];
                            referalCode[1] = result[1];

                            if (result[1].length() > 0) {
                                conf.setSharedCode(result[1]); //referralCode2
                                edSharingCode.setText(result[1]);
                                mySharingCode = true;
                            } else {
                                conf.setSharedCode(result[0]);//referralCode
                            }
                        }

                        edSharingCode.setText(conf.getSharedCode());

                        if (result[2] != null) {
                            tvBonus.setText(result[2]);//bonusAmount
                        } else {
                            tvBonus.setText("0.0");
                        }

                        if (result[4] != null) {//bonusAllTime
                            tvEarn.setText(result[4]);
                        } else {
                            tvEarn.setText(String.valueOf(currentBonus));
                        }


                        if (result[3] != null) {//referralsCount
                            tvSignup.setText(result[3]);
                            final int caseStatus = Integer.valueOf(result[3]);

                            if (caseStatus < 3) {
                                tvStatusValue.setText(getString(R.string.ui_status_beginer));
                            }
                            if (caseStatus >= 3 && caseStatus < 10) {
                                tvStatusValue.setText(getString(R.string.ui_status_agent));
                            }
                            if (caseStatus >= 10 && caseStatus < 25) {
                                tvStatusValue.setText(getString(R.string.ui_status_buisness));
                            }
                            if (caseStatus >= 25) {
                                tvStatusValue.setText(getString(R.string.ui_status_boss));
                            }
                        } else {
                            tvSignup.setText("0");
                        }

                        pd.cancel();
                        conf.setBalanceNeedUpdate(true);
                    }
                });
            };

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.cancel();
                        if (message != null) {
                            AppConf.alertDialog(activity, activity.getString(R.string.ui_warning),
                                    message, R.drawable.ic_info, null, 0, activity.getString(R.string.ui_ok),
                                    null, null, null, null, null);
                        }

                        final String text = edSharingCode.getText().toString();
                        if (text.length() == 0 || text.equals("")) {
                            setSharingInfo();
                        }
                    }
                });
            }
        });
    }

    private String formatFloat(String str) {
        if (str.indexOf(",")> -1) {
            str = str.replace(",", ".");
        }
        return str;
    }
    /**
     * Запрос перевода бонусных средств
     */
    private void bonusTransfer() {
        String sBonus = formatFloat(String.format(activity.getString(
                R.string.ui_trans_available), currentBonus));

        AppConf.alertDialog(activity, activity.getString(R.string.ui_balans_trans),
                sBonus,
                R.drawable.ic_contacts, null, 10, activity.getString(R.string.ui_trans_yes),
                new BtnCallBack() {//«Да
                    @Override
                    public void onBtnClick() {
                        bonusCash(currentBonus);
                    }
                }, activity.getString(R.string.ui_trans_no),
                new BtnCallBack() {//«Нет, указать сумму
                    @Override
                    public void onBtnClick() {
                        noTransferPart();
                    }
                }, activity.getString(R.string.ui_cancel), //Отмена
                null);
    }

    private void transferTo(String email) {
        RefRequest ref = new RefRequest(activity);
        AppConf appConf = new AppConf(getApplicationContext());
        final String login = appConf.getSipUser();
        final String sessId = appConf.getSessId();

        final ProgressDialog pd = MainActivity.createAndShowProgressDialog(activity);

        ref.transferTo(login, sessId, login, email, currentBonus, new ServerCallback() {
            @Override
            public void onSuccess(String[] result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.cancel();
                    }
                });
                updateBonusInfo();
            };

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.cancel();
                        if (message != null) {
                            AppConf.alertDialog(activity, activity.getString(R.string.ui_warning),
                                    message, R.drawable.ic_info, null, 0, activity.getString(R.string.ui_ok),
                                    null, null, null, null, null);
                        }
                    }
                });
            }
        });
    }

  //Да перевести всю сумму.
    private void transferBonus(final float bon) {
        RefRequest ref = new RefRequest(activity);
        AppConf appConf = new AppConf(getApplicationContext());
        final String login = appConf.getSipUser();
        final String sessId = appConf.getSessId();

        final ProgressDialog pd = MainActivity.createAndShowProgressDialog(activity);

        ref.convertToBalance(login, sessId, bon, new ServerCallback() {
            @Override
            public void onSuccess(String[] result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.cancel();
                    }
                });
                updateBonusInfo();
            };

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pd.cancel();
                        if (message != null) {
                            AppConf.alertDialog(activity, activity.getString(R.string.ui_warning),
                                    message, R.drawable.ic_info, null, 0, activity.getString(R.string.ui_ok),
                                    null, null, null, null, null);
                        }
                    }
                });
                updateBonusInfo();
            }
        });
    }

    private class DecimalDigitsInputFilter implements InputFilter {
        private final int decimalDigits;

        public DecimalDigitsInputFilter(int decimalDigits) {
            this.decimalDigits = decimalDigits;
        }

        @Override
        public CharSequence filter(CharSequence source,
                                   int start,
                                   int end,
                                   Spanned dest,
                                   int dstart,
                                   int dend) {


            int dotPos = -1;
            int len = dest.length();
            for (int i = 0; i < len; i++) {
                char c = dest.charAt(i);
                if (c == '.' || c == ',') {
                    dotPos = i;
                    break;
                }
            }
            if (dotPos >= 0) {
                if (source.equals(".") || source.equals(",")) {
                    return "";
                }
                if (dend <= dotPos) {
                    return null;
                }
                if (len - dotPos > decimalDigits) {
                    return "";
                }
            }

            return null;
        }
    }

    /**
     * Перевод части средств
     */
    private void noTransferPart() {
        final EditText inputText = new EditText(this);
        inputText.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        inputText.setFilters(new InputFilter[] {new DecimalDigitsInputFilter(2)});

        AppConf.alertDialog(activity, activity.getString(R.string.ui_input_cash), null,
                R.drawable.ic_contacts, inputText, 0, activity.getString(R.string.ui_ok),
                new BtnCallBack() {
                    @Override
                    public void onBtnClick() {
                        final String bonus = inputText.getText().toString();
                        final float cash = Float.valueOf(bonus);
                        bonusCash(cash);
                    }
                }, activity.getString(R.string.ui_cancel), null, null, null);
    }

    /**
     * Подтверждение о переводе с бонусного счета
     * @param cash
     */
    private void bonusCash(final float cash) {
        String sCash = formatFloat(String.format(activity.getString(
                R.string.ui_balans_append_confrm), cash));
        if (cash <= currentBonus) {
            AppConf.alertDialog(activity, activity.getString(R.string.ui_confirm),//Подтверждение
                    sCash,//ХХ руб будет переведено с бонусного счёта на баланс SigmaCall
                    R.drawable.ic_contacts, null, 0, activity.getString(R.string.ui_ok),
                    new BtnCallBack() {//Ok
                        @Override
                        public void onBtnClick() {
                            transferBonus(cash);
                        }
                    }, activity.getString(R.string.ui_cancel), null, null, null);
        } else if (cash > currentBonus) {
            AppConf.alertDialog(activity, activity.getString(R.string.ui_warning),//Предупреждение
                    activity.getString(R.string.ui_cash_error),//Введенная сумма больше доступной!
                    R.drawable.ic_contacts, null, 0, activity.getString(R.string.ui_ok),
                    null, null, null, null, null);
        }
    }


    /**
     * Подтверждение
     */
    private void confirmEmail(final String email) {
        AppConf.alertDialog(activity, activity.getString(R.string.ui_confirm),
                activity.getString(R.string.ui_confirm_email),
                R.drawable.ic_contacts, null, 0,
                activity.getString(R.string.ui_ok), new BtnCallBack() {
                    @Override
                    public void onBtnClick() {
                        transferTo(email);
                    }
                }, null, null, null, null);
    }
    /**
     * Запрос Email для связи с
     */
    private void requestCash() {
        final EditText inputText = new EditText(this);
        inputText.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        AppConf.alertDialog(activity, activity.getString(R.string.ui_entr_email),
                activity.getString(R.string.ui_input_email), R.drawable.ic_contacts,
                inputText, 0, activity.getString(R.string.ui_ok), new BtnCallBack() {
                    @Override
                    public void onBtnClick() {
                        String email = inputText.getText().toString();
                        confirmEmail(email);
                    }
                }, activity.getString(R.string.ui_cancel), null, null, null);
    }

    //Придумать код
    private void inventCode() {
        final EditText inputText = new EditText(this);
        inputText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable e) {
                Pattern ps = Pattern.compile("^[a-zA-Z||[0-9]]+$");
                Matcher ms = ps.matcher(e.toString());

                if (!ms.matches()) {
                    String text = e.toString();
                    int len = text.length();

                    if (len > 0) {
                        text = text.substring(0, len - 1);
                        inputText.setText(text);
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        AppConf.alertDialog(activity, activity.getString(R.string.ui_input_code), null,
                R.drawable.ic_contacts, inputText, 0, activity.getString(R.string.ui_ok),
                new BtnCallBack() {
                    @Override
                    public void onBtnClick() {
                        final String code = inputText.getText().toString();
                        activity.edSharingCode.setText(code);
                        addMySharingCode(code);
                    }
                }, activity.getString(R.string.ui_cancel), null, null, null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

}