package com.dirror.music.ui.activity

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.dirror.music.MyApplication
import com.dirror.music.R
import com.dirror.music.broadcast.HeadsetChangeReceiver
import com.dirror.music.databinding.ActivityMainBinding
import com.dirror.music.music.standard.SongPicture
import com.dirror.music.ui.dialog.PlaylistDialog
import com.dirror.music.ui.viewmodel.MainViewModel
import com.dirror.music.util.*
import com.google.android.material.tabs.TabLayoutMediator
import eightbitlab.com.blurview.RenderScriptBlur

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var musicBroadcastReceiver: MusicBroadcastReceiver // 音乐广播接收
    private lateinit var headSetChangeReceiver: HeadsetChangeReceiver // 耳机广播接收

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initData()
        initView()
        initListener()

        mainViewModel.getUserId().observe(this, { userId ->
            // toast("main:$it")
            if (userId == 0L) {
                binding.menuMain.tvUserName.text = "立即登录"
                // MyApplication.activityManager.startLoginActivity(this@MainActivity)
            } else {
                MyApplication.cloudMusicManager.getUserDetail(userId, {
                    runOnUiThread {
                        GlideUtil.load(it.profile.avatarUrl, binding.menuMain.ivCover)
                        binding.menuMain.tvUserName.text = it.profile.nickname
                    }
                }, {

                })
            }
        })
    }

    private fun initData() {
        var intentFilter = IntentFilter() // Intent 过滤器
        intentFilter.addAction("com.dirror.music.MUSIC_BROADCAST") // 只接收 "com.dirror.foyou.MUSIC_BROADCAST" 标识广播
        musicBroadcastReceiver = MusicBroadcastReceiver() //
        registerReceiver(musicBroadcastReceiver, intentFilter) // 注册接收器

        intentFilter = IntentFilter()
        intentFilter.addAction("android.intent.action.HEADSET_PLUG")
        headSetChangeReceiver = HeadsetChangeReceiver()
        registerReceiver(headSetChangeReceiver, intentFilter) // 注册接收器

        // 检查新版本
        UpdateUtil.checkNewVersion(this, false)
    }


    private fun initView() {
        // 请求广播
        MyApplication.musicBinderInterface?.sendBroadcast()

        val radius = 20f
        val decorView: View = window.decorView
        val windowBackground: Drawable = decorView.background
        binding.blurView.setupWith(decorView.findViewById(R.id.viewPager2))
            .setFrameClearDrawable(windowBackground)
            .setBlurAlgorithm(RenderScriptBlur(this))
            .setBlurRadius(radius)
            .setHasFixedTransformationMatrix(true)
        binding.blurViewPlay.setupWith(decorView.findViewById(R.id.viewPager2))
            .setFrameClearDrawable(windowBackground)
            .setBlurAlgorithm(RenderScriptBlur(this))
            .setBlurRadius(radius)
            .setHasFixedTransformationMatrix(true)

        // 适配状态栏
        val statusBarHeight = getStatusBarHeight(window, this) // px
        binding.titleBar.translationY = statusBarHeight.toFloat()
        binding.blurView.scaleY = (dp2px(56f) + statusBarHeight) / dp2px(56f)
        binding.blurView.translationY = statusBarHeight.toFloat() / 2
        binding.blurViewBottom.scaleY = binding.blurView.scaleY
        binding.blurViewBottom.translationY = statusBarHeight.toFloat() / 2
        // 侧滑状态栏适配
        (binding.menuMain.clUser.layoutParams as LinearLayout.LayoutParams).apply{
            topMargin = statusBarHeight
        }

        // 适配导航栏
        val navigationBarHeight = getNavigationBarHeight(this).toFloat()
        binding.clPlay.translationY = -navigationBarHeight
        binding.blurViewPlay.scaleY = (dp2px(56f) + navigationBarHeight) / dp2px(56f)
        binding.blurViewPlay.translationY = -navigationBarHeight / 2
        binding.blurViewPlayBottom.scaleY = (dp2px(56f) + navigationBarHeight) / dp2px(56f)
        binding.blurViewPlayBottom.translationY = -navigationBarHeight / 2

        binding.viewPager2.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int {
                return 2 // 2 个页面
            }

            override fun createFragment(position: Int): Fragment {
                return FragmentUtil.getFragment(position)
            }
        }

        // navigationView.itemIconTintList = null

        // 默认打开首页
        if (MyApplication.userManager.isUidLogin()) {
            binding.viewPager2.currentItem = 1
        } else {
            binding.viewPager2.currentItem = 0
        }

        ViewPager2Util.changeToNeverMode(binding.viewPager2)

        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.my)
                else -> getString(R.string.home)
            }
        }.attach()

        binding.includePlayer.root.setOnClickListener {
            startActivity(Intent(this, PlayActivity::class.java))
            overridePendingTransition(
                R.anim.anim_slide_enter_bottom,
                R.anim.anim_no_anim
            )
        }

        binding.includePlayer.ivPlaylist.setOnClickListener {
            PlaylistDialog(this).show()
        }

    }

    @SuppressLint("WrongConstant")
    private fun initListener() {

        // 播放栏
        binding.includePlayer.ivPlay.setOnClickListener {
            // 更新
            MyApplication.musicBinderInterface?.changePlayState()
            refreshPlayState()
        }

        // 搜索按钮
        binding.ivSearch.setOnClickListener {
            startActivity(Intent(this@MainActivity, SearchActivity::class.java))
        }


        // 设置按钮
        binding.ivSettings.setOnClickListener {
            binding.drawerLayout.openDrawer(Gravity.START)
        }


        // 侧滑
        binding.menuMain.apply {
            clUser.setOnClickListener {
                if (MyApplication.userManager.getCurrentUid() == 0L) {
                    MyApplication.activityManager.startLoginActivity(this@MainActivity)
                } else {
                    MyApplication.activityManager.startUserActivity(this@MainActivity, MyApplication.userManager.getCurrentUid())
                }
            }

            itemSwitchAccount.setOnClickListener {
                MyApplication.activityManager.startLoginActivity(this@MainActivity)
            }

            itemLoginByPhone.setOnClickListener {
                MyApplication.activityManager.startLoginByPhoneActivity(this@MainActivity)
            }

        }


        binding.menuMain.switchPlayOnMobile.isChecked = MyApplication.mmkv.decodeBool(Config.PLAY_ON_MOBILE, false)
        binding.menuMain.switchPauseSongAfterUnplugHeadset.isChecked = MyApplication.mmkv.decodeBool(Config.PAUSE_SONG_AFTER_UNPLUG_HEADSET, true)

        // 反馈
        binding.menuMain.itemFeedback.setOnClickListener {
            startActivity(Intent(this, FeedbackActivity::class.java))
        }

        binding.menuMain.itemSourceCode.setOnClickListener {
            MyApplication.activityManager.startWebActivity(this, "https://github.com/Moriafly/dirror-music")
        }

        binding.menuMain.itemPlayOnMobile.setOnClickListener {
            binding.menuMain.switchPlayOnMobile.isChecked = !binding.menuMain.switchPlayOnMobile.isChecked
        }

        binding.menuMain.switchPlayOnMobile.setOnCheckedChangeListener { _, isChecked ->
            MyApplication.mmkv.encode(Config.PLAY_ON_MOBILE, isChecked)
        }

        binding.menuMain.itemPauseSongAfterUnplugHeadset.setOnClickListener {
            binding.menuMain.switchPauseSongAfterUnplugHeadset.isChecked = !binding.menuMain.switchPauseSongAfterUnplugHeadset.isChecked
        }

        binding.menuMain.switchPauseSongAfterUnplugHeadset.setOnCheckedChangeListener { _, isChecked ->
            MyApplication.mmkv.encode(Config.PAUSE_SONG_AFTER_UNPLUG_HEADSET, isChecked)
        }

        binding.menuMain.itemAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        // 解绑广播接收
        unregisterReceiver(musicBroadcastReceiver)
        unregisterReceiver(headSetChangeReceiver)
    }

    inner class MusicBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val song = MyApplication.musicBinderInterface?.getNowSongData()
            if (song != null) {
                binding.includePlayer.tvName.text = song.name
                binding.includePlayer.tvArtist.text = song.artists?.let { parseArtist(it) }
                // 这里应该用小的，等待修改
                SongPicture.getSongPicture(song, SongPicture.TYPE_LARGE) {
                    binding.includePlayer.ivCover.setImageBitmap(it)
                }
            }
            refreshPlayState()
        }
    }

    /**
     * 刷新播放状态
     */
    private fun refreshPlayState() {
        if (MyApplication.musicBinderInterface?.getPlayState()!!) {
            binding.includePlayer.ivPlay.setImageResource(R.drawable.ic_bq_control_pause)
        } else {
            binding.includePlayer.ivPlay.setImageResource(R.drawable.ic_bq_control_play)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            0 -> if (resultCode == RESULT_OK) {
                // toast("activity 回调成功")
                // 通知 viewModel
                mainViewModel.setUserId()
            }
        }
    }

}