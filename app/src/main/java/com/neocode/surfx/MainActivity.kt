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
        if (vm.adRewardDone) { vm.showNotice("오늘의 추가 보상은 이미 받았어요"); return }
        if (!UserMessagingPlatform.getConsentInformation(this).canRequestAds()) { vm.showNotice("광고를 불러올 수 없어요"); return }
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { ad.show(this@MainActivity) { vm.rewardAd() } }
            override fun onAdFailedToLoad(error: LoadAdError) { vm.showNotice("광고를 불러오지 못했어요. 다시 시도해 주세요") }
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
    if (exitDialog) AlertDialog(onDismissRequest = { exitDialog = false }, title = { Text("SurfX 종료", color = White) },
        text = { Column { Text("정말 종료하시겠습니까?", color = Muted); Gap(10); (activity as MainActivity).exitNativeAd?.let { ExitNativeAd(it) } } },
        confirmButton = { TextButton(onClick = { activity.finish() }) { Text("확인") } },
        dismissButton = { TextButton(onClick = { exitDialog = false }) { Text("취소") } },
        containerColor = Card)
    if (updateMessage.isNotEmpty()) AlertDialog(onDismissRequest = clearUpdate,
        title = { Text("업데이트 안내") }, text = { Text(updateMessage) },
        confirmButton = { TextButton(onClick = { clearUpdate(); openUrl("https://play.google.com/store/apps/details?id=com.neocode.surfx") }) { Text("스토어 열기") } },
        dismissButton = { TextButton(onClick = clearUpdate) { Text("나중에") } }, containerColor = Card)
    if (vm.notice.isNotEmpty()) AlertDialog(onDismissRequest = vm::clearNotice,
        text = { Text(vm.notice) }, confirmButton = { TextButton(onClick = vm::clearNotice) { Text("확인") } }, containerColor = Card)

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
        listOf(Triple("⌂", "홈", Screen.Home), Triple("▣", "미션", Screen.Missions), Triple("♧", "크루", Screen.Crew), Triple("▤", "지갑", Screen.Wallet)).forEach { (icon,label,target) ->
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
        Text("작은 도전,\n새로운 파도.", color = White, textAlign = TextAlign.Center, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold)
        Gap(14); Sub("매일 배우고, 함께 도전하며\n나만의 포인트를 모아요.")
        Spacer(Modifier.weight(1f)); Primary("체험 시작하기", vm::enter); Gap(10)
        Text("이 앱의 포인트와 활동은 이 기기에만 저장되는 체험 데이터입니다.", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center)
        Gap(12); Row {
            Text("서비스 이용약관", Modifier.clickable { openUrl("https://neocode.cloud/term_of_service.html") }, color = Muted, fontSize = 11.sp)
            Text("  ·  ", color = Muted)
            Text("개인정보 처리방침", Modifier.clickable { openUrl("https://neocode.cloud/private_privacy.html") }, color = Muted, fontSize = 11.sp)
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
        Sub("내 포인트"); Row(Modifier.clickable { vm.go(Screen.Wallet) }, verticalAlignment = Alignment.Bottom) {
            Text("${"%,d".format(vm.points)}", color = White, fontSize = 30.sp, fontWeight = FontWeight.Bold); Text(" P  ›", color = Muted, fontSize = 16.sp)
        }
        Hero(225)
        Text(if (vm.surfingSince == 0L) "오늘의 파도를 시작해요" else "포인트가 쌓이고 있어요", Modifier.fillMaxWidth(), color = White, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
        Gap(4); Text(if (vm.surfingSince == 0L) "24시간 서핑하고 최대 120P" else "서핑 진행 중 · 체험 기록", Modifier.fillMaxWidth(), color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
        Gap(18); if (vm.surfingSince == 0L) Primary("〰  서핑 시작", vm::startSurf)
            else if (vm.surfProgress() >= 1f) Primary("서핑 완료 · 120P 받기", vm::claimSurf)
            else Primary("＋  미션으로 더 모으기") { vm.go(Screen.Missions) }
        Gap(20)
        if (vm.surfingSince != 0L) {
            Surface { H2("이번 서핑"); Gap(10); LinearProgressIndicator(progress = { vm.surfProgress() }, modifier = Modifier.fillMaxWidth(), color = Mint, trackColor = CardAlt); Gap(7); Sub("적립 예정 ${(vm.surfProgress()*120).toInt()} / 120 P · 완료 후 받을 수 있어요") }
        } else {
            H2("오늘의 작은 도전"); Gap(10)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeMission("♧", "출석 체크", "오늘도 만나서 반가워요", "+10P", Modifier.weight(1f)) { vm.go(Screen.Checkin) }
                HomeMission("▣", "오늘의 퀴즈", "한 문제로 가볍게", "+20P", Modifier.weight(1f)) { vm.go(Screen.Quiz) }
            }
        }
        Gap(8); Item("나의 Mint Wave", "Lv. ${vm.boardLevel} · 다음 파도를 준비해요") { vm.go(Screen.Board) }
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

@Composable private fun Checkin(vm: SurfViewModel) = Page("출석 체크", vm) {
    Eyebrow("A little, every day"); Gap(24)
    Text("🎁", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 62.sp); Gap(18)
    Text("매일 만나는 보상", Modifier.fillMaxWidth(), color = White, textAlign = TextAlign.Center, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    Gap(8); Text("오늘도 함께 파도를 이어가요", Modifier.fillMaxWidth(), color = Muted, textAlign = TextAlign.Center)
    Gap(32); H2("7일 출석 챌린지"); Gap(12)
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf("1일차\n✓\n+10P", "2일차\n✓\n+10P", "오늘\n🎁\n+10P", "4일차\n+15P").forEachIndexed { i, s ->
            Box(Modifier.weight(1f).height(105.dp).background(if (i == 2) Mint else Card, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                Text(s, color = if (i == 2) Bg else White, textAlign = TextAlign.Center, fontSize = 13.sp, lineHeight = 22.sp)
            }
        }
    }
    Gap(10); Surface { H2("7일 완주 보너스  +50P"); Sub("매일 한 번, 작은 도전을 이어가 보세요.") }
    Gap(35); Primary(if (vm.checkedIn) "오늘 출석 완료" else "오늘의 10P 받기", vm::checkin)
}

@Composable private fun Missions(vm: SurfViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("미션", Modifier.fillMaxWidth(), color = White, textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Gap(28); Eyebrow("One step, one wave"); Gap(8); H1("오늘의 작은 도전"); Sub("가볍게 배우고, 차곡차곡 모아요.")
        Gap(24); Tabs(listOf("오늘", "진행 중", "완료"), vm.missionTab, vm::chooseMissionTab)
        Gap(15); Surface { Eyebrow("오늘의 추천"); Gap(10); H2("Web3, 처음부터 가볍게"); Sub("2분 학습 · +30P"); Gap(12); Secondary("학습하러 가기") { vm.go(Screen.Lesson) } }
        Gap(24); H2("참여할 수 있는 미션")
        when(vm.missionTab) {
            0 -> { Item("오늘의 퀴즈", "1문제 · 약 30초", "+20P") { vm.go(Screen.Quiz) }; Item("새로운 프로젝트 만나기", "소개 읽기 · 약 2분", "+50P") { vm.go(Screen.Lesson) }; Item("오늘도 출석", "3일차 출석 받기", "+10P") { vm.go(Screen.Checkin) } }
            1 -> Item("데일리 서핑", "서핑 상태 확인", "›") { vm.go(Screen.Home) }
            else -> { if (vm.quizDone) Item("오늘의 퀴즈", "오늘 완료", "✓") { vm.go(Screen.Result) }; if (vm.lessonDone) Item("Web3 학습", "오늘 완료", "✓") { vm.go(Screen.Lesson) }; if (vm.checkedIn) Item("출석 체크", "오늘 완료", "✓") { vm.go(Screen.Checkin) } }
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

@Composable private fun Lesson(vm: SurfViewModel) = Page("가볍게 배우기", vm) {
    Eyebrow("Web3 starter"); Gap(8); H1("블록체인,\n함께 기록하는 방법"); Gap(24)
    Box(Modifier.fillMaxWidth().height(180.dp).background(CardAlt, RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) { Text("▣    ◈    ♧", color = Mint, fontSize = 42.sp) }
    Gap(22); Sub("블록체인은 거래 기록을 여러 참여자가 함께 보관하고 확인하는 기술이에요.")
    Gap(22); listOf("한 곳에 기록을 맡기지 않아요.", "기록의 변경 여부를 함께 확인해요.", "활동을 더 신뢰할 수 있게 도와줘요.").forEach { Item("✓  $it", "", "") {} }
    Gap(30); Primary(if (vm.lessonDone) "퀴즈로 이어가기" else "학습 완료 · 30P 받기", vm::completeLesson)
}

@Composable private fun Quiz(vm: SurfViewModel) = Page("오늘의 퀴즈", vm) {
    Eyebrow("Daily quiz"); Gap(8); H1("오늘의 한 문제"); Gap(25)
    LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth(), color = Mint, trackColor = CardAlt)
    Gap(32); H2("블록체인에서 거래 기록을\n함께 보관하고 확인하는\n주체는 누구일까요?")
    Gap(27); listOf("한 사람과 서버", "네트워크의 여러 참여자", "서비스 운영자 한 명", "특정 기기 한 대").forEachIndexed { i, s ->
        Row(Modifier.fillMaxWidth().padding(bottom = 9.dp).background(if (vm.selectedAnswer == i) CardAlt else Card, RoundedCornerShape(13.dp)).border(1.dp, if (vm.selectedAnswer == i) Mint else Color.Transparent, RoundedCornerShape(13.dp)).clickable { vm.answer(i) }.padding(17.dp)) {
            Text("${'A'+i}     $s", color = White, fontSize = 14.sp)
            if (vm.selectedAnswer == i) Text("  ✓", color = Mint)
        }
    }
    Gap(30); Primary("정답 확인", vm::submitQuiz)
}

@Composable private fun Result(vm: SurfViewModel) = Page("보상", vm) {
    val activity = LocalContext.current as MainActivity
    Spacer(Modifier.height(42.dp)); Text("✦", Modifier.fillMaxWidth(), color = Mint, textAlign = TextAlign.Center, fontSize = 92.sp)
    Gap(16); Text("정답이에요!", Modifier.fillMaxWidth(), color = White, textAlign = TextAlign.Center, fontSize = 27.sp, fontWeight = FontWeight.Bold)
    Gap(12); Text("체험 포인트", Modifier.fillMaxWidth(), color = Muted, textAlign = TextAlign.Center)
    Text("+20 P", Modifier.fillMaxWidth(), color = Mint, textAlign = TextAlign.Center, fontSize = 39.sp, fontWeight = FontWeight.Bold)
    Gap(22); Surface { H2("현재 보유 포인트"); Text("${"%,d".format(vm.points)} P", color = White, fontSize = 20.sp) }
    Gap(28); Primary("확인") { vm.go(Screen.Missions) }; Gap(10); Secondary("내 지갑 보기") { vm.go(Screen.Wallet) }
    Gap(10); Secondary(if (vm.adRewardDone) "오늘의 광고 보상 완료" else "광고 보고 추가 체험 20P 받기") { activity.showRewardedAd() }
}

@Composable private fun Crew(vm: SurfViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("크루", Modifier.fillMaxWidth(), color = White, textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Gap(28); Eyebrow("Your crew"); Gap(8); H1("함께 타는 파도"); Sub("작은 도전도 함께하면 더 멀리.")
        Gap(22); Surface { Sub("내 크루"); Text("8 명", color = White, fontSize = 30.sp, fontWeight = FontWeight.Bold); Sub("오늘 5명 참여"); Gap(10); Text("●  ●  ●  ●  +4", color = Mint, fontSize = 21.sp) }
        Gap(14); Primary("＋  친구 초대") { vm.go(Screen.Invite) }
        Gap(26); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { H2("크루 활동"); Text("랭킹  ›", Modifier.clickable { vm.go(Screen.Ranking) }, color = Mint) }
        Item("Wave Rider", "오늘도 새로운 파도에 도전", "서핑 중") {}
        Item("Neo", "3일 연속 출석", "+10P") {}
        Item("Jin", "오늘의 퀴즈 완료", "+20P") {}
    }
}

@Composable private fun Invite(vm: SurfViewModel) = Page("친구 초대", vm) {
    val ctx = LocalContext.current
    Eyebrow("Make waves together"); Gap(8); H1("좋은 파도는\n함께 타요."); Gap(12); Sub("친구와 SurfX를 시작하고\n함께 성장해 보세요.")
    Gap(35); Surface {
        Sub("내 초대 코드"); Gap(8); Text("SURFX24", color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Gap(14); Secondary("코드 복사") { (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("SurfX invite code", "SURFX24")); vm.showNotice("초대 코드를 복사했어요") }
    }
    Gap(12); Surface { H2("친구 초대 보상  +100P"); Gap(5); Sub("친구가 첫 서핑을 완료하면 지급하는 예시 정책입니다. 체험 앱에서는 자동 지급되지 않아요.") }
    Gap(30); Primary("초대 링크 공유") {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "SurfX Protocol 체험 코드: SURFX24")
        ctx.startActivity(Intent.createChooser(send, "친구에게 공유"))
    }
}

@Composable private fun Wallet(vm: SurfViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("지갑", Modifier.fillMaxWidth(), color = White, textAlign = TextAlign.Center, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Gap(30); Eyebrow("My points"); Gap(10); Sub("내 포인트"); Text("${"%,d".format(vm.points)} P", color = White, fontSize = 36.sp, fontWeight = FontWeight.Bold); Sub("나의 도전이 차곡차곡 쌓이는 곳")
        Gap(22); Text("체험 포인트 · 현금이나 토큰으로 교환할 수 없어요", color = Muted, fontSize = 12.sp)
        Gap(35); H2("포인트 내역"); Gap(12); Tabs(listOf("전체", "적립", "사용"), vm.walletTab, vm::chooseWalletTab)
        Gap(14); if (vm.walletTab != 1) Item("보드 업그레이드", "최근 활동", "-500P") {}
        if (vm.walletTab != 2) {
            if (vm.quizDone) Item("오늘의 퀴즈", "오늘", "+20P") {}
            if (vm.lessonDone) Item("Web3 학습", "오늘", "+30P") {}
            if (vm.checkedIn) Item("출석 체크", "오늘", "+10P") {}
            Item("서핑 완료", "예시 기록", "+120P") {}
        }
    }
}

@Composable private fun BoardScreen(vm: SurfViewModel) = Page("내 보드", vm) {
    var confirm by remember { mutableStateOf(false) }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("보드 업그레이드") },
        text = { Text("체험 포인트 500P를 사용하여 보드를 Lv. ${vm.boardLevel+1}로 올릴까요?") },
        confirmButton = { TextButton(onClick = { confirm = false; vm.upgrade() }) { Text("업그레이드") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("취소") } }, containerColor = Card)
    Eyebrow("Your board"); Gap(8); H1("Mint Wave"); Sub("LEVEL ${"%02d".format(vm.boardLevel)}")
    Gap(20); Box(Modifier.fillMaxWidth().height(270.dp).background(Card, RoundedCornerShape(25.dp)), contentAlignment = Alignment.Center) {
        Box(Modifier.width(90.dp).height(220.dp).background(Mint, RoundedCornerShape(48.dp)), contentAlignment = Alignment.Center) { Text("〰\n〰\n〰", color = Bg, textAlign = TextAlign.Center, fontSize = 25.sp) }
    }
    Gap(20); Surface { H2("다음 파도를 향해"); Sub("보드를 한 단계 성장시켜요."); Gap(12); Text("Lv. ${vm.boardLevel+1}   ·   500P", color = Mint, fontWeight = FontWeight.Bold); Sub("보유 포인트  ${"%,d".format(vm.points)}P") }
    Gap(24); Primary("500P로 업그레이드") { confirm = true }
}

@Composable private fun Ranking(vm: SurfViewModel) = Page("랭킹", vm) {
    Eyebrow("Weekly leaderboard"); Gap(8); H1("이번 주의 서퍼"); Sub("주간 활동 포인트 · 예시 순위")
    Gap(22); Tabs(listOf("전체", "내 크루"), vm.rankingTab, vm::chooseRankingTab)
    Gap(26); listOf("1  Neo          2,140P", "2  Jin             1,870P", "3  Mina         1,730P", "4  Sora          1,650P", "5  Leo            1,520P", "6  Yuna          1,410P").forEach { s -> Item(s, "이번 주 활동", "☆") {} }
    Gap(16); Surface { H2("24  Wave Rider  ·  나"); Text("480P", color = Mint) }
}

@Composable private fun Profile(vm: SurfViewModel, openUrl: (String) -> Unit) = Page("마이 SurfX", vm) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(56.dp).background(CardAlt, RoundedCornerShape(50)), contentAlignment = Alignment.Center) { Text("W", color = Mint, fontSize = 20.sp) }
        Spacer(Modifier.width(12.dp)); Column { H2("Wave Rider"); Sub("@wave.rider · 체험 프로필") }
    }
    Gap(24); Surface { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("18\n완료한 미션", color = White); Text("7일\n최고 연속 출석", color = White); Text("${vm.boardLevel}\n보드 레벨", color = White) } }
    Gap(22); H2("나의 배지"); Gap(8); Item("첫 서핑 · 꾸준한 도전 · 퀴즈 마스터", "예시 배지") {}
    Gap(22); Item("알림 설정", "체험 모드") { vm.showNotice("알림 기능은 준비 중이에요") }
    Item("계정 및 보안", "로컬 체험 데이터") { vm.showNotice("계정 연동은 준비 중이에요") }
    Item("개인정보 처리방침", "neocode.cloud") { openUrl("https://neocode.cloud/private_privacy.html") }
    Item("서비스 약관", "neocode.cloud") { openUrl("https://neocode.cloud/term_of_service.html") }
    Gap(18); Sub("SurfX 1.0.0 · 내부 테스트용 체험 앱"); Gap(10)
    Secondary("시작 화면으로") { vm.logout() }
    Gap(12); Image(painterResource(R.drawable.neocode_logo), "neocode", Modifier.height(28.dp))
}
