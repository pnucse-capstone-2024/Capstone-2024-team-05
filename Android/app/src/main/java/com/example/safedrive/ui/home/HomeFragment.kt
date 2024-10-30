package com.example.safedrive.ui.home

import CardNewsAdapter
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.safedrive.R
import com.example.safedrive.databinding.FragmentHomeBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class HomeFragment : Fragment() {

    private var _homeFragmentBinding: FragmentHomeBinding? = null
    private val homeFragmentBinding get() = _homeFragmentBinding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _homeFragmentBinding = FragmentHomeBinding.inflate(inflater, container, false)

        homeFragmentBinding.notificationCard.setOnClickListener {
            val dialog = Dialog(requireContext())
            dialog.setContentView(R.layout.dialog_card_news)

            val viewPager = dialog.findViewById<ViewPager2>(R.id.viewPagerCardNews)
            val tabLayout = dialog.findViewById<TabLayout>(R.id.tabLayout)
            val closeBtn = dialog.findViewById<ImageButton>(R.id.btnClose)

            val cardNewsTitle = "교통사고 발생 시 대처법" // 모든 카드 뉴스의 동일한 제목
            val cardNewsList = listOf(
                Triple("1. 사고 발생 즉시 인근에 차량을 정차시킵니다.", R.drawable.img_accident_step1, "비상등 켜고 안전한 곳에 정차해! 사고 현장을 벗어나면 뺑소니로 오해 받을 수 있어!"),
                Triple("2. 차에서 내려 상대방의 상해 정도를 확인합니다.", R.drawable.img_accident_step2, "상대 차량 탑승자가 부상을 입었는지 확인하기! 119에 재빨리 신고해!"),
                Triple("3. 보험회사와 경찰서에 사고 신고를 합니다.", R.drawable.img_accident_step3, "보험회사에 연락해! 112에 신고해! 가벼운 접촉사고라고 연락 안 하면 나중에 악용될 수 있어!"),
                Triple("4. 사고 현장을 보존하거나 증거 사진을 확보합니다.", R.drawable.img_accident_step4, "카메라로 차량 손상 부위, 파손 정도, 형태 등을 꼼꼼하게 찍어놔!"),
                Triple("5. 상대 차와 함께 안전한 장소로 이동합니다.", R.drawable.img_accident_step5, "현장 사진을 확보했다면 상대방과 안전한 곳으로 차를 이동시켜 보험사와 경찰을 조용히 기다려!")
            )

            val adapter = CardNewsAdapter(cardNewsTitle, cardNewsList)
            viewPager.adapter = adapter

            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            }.attach()

            closeBtn.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        setBottomNavigationVisibility(View.GONE)
        setupClickListeners()
        return homeFragmentBinding.root
    }

    private fun setupClickListeners() {
        homeFragmentBinding.navCamera.setOnClickListener { navigateTo(R.id.navigation_camera) }
        homeFragmentBinding.navMap.setOnClickListener { navigateTo(R.id.navigation_map) }
        homeFragmentBinding.navDashboard.setOnClickListener { navigateTo(R.id.navigation_dashboard) }
    }

    private fun navigateTo(destinationId: Int) {
        findNavController().navigate(destinationId)
    }

    private fun setBottomNavigationVisibility(visibility: Int) {
        val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.nav_view)
        bottomNavigationView?.visibility = visibility
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _homeFragmentBinding = null
    }

    override fun onResume() {
        super.onResume()
        setBottomNavigationVisibility(View.GONE)
    }

    override fun onStop() {
        super.onStop()
        setBottomNavigationVisibility(View.VISIBLE)
    }
}