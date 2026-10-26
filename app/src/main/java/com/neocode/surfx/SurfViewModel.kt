package com.neocode.surfx

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

enum class Screen { Welcome, Home, Checkin, Missions, Lesson, Quiz, Result, Crew, Invite, Wallet, Board, Ranking, Profile }

/** Local prototype state. No points, identity, or rankings are synchronized with a server. */
class SurfViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("surfx_demo", 0)
    var screen by mutableStateOf(if (prefs.getBoolean("started", false)) Screen.Home else Screen.Welcome)
        private set
    var previous by mutableStateOf(Screen.Home)
        private set
    var points by mutableIntStateOf(prefs.getInt("points", 12450))
        private set
    var boardLevel by mutableIntStateOf(prefs.getInt("board", 3))
        private set
    var surfingSince by mutableStateOf(prefs.getLong("surfing", 0L))
        private set
    var surfClock by mutableStateOf(System.currentTimeMillis())
        private set
    var checkedIn by mutableStateOf(prefs.getString("checkin", "") == today())
        private set
    var quizDone by mutableStateOf(prefs.getString("quiz", "") == today())
        private set
    var lessonDone by mutableStateOf(prefs.getString("lesson", "") == today())
        private set
    var adRewardDone by mutableStateOf(prefs.getString("adreward", "") == today())
        private set
    var selectedAnswer by mutableIntStateOf(-1)
        private set
    var missionTab by mutableIntStateOf(0)
        private set
    var walletTab by mutableIntStateOf(0)
        private set
    var rankingTab by mutableIntStateOf(0)
        private set
    var notice by mutableStateOf("")

    private fun today() = LocalDate.now().toString()
    fun enter() { prefs.edit().putBoolean("started", true).apply(); screen = Screen.Home }
    fun go(next: Screen) { previous = screen; screen = next }
    fun back() {
        screen = when (screen) {
            Screen.Welcome, Screen.Home -> Screen.Home
            Screen.Checkin, Screen.Board, Screen.Profile, Screen.Wallet -> Screen.Home
            Screen.Lesson, Screen.Quiz, Screen.Result -> Screen.Missions
            Screen.Invite, Screen.Ranking -> Screen.Crew
            else -> Screen.Home
        }
    }
    fun startSurf() {
        if (surfingSince != 0L) return
        surfingSince = System.currentTimeMillis()
        prefs.edit().putLong("surfing", surfingSince).apply()
        notice = "서핑을 시작했어요. 체험 기록은 이 기기에 저장됩니다."
    }
    fun tick() { surfClock = System.currentTimeMillis() }
    fun surfProgress(): Float = if (surfingSince == 0L) 0f else ((surfClock - surfingSince).toFloat() / 86_400_000f).coerceIn(0f, 1f)
    fun claimSurf() {
        if (surfingSince == 0L || surfProgress() < 1f) return
        points += 120; surfingSince = 0L
        prefs.edit().putInt("points", points).remove("surfing").apply()
        notice = "서핑 완료 · 체험 포인트 120P"
    }
    fun checkin() {
        if (checkedIn) { notice = "오늘 출석은 이미 완료했어요"; return }
        checkedIn = true
        points += 10
        prefs.edit().putString("checkin", today()).putInt("points", points).apply()
        notice = "체험 포인트 10P를 받았어요"
    }
    fun completeLesson() {
        if (!lessonDone) {
            lessonDone = true; points += 30
            prefs.edit().putString("lesson", today()).putInt("points", points).apply()
            notice = "학습 완료 · 체험 포인트 30P"
        }
        go(Screen.Quiz)
    }
    fun answer(index: Int) { selectedAnswer = index }
    fun submitQuiz() {
        if (selectedAnswer != 1) { notice = if (selectedAnswer < 0) "답을 선택해 주세요" else "다시 생각해 보세요"; return }
        if (!quizDone) {
            quizDone = true; points += 20
            prefs.edit().putString("quiz", today()).putInt("points", points).apply()
        }
        go(Screen.Result)
    }
    fun upgrade() {
        if (points < 500) { notice = "포인트가 부족해요"; return }
        points -= 500; boardLevel++
        prefs.edit().putInt("points", points).putInt("board", boardLevel).apply()
        notice = "보드가 Lv. $boardLevel 로 성장했어요"
    }
    fun rewardAd() {
        if (adRewardDone) { notice = "오늘의 추가 보상은 이미 받았어요"; return }
        adRewardDone = true; points += 20
        prefs.edit().putString("adreward", today()).putInt("points", points).apply()
        notice = "광고 보상 · 체험 포인트 20P"
    }
    fun chooseMissionTab(i: Int) { missionTab = i }
    fun chooseWalletTab(i: Int) { walletTab = i }
    fun chooseRankingTab(i: Int) { rankingTab = i }
    fun clearNotice() { notice = "" }
    fun showNotice(message: String) { notice = message }
    fun logout() { prefs.edit().putBoolean("started", false).apply(); screen = Screen.Welcome }
}
