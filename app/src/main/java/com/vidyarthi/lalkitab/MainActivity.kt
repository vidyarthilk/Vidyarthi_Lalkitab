package com.vidyarthi.lalkitab

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.vidyarthi.lalkitab.ads.AdsManager
import com.vidyarthi.lalkitab.ads.ConsentManager
import com.vidyarthi.lalkitab.data.KundliData
import com.vidyarthi.lalkitab.navigation.Dasha35TabFragment
import com.vidyarthi.lalkitab.navigation.HomeTabFragment
import com.vidyarthi.lalkitab.navigation.KundliChartsTabFragment
import com.vidyarthi.lalkitab.navigation.MainKundliNavigator
import com.vidyarthi.lalkitab.navigation.PanchangTabFragment
import com.vidyarthi.lalkitab.navigation.VarshfalTabFragment
import com.vidyarthi.lalkitab.ui.BottomNavUi
import com.vidyarthi.lalkitab.ui.StorageWelcomeHelper
import com.vidyarthi.lalkitab.ui.kundli.SharedKundliViewModel
import com.vidyarthi.lalkitab.update.PlayStoreUpdateHelper
import com.vidyarthi.lalkitab.pdf.KundliReportPdfExporter
import com.vidyarthi.lalkitab.pdf.PdfShareHelper
import com.vidyarthi.lalkitab.utils.KundliHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import com.vidyarthi.lalkitab.utils.SwissEphManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout

class MainActivity : BaseActivity(), MainKundliNavigator {

    private lateinit var sharedVM: SharedKundliViewModel

    private val tabFragments = mutableMapOf<Int, Fragment>()
    private var currentTabId: Int = R.id.nav_home
    private lateinit var playStoreUpdateHelper: PlayStoreUpdateHelper

    private data class BottomNavItem(
        val tabId: Int,
        @StringRes val labelRes: Int,
        @DrawableRes val iconRes: Int
    )

    private val bottomTabOrder = listOf(
        BottomNavItem(R.id.nav_home, R.string.nav_home, R.drawable.ic_nav_kundli),
        BottomNavItem(R.id.nav_panchang, R.string.nav_panchang, R.drawable.ic_nav_panchang),
        BottomNavItem(R.id.nav_kundli, R.string.nav_kundli, R.drawable.ic_nav_janam),
        BottomNavItem(R.id.nav_varshfal, R.string.nav_varshfal, R.drawable.ic_nav_varshfal),
        BottomNavItem(R.id.nav_35_varsh, R.string.nav_35_varsh, R.drawable.ic_nav_cycle)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Vidyarthi_Lalkitab)
        super.onCreate(savedInstanceState)

        if (!SwissEphManager.isInitialized()) {
            startActivity(EphemerisErrorActivity.intent(this))
            finish()
            return
        }

        // Must register before STARTED — consent callback runs after onCreate returns.
        playStoreUpdateHelper = PlayStoreUpdateHelper(this)

