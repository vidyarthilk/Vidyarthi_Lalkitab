package com.vidyarthi.lalkitab

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.vidyarthi.lalkitab.ads.ConsentManager
import com.vidyarthi.lalkitab.auth.LoginActivity
import com.vidyarthi.lalkitab.auth.UserAccountManager
import com.vidyarthi.lalkitab.data.db.AppDatabase
import com.vidyarthi.lalkitab.subscription.SubscriberProfileManager
import com.vidyarthi.lalkitab.subscription.SubscriptionBilling
import com.vidyarthi.lalkitab.subscription.SubscriptionManager
import com.vidyarthi.lalkitab.ui.settings.KundliBackupUiHelper
import com.vidyarthi.lalkitab.utils.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : BaseActivity() {

    private lateinit var backupUi: KundliBackupUiHelper
    private lateinit var tvAccountStatus: TextView
    private lateinit var btnAccountAction: MaterialButton
    private lateinit var tvSubscriptionStatus: TextView
    private lateinit var tvSubscriptionStorage: TextView
    private lateinit var tvChoosePlan: TextView
    private lateinit var rgSubscriptionPlan: RadioGroup
    private lateinit var rbPlanMonthly: RadioButton
    private lateinit var rbPlanQuarterly: RadioButton
    private lateinit var rbPlanYearly: RadioButton
    private lateinit var btnSubscribe: MaterialButton
    private lateinit var btnManageSubscription: MaterialButton
    private lateinit var cardPdfProfile: View
    private lateinit var etProfileName: TextInputEditText
    private lateinit var etProfilePhone: TextInputEditText
    private lateinit var etProfileAddress: TextInputEditText

    private var subscriptionBilling: SubscriptionBilling? = null

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        refreshAccountUi()
        refreshSubscriptionUi()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val root = findViewById<View>(android.R.id.content)
        backupUi = KundliBackupUiHelper(this, root) { }

        findViewById<MaterialButton>(R.id.btnBackup).setOnClickListener { backupUi.startBackup() }
        findViewById<MaterialButton>(R.id.btnRestore).setOnClickListener { backupUi.startRestore() }

        tvAccountStatus = findViewById(R.id.tvAccountStatus)
        btnAccountAction = findViewById(R.id.btnAccountAction)
        btnAccountAction.setOnClickListener {
            if (UserAccountManager.isLoggedIn(this)) {
                UserAccountManager.logout(this)
                refreshAccountUi()
            } else {
                loginLauncher.launch(Intent(this, LoginActivity::class.java))
            }
        }
        refreshAccountUi()

        tvSubscriptionStatus = findViewById(R.id.tvSubscriptionStatus)
        tvSubscriptionStorage = findViewById(R.id.tvSubscriptionStorage)
        tvChoosePlan = findViewById(R.id.tvChoosePlan)
        rgSubscriptionPlan = findViewById(R.id.rgSubscriptionPlan)
        rbPlanMonthly = findViewById(R.id.rbPlanMonthly)
        rbPlanQuarterly = findViewById(R.id.rbPlanQuarterly)
        rbPlanYearly = findViewById(R.id.rbPlanYearly)
        btnSubscribe = findViewById(R.id.btnSubscribe)
        btnManageSubscription = findViewById(R.id.btnManageSubscription)
        cardPdfProfile = findViewById(R.id.cardPdfProfile)
        etProfileName = findViewById(R.id.etProfileName)
        etProfilePhone = findViewById(R.id.etProfilePhone)
        etProfileAddress = findViewById(R.id.etProfileAddress)
        findViewById<MaterialButton>(R.id.btnSaveProfile).setOnClickListener { savePdfProfile() }
        loadPdfProfile()
        btnSubscribe.setOnClickListener { subscriptionBilling?.launchPurchase(selectedBasePlanId()) }
        btnManageSubscription.setOnClickListener { openManageSubscription() }

        subscriptionBilling = SubscriptionBilling(this) {
            refreshSubscriptionUi()
        }.also { billing ->
            billing.setPurchaseMessageHandler { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
            billing.setOnPlansLoaded { bindSubscriptionPlans() }
        }

        findViewById<MaterialButton>(R.id.btnPrivacyPolicy).setOnClickListener {
            openPrivacyPolicy()
        }

        val btnAdPrivacy = findViewById<MaterialButton>(R.id.btnAdPrivacy)
        if (ConsentManager.isPrivacyOptionsRequired(this) ||
            ConsentManager.isGatheringComplete()
        ) {
            btnAdPrivacy.visibility = View.VISIBLE
            btnAdPrivacy.setOnClickListener {
                ConsentManager.showPrivacyOptions(this)
            }
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.settingsToolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val rg = findViewById<RadioGroup>(R.id.rgLanguage)
        when (LocaleHelper.getLanguage(this)) {
            LocaleHelper.LANG_HI -> rg.check(R.id.rbHindi)
            else -> rg.check(R.id.rbEnglish)
        }

        findViewById<MaterialButton>(R.id.btnApplyLanguage).setOnClickListener {
            val code = when (rg.checkedRadioButtonId) {
                R.id.rbHindi -> LocaleHelper.LANG_HI
                else -> LocaleHelper.LANG_EN
            }
            LocaleHelper.persistLanguage(this, code)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: Exception) {
            null
        }
        findViewById<TextView>(R.id.tvAppVersion).text =
            getString(R.string.settings_version_format, versionName ?: "—")
    }

    override fun onStart() {
        super.onStart()
        subscriptionBilling?.start()
        refreshSubscriptionUi()
    }

    override fun onResume() {
        super.onResume()
        refreshAccountUi()
        subscriptionBilling?.refreshEntitlement()
        bindSubscriptionPlans()
        refreshSubscriptionUi()
    }

    override fun onDestroy() {
        subscriptionBilling?.destroy()
        subscriptionBilling = null
        super.onDestroy()
    }

    private fun refreshAccountUi() {
        if (UserAccountManager.isLoggedIn(this)) {
            val email = UserAccountManager.registeredEmail(this).orEmpty()
            tvAccountStatus.text = getString(R.string.auth_status_logged_in, email)
            btnAccountAction.setText(R.string.auth_btn_logout)
        } else {
            tvAccountStatus.text = getString(
                R.string.auth_status_guest,
                SubscriptionManager.GUEST_KUNDLI_LIMIT,
                SubscriptionManager.LOGGED_IN_KUNDLI_LIMIT
            )
            btnAccountAction.setText(R.string.auth_btn_login_settings)
        }
    }

    private fun refreshSubscriptionUi() {
        val subscribed = SubscriptionManager.isSubscribed(this)
        tvSubscriptionStatus.text = if (subscribed) {
            getString(R.string.subscription_status_premium)
        } else {
            getString(R.string.subscription_status_free)
        }
        btnSubscribe.visibility = if (subscribed) View.GONE else View.VISIBLE
        btnManageSubscription.visibility = if (subscribed) View.VISIBLE else View.GONE
        val planPickerVisible = if (subscribed) View.GONE else View.VISIBLE
        tvChoosePlan.visibility = planPickerVisible
        rgSubscriptionPlan.visibility = planPickerVisible
        cardPdfProfile.visibility = if (subscribed) View.VISIBLE else View.GONE

        lifecycleScope.launch {
            val savedCount = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(applicationContext).kundliDao().countKundli()
            }
            tvSubscriptionStorage.text =
                SubscriptionManager.storageStatusText(this@SettingsActivity, savedCount)
        }
    }

    private fun openPrivacyPolicy() {
        val url = getString(R.string.privacy_policy_url).trim()
        if (url.isBlank() || url.contains("YOUR_USERNAME", ignoreCase = true)) {
            Toast.makeText(this, R.string.toast_privacy_url_not_set, Toast.LENGTH_LONG).show()
            return
        }
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun loadPdfProfile() {
        val p = SubscriberProfileManager.load(this)
        etProfileName.setText(p.name)
        etProfilePhone.setText(p.phone)
        etProfileAddress.setText(p.address)
    }

    private fun savePdfProfile() {
        SubscriberProfileManager.save(
            this,
            etProfileName.text?.toString().orEmpty(),
            etProfilePhone.text?.toString().orEmpty(),
            etProfileAddress.text?.toString().orEmpty()
        )
        Toast.makeText(this, R.string.settings_profile_saved, Toast.LENGTH_SHORT).show()
    }

    private fun openManageSubscription() {
        val productId = getString(R.string.subscription_product_id)
        val uri = Uri.parse(
            "https://play.google.com/store/account/subscriptions" +
                "?sku=$productId&package=$packageName"
        )
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun bindSubscriptionPlans() {
        val billing = subscriptionBilling ?: return
        val plans = billing.getAvailablePlans()
        val entries = listOf(
            rbPlanMonthly to getString(R.string.subscription_base_plan_monthly),
            rbPlanQuarterly to getString(R.string.subscription_base_plan_quarterly),
            rbPlanYearly to getString(R.string.subscription_base_plan_yearly)
        )
        var firstVisibleId: Int? = null
        for ((button, basePlanId) in entries) {
            val plan = plans.firstOrNull { it.basePlanId.equals(basePlanId, ignoreCase = true) }
            if (plan == null) {
                button.visibility = View.GONE
                continue
            }
            button.visibility = View.VISIBLE
            button.text = getString(
                R.string.subscription_plan_price_format,
                plan.label,
                plan.priceText
            )
            button.tag = plan.basePlanId
            if (firstVisibleId == null) {
                firstVisibleId = button.id
            }
        }
        if (rgSubscriptionPlan.checkedRadioButtonId == View.NO_ID ||
            findViewById<RadioButton>(rgSubscriptionPlan.checkedRadioButtonId)?.visibility != View.VISIBLE
        ) {
            firstVisibleId?.let { rgSubscriptionPlan.check(it) }
        }
        val anyVisible = entries.any { (btn, _) -> btn.visibility == View.VISIBLE }
        val showPicker = anyVisible && !SubscriptionManager.isSubscribed(this)
        tvChoosePlan.visibility = if (showPicker) View.VISIBLE else View.GONE
        rgSubscriptionPlan.visibility = if (showPicker) View.VISIBLE else View.GONE
    }

    private fun selectedBasePlanId(): String? {
        val checkedId = rgSubscriptionPlan.checkedRadioButtonId
        if (checkedId == View.NO_ID) return null
        return findViewById<RadioButton>(checkedId)?.tag as? String
    }
}
