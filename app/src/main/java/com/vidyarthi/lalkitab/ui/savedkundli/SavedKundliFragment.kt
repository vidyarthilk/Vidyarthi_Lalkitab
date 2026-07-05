package com.vidyarthi.lalkitab.ui.savedkundli

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.vidyarthi.lalkitab.R
import com.vidyarthi.lalkitab.ads.BannerAdHost
import com.vidyarthi.lalkitab.data.CityRepository
import com.vidyarthi.lalkitab.data.db.AppDatabase
import com.vidyarthi.lalkitab.data.entity.KundliEntity
import com.vidyarthi.lalkitab.subscription.SubscriptionManager
import com.vidyarthi.lalkitab.ui.kundli.KundliFlowLauncher
import com.vidyarthi.lalkitab.ui.kundli.SharedKundliViewModel
import com.vidyarthi.lalkitab.navigation.MainKundliNavigator
import com.vidyarthi.lalkitab.utils.KundliDataMapper
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SavedKundliFragment : Fragment() {

    private val sharedVM: SharedKundliViewModel by activityViewModels()

    private var recyclerView: RecyclerView? = null
    private var searchView: SearchView? = null
    private lateinit var cityRepository: CityRepository
    private var adapter: SavedKundliAdapter? = null
    private var allKundlis: List<KundliEntity> = emptyList()
    private val bannerAd = BannerAdHost(this)
    private var emptySavedCard: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_saved_kundli, container, false)
        recyclerView = view.findViewById(R.id.rvSavedKundli)
        searchView = view.findViewById(R.id.searchView)
        emptySavedCard = view.findViewById(R.id.emptySavedCard)
        cityRepository = CityRepository(requireContext())

        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        adapter = SavedKundliAdapter(
            emptyList(),
            onClick = { openKundli(it) },
            onLongClick = { showKundliOptions(it) }
        )
        recyclerView?.adapter = adapter

        setupSearch()
        bannerAd.attach(view)
        return view
    }

    override fun onResume() {
        super.onResume()
        bannerAd.onResume()
        loadKundliList()
    }

    override fun onPause() {
        bannerAd.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        bannerAd.onDestroyView()
        super.onDestroyView()
    }

    private fun loadKundliList() {
        lifecycleScope.launch {
            val dao = AppDatabase.getDatabase(requireContext()).kundliDao()
            allKundlis = withContext(Dispatchers.IO) { dao.getAllKundli() }
            adapter?.updateList(allKundlis)
            emptySavedCard?.visibility = if (allKundlis.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupSearch() {
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = true

            override fun onQueryTextChange(newText: String?): Boolean {
                val q = newText?.trim().orEmpty()
                val filtered = if (q.isEmpty()) {
                    allKundlis
                } else {
                    allKundlis.filter {
                        it.name.contains(q, ignoreCase = true) ||
                            it.city.contains(q, ignoreCase = true)
                    }
                }
                adapter?.updateList(filtered)
                emptySavedCard?.visibility = if (allKundlis.isEmpty()) View.VISIBLE else View.GONE
                return true
            }
        })
    }

    private fun openKundli(entity: KundliEntity) {
        lifecycleScope.launch {
            val cityPick = withContext(Dispatchers.IO) {
                cityRepository.resolveCityPickForKundli(entity.city)
            }
            if (cityPick == null) {
                Toast.makeText(
                    requireContext(),
                    requireContext().getString(R.string.toast_select_city),
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            val kundliData = KundliDataMapper.fromEntity(entity, cityPick)
            if (kundliData == null) {
                Toast.makeText(
                    requireContext(),
                    requireContext().getString(R.string.toast_error_prefix, "date/time"),
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }
            KundliFlowLauncher.open(
                requireContext(),
                kundliData,
                R.id.nav_panchang,
                city = entity.city,
                gender = entity.gender
            )
        }
    }

    private fun showKundliOptions(entity: KundliEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_kundli_options)
            .setItems(
                arrayOf(
                    getString(R.string.dialog_edit),
                    getString(R.string.dialog_delete)
                )
            ) { _, which ->
                when (which) {
                    0 -> startEditKundli(entity)
                    1 -> deleteKundli(entity)
                }
            }
            .show()
    }

    private fun startEditKundli(entity: KundliEntity) {
        sharedVM.setEditKundli(entity)
        (requireActivity() as? MainKundliNavigator)?.switchToBottomTab(R.id.nav_home)
        requireActivity()
            .findViewById<ViewPager2>(R.id.viewPagerKundliHome)
            ?.setCurrentItem(0, true)
    }

    private fun deleteKundli(entity: KundliEntity) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(requireContext()).kundliDao().deleteKundli(entity)
            }
            loadKundliList()
            val root = view ?: return@launch
            Snackbar.make(root, R.string.snackbar_kundli_deleted, Snackbar.LENGTH_LONG)
                .setAction(R.string.snackbar_undo) {
                    lifecycleScope.launch {
                        val ctx = requireContext()
                        val ok = withContext(Dispatchers.IO) {
                            val dao = AppDatabase.getDatabase(ctx).kundliDao()
                            val count = dao.countKundli()
                            if (!SubscriptionManager.canRestoreDeleted(ctx, count)) {
                                false
                            } else {
                                dao.insertKundli(entity)
                                true
                            }
                        }
                        if (!ok) {
                            Toast.makeText(
                                ctx,
                                SubscriptionManager.undoLimitReachedMessage(ctx),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        loadKundliList()
                    }
                }
                .show()
        }
    }
}
