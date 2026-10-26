package com.neocode.surfx

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import org.json.JSONObject
import java.net.URL
import kotlin.concurrent.thread
import kotlinx.coroutines.delay

private val Bg = Color(0xFF081419)
private val Card = Color(0xFF102329)
private val CardAlt = Color(0xFF173539)
private val Mint = Color(0xFF62F4CC)
private val White = Color(0xFFF1F7F5)
private val Muted = Color(0xFF8CA6A8)

class MainActivity : ComponentActivity() {
    private val vm: SurfViewModel by viewModels()
    private var updateMessage by mutableStateOf("")
    var exitNativeAd by mutableStateOf<NativeAd?>(null)
        private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(8,20,25)
        window.navigationBarColor = android.graphics.Color.rgb(8,20,25)
        UserMessagingPlatform.getConsentInformation(this).requestConsentInfoUpdate(
            this, ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) {
                    if (UserMessagingPlatform.getConsentInformation(this).canRequestAds()) MobileAds.initialize(this)
                }
            }, { if (UserMessagingPlatform.getConsentInformation(this).canRequestAds()) MobileAds.initialize(this) }
        )
        checkVersion()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Mint, background = Bg, surface = Card)) {
                SurfApp(vm, updateMessage, { updateMessage = "" }, ::openUrl)
            }
        }
    }
    private fun openUrl(url: String) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    fun loadExitNativeAd() {
        if (exitNativeAd != null || !UserMessagingPlatform.getConsentInformation(this).canRequestAds()) return
        AdLoader.Builder(this, "ca-app-pub-3940256099942544/2247696110")
            .forNativeAd { ad -> exitNativeAd?.destroy(); exitNativeAd = ad }
            .build().loadAd(AdRequest.Builder().build())
    }
    fun showRewardedAd() {
        if (vm.adRewardDone) { vm.showNotice(vm.tr("오늘의 추가 보상은 이미 받았어요", "You already claimed today's bonus")); return }
        if (!UserMessagingPlatform.getConsentInformation(this).canRequestAds()) { vm.showNotice(vm.tr("광고를 불러올 수 없어요", "Ads are unavailable")); return }
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { ad.show(this@MainActivity) { vm.rewardAd() } }
            override fun onAdFailedToLoad(error: LoadAdError) { vm.showNotice(vm.tr("광고를 불러오지 못했어요. 다시 시도해 주세요", "Couldn't load the ad. Please try again.")) }
        })
    }
    private fun checkVersion() {
        thread {
            runCatching {
                val data = JSONObject(URL("https://neocode.cloud/appsettings.json").readText())
                val apps = data.optJSONArray("app") ?: return@runCatching
                for (i in 0 until apps.length()) {
                    val app = apps.getJSONObject(i)
                    if (app.optString("package") == packageName && app.optInt("aos_version_code") > 1) {
                        runOnUiThread { updateMessage = "새 버전이 있어요. Play 스토어에서 업데이트해 주세요." }
                    }
                }
            }
        }
    }
    override fun onDestroy() { exitNativeAd?.destroy(); super.onDestroy() }
}

@Composable
private fun SurfApp(vm: SurfViewModel, updateMessage: String, clearUpdate: () -> Unit, openUrl: (String) -> Unit) {
    val activity = LocalContext.current as Activity
    var exitDialog by remember { mutableStateOf(false) }
    var showBrandSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(900); showBrandSplash = false }
    BackHandler(vm.screen != Screen.Welcome) {
        if (vm.screen == Screen.Home) { (activity as MainActivity).loadExitNativeAd(); exitDialog = true } else vm.back()
    }
    if (exitDialog) AlertDialog(onDismissRequest = { exitDialog = false }, title = { Text(vm.tr("SurfX 종료", "Exit SurfX"), color = White) },
        text = { Column { Text(vm.tr("정말 종료하시겠습니까?", "Are you sure you want to exit?"), color = Muted); Gap(10); (activity as MainActivity).exitNativeAd?.let { ExitNativeAd(it) } } },
        confirmButton = { TextButton(onClick = { activity.finish() }) { Text(vm.tr("확인", "OK")) } },
        dismissButton = { TextButton(onClick = { exitDialog = false }) { Text(vm.tr("취소", "Cancel")) } },
        containerColor = Card)
    if (updateMessage.isNotEmpty()) AlertDialog(onDismissRequest = clearUpdate,
        title = { Text(vm.tr("업데이트 안내", "Update available")) }, text = { Text(vm.tr(updateMessage, "A new version is available on the Play Store.")) },
        confirmButton = { TextButton(onClick = { clearUpdate(); openUrl("https://play.google.com/store/apps/details?id=com.neocode.surfx") }) { Text(vm.tr("스토어 열기", "Open store")) } },
        dismissButton = { TextButton(onClick = clearUpdate) { Text(vm.tr("나중에", "Later")) } }, containerColor = Card)
    if (vm.notice.isNotEmpty()) AlertDialog(onDismissRequest = vm::clearNotice,
        text = { Text(vm.notice) }, confirmButton = { TextButton(onClick = vm::clearNotice) { Text(vm.tr("확인", "OK")) } }, containerColor = Card)

    Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding().navigationBarsPadding()) {
        if (showBrandSplash) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.neocode_logo), "neocode", Modifier.fillMaxWidth(0.75f))
            }
            return@Column
        }
        Box(Modifier.weight(1f)) {
            when (vm.screen) {
                Screen.Welcome -> Welcome(vm, openUrl)
                Screen.Home -> Home(vm)
                Screen.Checkin -> Checkin(vm)
                Screen.Missions -> Missions(vm)
                Screen.Lesson -> Lesson(vm)
                Screen.QuizBank -> QuizBank(vm)
                Screen.Quiz -> Quiz(vm)
                Screen.Result -> Result(vm)
                Screen.Crew -> Crew(vm)
                Screen.Invite -> Invite(vm)
                Screen.Wallet -> Wallet(vm)
                Screen.Board -> BoardScreen(vm)
                Screen.Ranking -> Ranking(vm)
                Screen.Profile -> Profile(vm, openUrl)
            }
        }
        if (vm.screen in setOf(Screen.Home, Screen.Missions, Screen.Crew, Screen.Wallet)) BottomNav(vm)
        if (vm.screen != Screen.Welcome) AdBanner()
    }
}