        ConsentManager.gatherConsent(this) {
            if (isFinishing || isDestroyed) return@gatherConsent
            AdsManager.initialize(applicationContext)
            finishMainSetup(savedInstanceState)
        }
    }

    private fun finishMainSetup(savedInstanceState: Bundle?) {
        if (isFinishing || isDestroyed) return
        sharedVM = ViewModelProvider(this)[SharedKundliViewModel::class.java]
        setContentView(R.layout.activity_main)

        findViewById<MaterialToolbar>(R.id.toolbar).apply {
            inflateMenu(R.menu.main_menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_settings -> {
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                        true
                    }
                    R.id.menu_export_pdf -> {
                        exportKundliPdf()
                        true
                    }
                    else -> false
                }
            }
        }

        setupBottomNavigation()
        handleLaunchIntent(intent)

        if (savedInstanceState == null && !intent.hasExtra(EXTRA_KUNDLI)) {
            switchToBottomTab(R.id.nav_home)
        } else if (savedInstanceState != null) {
            currentTabId = savedInstanceState.getInt(STATE_CURRENT_TAB, R.id.nav_home)
            selectBottomTabUi(currentTabId)
            showTabFragment(currentTabId)
        }
        updateToolbarTitle()
        playStoreUpdateHelper.checkForUpdate()
        if (savedInstanceState == null) {
            StorageWelcomeHelper.showIfFirstLaunch(this)
        }
        AdsManager.preloadInterstitial(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        intent ?: return
        val k: KundliData? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_KUNDLI, KundliData::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_KUNDLI) as? KundliData
        }
        if (k != null) {
            val tabId = intent.getIntExtra(EXTRA_BOTTOM_TAB, R.id.nav_panchang)
            onKundliOpened(k, tabId)
        }
    }

    private fun setupBottomNavigation() {
        val tabLayout = findViewById<TabLayout>(R.id.bottomNavTabs)
        bottomTabOrder.forEach { item ->
            val tab = tabLayout.newTab()
            val customView = layoutInflater.inflate(R.layout.item_bottom_nav_tab, tabLayout, false)
            customView.findViewById<ImageView>(R.id.tabIcon).setImageResource(item.iconRes)
            customView.findViewById<TextView>(R.id.tabText).text = getString(item.labelRes)
            tab.customView = customView
            tab.tag = item.tabId
            tabLayout.addTab(tab, false)
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                BottomNavUi.applyBottomNavStyle(tab.customView, true)
                val tabId = tab.tag as? Int ?: return
                if (tabId != currentTabId) {
                    currentTabId = tabId
                    showTabFragment(tabId)
                    updateToolbarTitle()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                BottomNavUi.applyBottomNavStyle(tab.customView, false)
            }

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        bottomTabOrder.indices.forEach { index ->
            BottomNavUi.applyBottomNavStyle(tabLayout.getTabAt(index)?.customView, index == 0)
        }
    }

    override fun onKundliOpened(data: KundliData, bottomTabId: Int?) {
        AdsManager.showInterstitialIfReady(this) {
            applyKundliOpened(data, bottomTabId)
        }
    }

    private fun applyKundliOpened(data: KundliData, bottomTabId: Int?) {
        sharedVM.openKundliSession(data)
        bottomTabId?.let { switchToBottomTab(it) } ?: updateToolbarTitle()
    }

    override fun switchToBottomTab(bottomTabId: Int) {
        currentTabId = bottomTabId
        selectBottomTabUi(bottomTabId)
        showTabFragment(bottomTabId)
        updateToolbarTitle()
    }

    private fun selectBottomTabUi(bottomTabId: Int) {
        val tabLayout = findViewById<TabLayout>(R.id.bottomNavTabs)
        val index = bottomTabOrder.indexOfFirst { it.tabId == bottomTabId }.coerceAtLeast(0)
        tabLayout.getTabAt(index)?.select()
    }

    private fun showTabFragment(tabId: Int) {
        val fragment = tabFragments.getOrPut(tabId) { createTabFragment(tabId) }
        supportFragmentManager.beginTransaction()
            .replace(R.id.mainNavHost, fragment, tabId.toString())
            .commit()
    }

    private fun createTabFragment(tabId: Int): Fragment = when (tabId) {
        R.id.nav_home -> HomeTabFragment()
        R.id.nav_panchang -> PanchangTabFragment()
        R.id.nav_kundli -> KundliChartsTabFragment()
        R.id.nav_varshfal -> VarshfalTabFragment()
        R.id.nav_35_varsh -> Dasha35TabFragment()
        else -> HomeTabFragment()
    }

    private fun updateToolbarTitle() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val titleView = findViewById<TextView>(R.id.tvAppTitle)
        val kundliName = sharedVM.kundliData.value?.name?.takeIf { it.isNotBlank() }
        val title = when {
            currentTabId == R.id.nav_home -> getString(R.string.app_title)
            kundliName != null -> kundliName
            else -> getString(R.string.app_title)
        }
        titleView.text = title
        toolbar.title = ""
        toolbar.menu.findItem(R.id.menu_settings)?.isVisible = currentTabId == R.id.nav_home
        toolbar.menu.findItem(R.id.menu_export_pdf)?.isVisible =
            currentTabId != R.id.nav_home && sharedVM.kundliData.value != null
    }

    private fun exportKundliPdf() {
        val k = sharedVM.kundliData.value ?: return
        Toast.makeText(this, R.string.pdf_generating, Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val file = withContext(Dispatchers.IO) {
                runCatching {
                    KundliReportPdfExporter.export(
                        this@MainActivity,
                        k,
                        KundliHolder.sessionCity,
                        KundliHolder.sessionGender
                    )
                }.getOrNull()
            }
            if (file == null) {
                Toast.makeText(this@MainActivity, R.string.pdf_export_failed, Toast.LENGTH_LONG).show()
            } else {
                PdfShareHelper.sharePdf(this@MainActivity, file)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_CURRENT_TAB, currentTabId)
    }

    companion object {
        const val EXTRA_KUNDLI = "extra_kundli"
        const val EXTRA_BOTTOM_TAB = "extra_bottom_tab"
        private const val STATE_CURRENT_TAB = "main_current_tab"
    }
}