@Composable private fun ExitNativeAd(ad: NativeAd) {
    AndroidView(factory = { ctx ->
        NativeAdView(ctx).apply {
            val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(14, 12, 14, 12); setBackgroundColor(android.graphics.Color.rgb(23,53,57)) }
            val headline = TextView(ctx).apply { setTextColor(android.graphics.Color.WHITE); textSize = 14f }
            val body = TextView(ctx).apply { setTextColor(android.graphics.Color.LTGRAY); textSize = 12f }
            val action = Button(ctx).apply { textSize = 12f }
            layout.addView(headline); layout.addView(body); layout.addView(action)
            addView(layout)
            headlineView = headline; bodyView = body; callToActionView = action
        }
    }, update = { view ->
        (view.headlineView as TextView).text = ad.headline
        (view.bodyView as TextView).text = ad.body ?: ""
        (view.callToActionView as Button).text = ad.callToAction ?: "자세히 보기"
        view.setNativeAd(ad)
    }, modifier = Modifier.fillMaxWidth().height(130.dp))
}

@Composable private fun AdBanner() {
    AndroidView(factory = { context -> AdView(context).apply {
        adUnitId = "ca-app-pub-3940256099942544/6300978111"
        setAdSize(AdSize.BANNER)
        loadAd(AdRequest.Builder().build())
    } }, modifier = Modifier.fillMaxWidth().height(50.dp))
}

@Composable private fun BottomNav(vm: SurfViewModel) {
    Row(Modifier.fillMaxWidth().background(Color(0xFF0B1B20)).height(60.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        listOf(Triple("⌂", vm.tr("홈", "Home"), Screen.Home), Triple("▣", vm.tr("미션", "Missions"), Screen.Missions), Triple("♧", vm.tr("크루", "Crew"), Screen.Crew), Triple("▤", vm.tr("지갑", "Wallet"), Screen.Wallet)).forEach { (icon,label,target) ->
            val selected = vm.screen == target
            Column(Modifier.weight(1f).fillMaxHeight().clickable { vm.go(target) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(icon, color = if (selected) Mint else Muted, fontSize = 22.sp)
                Text(label, color = if (selected) Mint else Muted, fontSize = 10.sp)
            }
        }
    }
}

@Composable private fun Page(title: String, vm: SurfViewModel, body: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", modifier = Modifier.clickable { vm.back() }.padding(end = 20.dp), fontSize = 32.sp, color = White)
            Text(title, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(32.dp))
        }
        Spacer(Modifier.height(18.dp))
        body()
        Spacer(Modifier.height(28.dp))
    }
}
@Composable private fun Eyebrow(s: String) { Text(s.uppercase(), color = Mint, fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold) }
@Composable private fun H1(s: String) { Text(s, color = White, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold) }
@Composable private fun H2(s: String) { Text(s, color = White, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
@Composable private fun Sub(s: String) { Text(s, color = Muted, fontSize = 13.sp, lineHeight = 20.sp) }
@Composable private fun Gap(h: Int = 16) { Spacer(Modifier.height(h.dp)) }
@Composable private fun Primary(text: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(54.dp).background(Mint, RoundedCornerShape(13.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = Bg, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable private fun Secondary(text: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(48.dp).background(CardAlt, RoundedCornerShape(13.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
@Composable private fun Surface(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(20.dp)).padding(17.dp), content = content)
}
@Composable private fun Item(title: String, detail: String, trailing: String = "›", onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).background(Card, RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Gap(3); Sub(detail) }
        Text(trailing, color = Mint, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
@Composable private fun Hero(height: Int = 215) {
    Image(painterResource(R.drawable.surfx_wave), null, Modifier.fillMaxWidth().height(height.dp), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
}

@Composable private fun Welcome(vm: SurfViewModel, openUrl: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Gap(28); Text("〰 SurfX", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(0.3f)); Hero(285)
        Gap(8)
        Text(vm.tr("작은 도전,\n새로운 파도.", "Small challenges,\nnew waves."), color = White, textAlign = TextAlign.Center, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
        Gap(14); Sub(vm.tr("매일 배우고, 함께 도전하며\n나만의 포인트를 모아요.", "Learn every day, take on challenges together,\nand collect your points."))
        Spacer(Modifier.weight(1f)); Primary(vm.tr("체험 시작하기", "Get started"), vm::enter); Gap(10)
        Text(vm.tr("이 앱의 포인트와 활동은 이 기기에만 저장되는 체험 데이터입니다.", "Points and activity in this demo are stored only on this device."), color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
        Gap(12); Row {
            Text(vm.tr("서비스 이용약관", "Terms of service"), Modifier.clickable { openUrl("https://neocode.cloud/term_of_service.html") }, color = Muted, fontSize = 11.sp)
            Text("  ·  ", color = Muted)
            Text(vm.tr("개인정보 처리방침", "Privacy policy"), Modifier.clickable { openUrl("https://neocode.cloud/private_privacy.html") }, color = Muted, fontSize = 11.sp)
        }; Gap(10); Image(painterResource(R.drawable.neocode_logo), "neocode", Modifier.height(22.dp))
    }
}

@Composable private fun Home(vm: SurfViewModel) {
    LaunchedEffect(vm.surfingSince) {
        while (vm.surfingSince != 0L) { vm.tick(); delay(60_000) }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("〰 SurfX", Modifier.weight(1f), color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("W", Modifier.background(CardAlt, RoundedCornerShape(50)).clickable { vm.go(Screen.Profile) }.padding(horizontal = 13.dp, vertical = 9.dp), color = Mint)
        }
        Sub(vm.tr("내 포인트", "My points")); Row(Modifier.clickable { vm.go(Screen.Wallet) }, verticalAlignment = Alignment.Bottom) {
            Text("${"%,d".format(vm.points)}", color = White, fontSize = 30.sp, fontWeight = FontWeight.Bold); Text(" P  ›", color = Muted, fontSize = 16.sp)
        }
        Hero(225)
        Text(if (vm.surfingSince == 0L) vm.tr("오늘의 파도를 시작해요", "Start today's wave") else vm.tr("포인트가 쌓이고 있어요", "Your points are growing"), Modifier.fillMaxWidth(), color = White, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
        Gap(4); Text(if (vm.surfingSince == 0L) vm.tr("24시간 서핑하고 최대 120P", "Surf for 24 hours to earn up to 120P") else vm.tr("서핑 진행 중 · 체험 기록", "Surfing in progress · demo record"), Modifier.fillMaxWidth(), color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
        Gap(18); if (vm.surfingSince == 0L) Primary(vm.tr("〰  서핑 시작", "〰  Start surfing"), vm::startSurf)
            else if (vm.surfProgress() >= 1f) Primary(vm.tr("서핑 완료 · 120P 받기", "Surf complete · Claim 120P"), vm::claimSurf)
            else Primary(vm.tr("＋  미션으로 더 모으기", "＋  Earn more in missions")) { vm.go(Screen.Missions) }
        Gap(20)
        if (vm.surfingSince != 0L) {
            Surface { H2(vm.tr("이번 서핑", "Current surf")); Gap(10); LinearProgressIndicator(progress = { vm.surfProgress() }, modifier = Modifier.fillMaxWidth(), color = Mint, trackColor = CardAlt); Gap(7); Sub(vm.tr("적립 예정 ${(vm.surfProgress()*120).toInt()} / 120 P · 완료 후 받을 수 있어요", "Estimated ${(vm.surfProgress()*120).toInt()} / 120 P · Claim when complete")) }
        } else {
            H2(vm.tr("오늘의 작은 도전", "Today's challenges")); Gap(10)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeMission("♧", vm.tr("출석 체크", "Check in"), vm.tr("오늘도 만나서 반가워요", "Good to see you"), "+10P", Modifier.weight(1f)) { vm.go(Screen.Checkin) }
                HomeMission("▣", vm.tr("오늘의 퀴즈", "Daily quiz"), vm.tr("한 문제로 가볍게", "One quick question"), "+20P", Modifier.weight(1f)) { vm.openDailyQuiz() }
            }
        }
        Gap(8); Item(vm.tr("나의 Mint Wave", "My Mint Wave"), vm.tr("Lv. ${vm.boardLevel} · 다음 파도를 준비해요", "Lv. ${vm.boardLevel} · Ready for the next wave")) { vm.go(Screen.Board) }
        Gap(20)
    }
}
@Composable private fun HomeMission(icon: String, title: String, detail: String, reward: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.height(118.dp).background(Card, RoundedCornerShape(18.dp)).clickable(onClick = onClick).padding(15.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(icon, color = Mint, fontSize = 22.sp); Text(reward, color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
        Text(title, color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(detail, color = Muted, fontSize = 11.sp, maxLines = 1)
    }
}

@Composable private fun Checkin(vm: SurfViewModel) = Page(vm.tr("출석 체크", "Check in"), vm) {
    Eyebrow("A little, every day"); Gap(24)
    Text("🎁", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 62.sp); Gap(18)
    Text(vm.tr("매일 만나는 보상", "A daily reward"), Modifier.fillMaxWidth(), color = White, textAlign = TextAlign.Center, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    Gap(8); Text(vm.tr("오늘도 함께 파도를 이어가요", "Keep the wave going today"), Modifier.fillMaxWidth(), color = Muted, textAlign = TextAlign.Center)
    Gap(32); H2(vm.tr("7일 출석 챌린지", "7-day check-in challenge")); Gap(12)
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf(vm.tr("1일차\n✓\n+10P", "Day 1\n✓\n+10P"), vm.tr("2일차\n✓\n+10P", "Day 2\n✓\n+10P"), vm.tr("오늘\n🎁\n+10P", "Today\n🎁\n+10P"), vm.tr("4일차\n+15P", "Day 4\n+15P")).forEachIndexed { i, s ->
            Box(Modifier.weight(1f).height(105.dp).background(if (i == 2) Mint else Card, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Text(s, color = if (i == 2) Bg else White, textAlign = TextAlign.Center, fontSize = 13.sp, lineHeight = 22.sp)
            }
        }
    }
    Gap(10); Surface { H2(vm.tr("7일 완주 보너스  +50P", "7-day bonus  +50P")); Sub(vm.tr("매일 한 번, 작은 도전을 이어가 보세요.", "Keep up a small challenge every day.")) }
    Gap(35); Primary(if (vm.checkedIn) vm.tr("오늘 출석 완료", "Checked in today") else vm.tr("오늘의 10P 받기", "Claim today's 10P"), vm::checkin)
}

@Composable private fun Missions(vm: SurfViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text(vm.tr("미션", "Missions"), Modifier.fillMaxWidth(), color = White, textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Gap(28); Eyebrow("One step, one wave"); Gap(8); H1(vm.tr("오늘의 작은 도전", "Today's challenges")); Sub(vm.tr("가볍게 배우고, 차곡차곡 모아요.", "Learn a little and keep building."))
        Gap(24); Tabs(listOf(vm.tr("오늘", "Today"), vm.tr("진행 중", "In progress"), vm.tr("완료", "Completed")), vm.missionTab, vm::chooseMissionTab)
        Gap(15); Surface { Eyebrow(vm.tr("오늘의 추천", "TODAY'S PICK")); Gap(10); H2(vm.tr("Web3, 처음부터 가볍게", "An easy start with Web3")); Sub(vm.tr("2분 학습 · +30P", "2 min lesson · +30P")); Gap(12); Secondary(vm.tr("학습하러 가기", "Start learning")) { vm.go(Screen.Lesson) } }
        Gap(24); H2(vm.tr("참여할 수 있는 미션", "Available missions"))
        when(vm.missionTab) {
            0 -> { Item(vm.tr("오늘의 퀴즈", "Daily quiz"), vm.tr("1문제 · 약 30초", "1 question · about 30 seconds"), "+20P") { vm.openDailyQuiz() }; Item(vm.tr("블록체인 문제은행", "Blockchain question bank"), vm.tr("1,200문항 · 자유 연습", "1,200 questions · free practice")) { vm.go(Screen.QuizBank) }; Item(vm.tr("새로운 프로젝트 만나기", "Discover a new project"), vm.tr("소개 읽기 · 약 2분", "2 min read"), "+50P") { vm.go(Screen.Lesson) }; Item(vm.tr("오늘도 출석", "Check in today"), vm.tr("3일차 출석 받기", "Claim today's check-in"), "+10P") { vm.go(Screen.Checkin) } }
            1 -> Item(vm.tr("데일리 서핑", "Daily surfing"), vm.tr("서핑 상태 확인", "Check surfing status"), "›") { vm.go(Screen.Home) }
            else -> { if (vm.quizDone) Item(vm.tr("오늘의 퀴즈", "Daily quiz"), vm.tr("오늘 완료", "Completed today"), "✓") { vm.openDailyQuiz() }; if (vm.lessonDone) Item(vm.tr("Web3 학습", "Web3 lesson"), vm.tr("오늘 완료", "Completed today"), "✓") { vm.go(Screen.Lesson) }; if (vm.checkedIn) Item(vm.tr("출석 체크", "Check in"), vm.tr("오늘 완료", "Completed today"), "✓") { vm.go(Screen.Checkin) } }
        }
    }
}
@Composable private fun Tabs(labels: List<String>, selected: Int, choose: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(12.dp)).padding(4.dp)) {
        labels.forEachIndexed { i, label -> Box(Modifier.weight(1f).background(if (selected == i) CardAlt else Color.Transparent, RoundedCornerShape(10.dp)).clickable { choose(i) }.padding(vertical = 9.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (selected == i) White else Muted, fontSize = 13.sp)
        } }
    }
}

@Composable private fun Lesson(vm: SurfViewModel) = Page(vm.tr("가볍게 배우기", "Learn the basics"), vm) {
    Eyebrow("Web3 starter"); Gap(8); H1(vm.tr("블록체인,\n함께 기록하는 방법", "Blockchain: a shared record")); Gap(24)
    Box(Modifier.fillMaxWidth().height(180.dp).background(CardAlt, RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) { Text("▣    ◈    ♧", color = Mint, fontSize = 42.sp) }
    Gap(22); Sub(vm.tr("블록체인은 거래 기록을 여러 참여자가 함께 보관하고 확인하는 기술이에요.", "A blockchain lets many participants store and verify records together."))
    Gap(22); listOf(vm.tr("한 곳에 기록을 맡기지 않아요.", "Records aren't kept in one place."), vm.tr("기록의 변경 여부를 함께 확인해요.", "Participants can check for changes."), vm.tr("활동을 더 신뢰할 수 있게 도와줘요.", "This helps people trust the record.")).forEach { Item("✓  $it", "", "") {} }
    Gap(30); Primary(if (vm.lessonDone) vm.tr("퀴즈로 이어가기", "Continue to quiz") else vm.tr("학습 완료 · 30P 받기", "Finish lesson · Claim 30P"), vm::completeLesson)
}

@Composable private fun QuizBank(vm: SurfViewModel) = Page(vm.tr("블록체인 문제은행", "Blockchain question bank"), vm) {
    Eyebrow("BLOCKCHAIN QUIZ"); Gap(8)
    H1(vm.tr("1,200문항 자유 연습", "Practice 1,200 questions")); Gap(8)
    Sub(vm.tr("난이도와 문제 유형을 선택하세요. 연습 문제에는 포인트가 지급되지 않습니다.", "Choose a difficulty and question type. Practice does not award points."))
    Gap(24); H2(vm.tr("난이도", "Difficulty")); Gap(10)
    Tabs(listOf(vm.tr("전체", "All"), vm.tr("초급", "Beginner"), vm.tr("중급", "Intermediate"), vm.tr("고급", "Advanced")), listOf("all", "beginner", "intermediate", "advanced").indexOf(vm.difficultyFilter)) { vm.chooseDifficulty(listOf("all", "beginner", "intermediate", "advanced")[it]) }
    Gap(22); H2(vm.tr("문제 유형", "Question type")); Gap(10)
    Tabs(listOf(vm.tr("전체", "All"), vm.tr("4지선다", "Multiple choice"), "O/X"), listOf("all", "multiple_choice", "true_false").indexOf(vm.typeFilter)) { vm.chooseType(listOf("all", "multiple_choice", "true_false")[it]) }
    Gap(24); Sub(vm.tr("선택한 문제", "Selected questions") + "  ${vm.practiceQuestions().size} / 1,200")
    Gap(18); Primary(vm.tr("연습 시작", "Start practice"), vm::openPractice)
}

@Composable private fun Quiz(vm: SurfViewModel) = Page(if (vm.quizMode == "daily") vm.tr("오늘의 퀴즈", "Daily quiz") else vm.tr("문제은행", "Question bank"), vm) {
    val q = vm.currentQuestion()
    if (q == null) {
        Sub(if (vm.quizError) vm.tr("문제를 불러오지 못했어요.", "Couldn't load questions.") else vm.tr("문제를 불러오는 중…", "Loading questions…"))
        return@Page
    }
    val pool = if (vm.quizMode == "practice") vm.practiceQuestions() else listOf(q)
    val position = pool.indexOfFirst { it.id == q.id }.coerceAtLeast(0) + 1
    Eyebrow(if (vm.quizMode == "daily") "DAILY QUIZ" else "QUESTION $position / ${pool.size}")
    Gap(8); H1(q.topic); Gap(8)
    Sub("${q.order} / 1,200 · " + when (q.difficulty) { "beginner" -> vm.tr("초급", "Beginner"); "intermediate" -> vm.tr("중급", "Intermediate"); else -> vm.tr("고급", "Advanced") })
    Gap(20); LinearProgressIndicator(progress = { if (vm.quizMode == "daily") 1f else position.toFloat() / pool.size.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth(), color = Mint, trackColor = CardAlt)
    Gap(25); H2(q.question)
    Gap(22); q.options.forEachIndexed { i, option ->
        val selected = vm.selectedAnswer == option.id
        val correct = vm.answerRevealed && option.id == q.correctOptionId
        val border = if (correct) Mint else if (selected) (if (vm.answerRevealed) Color(0xFFFFA0A0) else Mint) else Color.Transparent
        Row(Modifier.fillMaxWidth().padding(bottom = 9.dp).background(if (selected || correct) CardAlt else Card, RoundedCornerShape(13.dp)).border(1.dp, border, RoundedCornerShape(13.dp)).clickable { vm.answer(option.id) }.padding(17.dp)) {
            Text("${if (q.type == "true_false") option.text else "${'A' + i}   ${option.text}"}", color = White, fontSize = 14.sp, modifier = Modifier.weight(1f))
            if (correct) Text("✓", color = Mint)
        }
    }
    Gap(16)
    if (vm.answerRevealed) {
        Surface {
            H2(if (vm.answerCorrect) vm.tr("정답이에요!", "Correct!") else vm.tr("아쉬워요", "Not quite"))
            if (vm.quizMode == "daily" && vm.answerCorrect) { Gap(5); Text(if (vm.rewardEarned) vm.tr("오늘의 보상: +20P", "Today's reward: +20P") else vm.tr("오늘의 보상은 이미 받았어요", "Today's reward was already claimed"), color = Mint) }
            Gap(12); Sub(q.explanation)
            Gap(12); Sub(vm.tr("학습목표: ", "Learning objective: ") + q.learningObjective)
        }
        Gap(18)
        if (vm.quizMode == "practice") Primary(vm.tr("다음 문제", "Next question"), vm::nextPractice)
        else Primary(vm.tr("미션으로 돌아가기", "Back to missions")) { vm.go(Screen.Missions) }
    } else Primary(vm.tr("정답 확인", "Check answer"), vm::submitQuiz)
}

@Composable private fun Result(vm: SurfViewModel) = Page(vm.tr("보상", "Reward"), vm) {
    val activity = LocalContext.current as MainActivity
    Spacer(Modifier.height(42.dp)); Text("✦", Modifier.fillMaxWidth(), color = Mint, textAlign = TextAlign.Center, fontSize = 92.sp)
    Gap(16); Text(vm.tr("정답이에요!", "Correct!"), Modifier.fillMaxWidth(), color = White, textAlign = TextAlign.Center, fontSize = 27.sp, fontWeight = FontWeight.Bold)
    Gap(12); Text(vm.tr("체험 포인트", "Demo points"), Modifier.fillMaxWidth(), color = Muted, textAlign = TextAlign.Center)
    Text("+20 P", Modifier.fillMaxWidth(), color = Mint, textAlign = TextAlign.Center, fontSize = 39.sp, fontWeight = FontWeight.Bold)
    Gap(22); Surface { H2(vm.tr("현재 보유 포인트", "Current balance")); Text("${"%,d".format(vm.points)} P", color = White, fontSize = 20.sp) }
    Gap(28); Primary(vm.tr("확인", "OK")) { vm.go(Screen.Missions) }; Gap(10); Secondary(vm.tr("내 지갑 보기", "View wallet")) { vm.go(Screen.Wallet) }
    Gap(10); Secondary(if (vm.adRewardDone) vm.tr("오늘의 광고 보상 완료", "Today's ad bonus claimed") else vm.tr("광고 보고 추가 체험 20P 받기", "Watch an ad for 20 more demo points")) { activity.showRewardedAd() }
}

@Composable private fun Crew(vm: SurfViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text(vm.tr("크루", "Crew"), Modifier.fillMaxWidth(), color = White, textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Gap(28); Eyebrow("Your crew"); Gap(8); H1(vm.tr("함께 타는 파도", "Ride the wave together")); Sub(vm.tr("작은 도전도 함께하면 더 멀리.", "Small challenges go further together."))
        Gap(22); Surface { Sub(vm.tr("내 크루", "My crew")); Text(vm.tr("8 명", "8 members"), color = White, fontSize = 30.sp, fontWeight = FontWeight.Bold); Sub(vm.tr("오늘 5명 참여", "5 active today")); Gap(10); Text("●  ●  ●  ●  +4", color = Mint, fontSize = 21.sp) }
        Gap(14); Primary(vm.tr("＋  친구 초대", "＋  Invite a friend")) { vm.go(Screen.Invite) }
        Gap(26); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { H2(vm.tr("크루 활동", "Crew activity")); Text(vm.tr("랭킹  ›", "Ranking  ›"), Modifier.clickable { vm.go(Screen.Ranking) }, color = Mint) }
        Item("Wave Rider", vm.tr("오늘도 새로운 파도에 도전", "Riding a new wave today"), vm.tr("서핑 중", "Surfing")) {}
        Item("Neo", vm.tr("3일 연속 출석", "3-day check-in streak"), "+10P") {}
        Item("Jin", vm.tr("오늘의 퀴즈 완료", "Daily quiz completed"), "+20P") {}
    }
}

@Composable private fun Invite(vm: SurfViewModel) = Page(vm.tr("친구 초대", "Invite a friend"), vm) {
    val ctx = LocalContext.current
    Eyebrow("Make waves together"); Gap(8); H1(vm.tr("좋은 파도는\n함께 타요.", "Great waves are\nbetter together.")); Gap(12); Sub(vm.tr("친구와 SurfX를 시작하고\n함께 성장해 보세요.", "Start SurfX with a friend\nand grow together."))
    Gap(35); Surface {
        Sub(vm.tr("내 초대 코드", "My invite code")); Gap(8); Text("SURFX24", color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Gap(14); Secondary(vm.tr("코드 복사", "Copy code")) { (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("SurfX invite code", "SURFX24")); vm.showNotice(vm.tr("초대 코드를 복사했어요", "Invite code copied")) }
    }
    Gap(12); Surface { H2(vm.tr("친구 초대 보상  +100P", "Friend invite reward  +100P")); Gap(5); Sub(vm.tr("친구가 첫 서핑을 완료하면 지급하는 예시 정책입니다. 체험 앱에서는 자동 지급되지 않아요.", "This is an example reward policy. The demo app does not award it automatically.")) }
    Gap(30); Primary(vm.tr("초대 링크 공유", "Share invite link")) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, vm.tr("SurfX Protocol 체험 코드: SURFX24", "SurfX Protocol invite code: SURFX24"))
        ctx.startActivity(Intent.createChooser(send, vm.tr("친구에게 공유", "Share with a friend")))
    }
}

@Composable private fun Wallet(vm: SurfViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text(vm.tr("지갑", "Wallet"), Modifier.fillMaxWidth(), color = White, textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Gap(30); Eyebrow("My points"); Gap(10); Sub(vm.tr("내 포인트", "My points")); Text("${"%,d".format(vm.points)} P", color = White, fontSize = 36.sp, fontWeight = FontWeight.Bold); Sub(vm.tr("나의 도전이 차곡차곡 쌓이는 곳", "Where your challenges add up"))
        Gap(22); Text(vm.tr("체험 포인트 · 현금이나 토큰으로 교환할 수 없어요", "Demo points · cannot be exchanged for cash or tokens"), color = Muted, fontSize = 12.sp)
        Gap(35); H2(vm.tr("포인트 내역", "Point history")); Gap(12); Tabs(listOf(vm.tr("전체", "All"), vm.tr("적립", "Earned"), vm.tr("사용", "Spent")), vm.walletTab, vm::chooseWalletTab)
        Gap(14); if (vm.walletTab != 1) Item(vm.tr("보드 업그레이드", "Board upgrade"), vm.tr("최근 활동", "Recent activity"), "-500P") {}
        if (vm.walletTab != 2) {
            if (vm.quizDone) Item(vm.tr("오늘의 퀴즈", "Daily quiz"), vm.tr("오늘", "Today"), "+20P") {}
            if (vm.lessonDone) Item(vm.tr("Web3 학습", "Web3 lesson"), vm.tr("오늘", "Today"), "+30P") {}
            if (vm.checkedIn) Item(vm.tr("출석 체크", "Check in"), vm.tr("오늘", "Today"), "+10P") {}
            Item(vm.tr("서핑 완료", "Surf completed"), vm.tr("예시 기록", "Example entry"), "+120P") {}
        }
    }
}

@Composable private fun BoardScreen(vm: SurfViewModel) = Page(vm.tr("내 보드", "My board"), vm) {
    var confirm by remember { mutableStateOf(false) }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(vm.tr("보드 업그레이드", "Upgrade board")) },
        text = { Text(vm.tr("체험 포인트 500P를 사용하여 보드를 Lv. ${vm.boardLevel+1}로 올릴까요?", "Spend 500 demo points to upgrade your board to Lv. ${vm.boardLevel+1}?")) },
        confirmButton = { TextButton(onClick = { confirm = false; vm.upgrade() }) { Text(vm.tr("업그레이드", "Upgrade")) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(vm.tr("취소", "Cancel")) } }, containerColor = Card)
    Eyebrow("Your board"); Gap(8); H1("Mint Wave"); Sub("LEVEL ${"%02d".format(vm.boardLevel)}")
    Gap(20); Box(Modifier.fillMaxWidth().height(270.dp).background(Card, RoundedCornerShape(25.dp)), contentAlignment = Alignment.Center) {
        Box(Modifier.width(90.dp).height(220.dp).background(Mint, RoundedCornerShape(48.dp)), contentAlignment = Alignment.Center) { Text("〰\n〰\n〰", color = Bg, textAlign = TextAlign.Center, fontSize = 25.sp) }
    }
    Gap(20); Surface { H2(vm.tr("다음 파도를 향해", "Toward the next wave")); Sub(vm.tr("보드를 한 단계 성장시켜요.", "Take your board to the next level.")); Gap(12); Text("Lv. ${vm.boardLevel+1}   ·   500P", color = Mint, fontWeight = FontWeight.Bold); Sub(vm.tr("보유 포인트  ${"%,d".format(vm.points)}P", "Balance  ${"%,d".format(vm.points)}P")) }
    Gap(24); Primary(vm.tr("500P로 업그레이드", "Upgrade for 500P")) { confirm = true }
}

@Composable private fun Ranking(vm: SurfViewModel) = Page(vm.tr("랭킹", "Ranking"), vm) {
    Eyebrow("Weekly leaderboard"); Gap(8); H1(vm.tr("이번 주의 서퍼", "Surfers this week")); Sub(vm.tr("주간 활동 포인트 · 예시 순위", "Weekly activity points · example ranking"))
    Gap(22); Tabs(listOf(vm.tr("전체", "All"), vm.tr("내 크루", "My crew")), vm.rankingTab, vm::chooseRankingTab)
    Gap(26); listOf("1  Neo          2,140P", "2  Jin             1,870P", "3  Mina         1,730P", "4  Sora          1,650P", "5  Leo            1,520P", "6  Yuna          1,410P").forEach { s -> Item(s, vm.tr("이번 주 활동", "This week's activity"), "☆") {} }
    Gap(16); Surface { H2(vm.tr("24  Wave Rider  ·  나", "24  Wave Rider  ·  Me")); Text("480P", color = Mint) }
}

@Composable private fun Profile(vm: SurfViewModel, openUrl: (String) -> Unit) = Page(vm.tr("마이 SurfX", "My SurfX"), vm) {
    var languageDialog by remember { mutableStateOf(false) }
    if (languageDialog) AlertDialog(onDismissRequest = { languageDialog = false },
        title = { Text("Language / 언어") },
        text = { Column {
            listOf("ko" to "한국어", "en" to "English").forEach { (code, label) ->
                Row(Modifier.fillMaxWidth().clickable { vm.chooseLanguage(code); languageDialog = false }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = vm.language == code, onClick = { vm.chooseLanguage(code); languageDialog = false })
                    Text(label, color = White)
                }
            }
        } },
        confirmButton = { TextButton(onClick = { languageDialog = false }) { Text(vm.tr("닫기", "Close")) } }, containerColor = Card)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(56.dp).background(CardAlt, RoundedCornerShape(50)), contentAlignment = Alignment.Center) { Text("W", color = Mint, fontSize = 20.sp) }
        Spacer(Modifier.width(12.dp)); Column { H2("Wave Rider"); Sub(vm.tr("@wave.rider · 체험 프로필", "@wave.rider · demo profile")) }
    }
    Gap(24); Surface { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(vm.tr("18\n완료한 미션", "18\nmissions done"), color = White); Text(vm.tr("7일\n최고 연속 출석", "7 days\nbest streak"), color = White); Text(vm.tr("${vm.boardLevel}\n보드 레벨", "${vm.boardLevel}\nboard level"), color = White) } }
    Gap(22); H2(vm.tr("나의 배지", "My badges")); Gap(8); Item(vm.tr("첫 서핑 · 꾸준한 도전 · 퀴즈 마스터", "First surf · Steady progress · Quiz master"), vm.tr("예시 배지", "Example badges")) {}
    Gap(22); Item("Language / 언어", if (vm.language == "en") "English" else "한국어") { languageDialog = true }
    Item(vm.tr("알림 설정", "Notifications"), vm.tr("체험 모드", "Demo mode")) { vm.showNotice(vm.tr("알림 기능은 준비 중이에요", "Notifications are coming soon")) }
    Item(vm.tr("계정 및 보안", "Account and security"), vm.tr("로컬 체험 데이터", "Local demo data")) { vm.showNotice(vm.tr("계정 연동은 준비 중이에요", "Account linking is coming soon")) }
    Item(vm.tr("개인정보 처리방침", "Privacy policy"), "neocode.cloud") { openUrl("https://neocode.cloud/private_privacy.html") }
    Item(vm.tr("서비스 약관", "Terms of service"), "neocode.cloud") { openUrl("https://neocode.cloud/term_of_service.html") }
    Gap(18); Sub(vm.tr("SurfX 1.0.7 · 내부 테스트용 체험 앱", "SurfX 1.0.7 · Internal test demo")); Gap(10)
    Secondary(vm.tr("시작 화면으로", "Back to welcome")) { vm.logout() }
    Gap(12); Image(painterResource(R.drawable.neocode_logo), "neocode", Modifier.height(28.dp))
}
