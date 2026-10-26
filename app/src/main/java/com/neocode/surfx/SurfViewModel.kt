package com.neocode.surfx

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { Welcome, Home, Checkin, Missions, Lesson, QuizBank, Quiz, Result, Crew, Invite, Wallet, Board, Ranking, Profile }

/** Local prototype state. No points, identity, or rankings are synchronized with a server. */
class SurfViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("surfx_demo", 0)
    private val quizRepository = QuizRepository(app)
    var language by mutableStateOf(prefs.getString("language", "ko") ?: "ko")
        private set
    var questions by mutableStateOf<List<QuizQuestion>>(emptyList())
        private set
    var quizError by mutableStateOf(false)
        private set
    var questionId by mutableStateOf<String?>(null)
        private set
    var quizMode by mutableStateOf("daily")
        private set
    var difficultyFilter by mutableStateOf("all")
        private set
    var typeFilter by mutableStateOf("all")
        private set
    var answerRevealed by mutableStateOf(false)
        private set
    var answerCorrect by mutableStateOf(false)
        private set
    var rewardEarned by mutableStateOf(false)
        private set
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
    var selectedAnswer by mutableStateOf<String?>(null)
        private set
    var missionTab by mutableIntStateOf(0)
        private set
    var walletTab by mutableIntStateOf(0)
        private set
    var rankingTab by mutableIntStateOf(0)
        private set
    var notice by mutableStateOf("")

    init { loadQuestions() }

    private fun loadQuestions() {
        val targetLanguage = language
        quizError = false
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { quizRepository.questions(targetLanguage) } }
                .onSuccess { loaded ->
                    if (language == targetLanguage) {
                        questions = loaded
                        if (questionId == null) questionId = if (quizMode == "practice") practiceQuestions().firstOrNull()?.id else dailyQuestion()?.id
                    }
                }
                .onFailure { if (language == targetLanguage) quizError = true }
        }
    }
    fun chooseLanguage(value: String) {
        if (value !in listOf("ko", "en") || value == language) return
        language = value
        prefs.edit().putString("language", value).apply()
        questions = emptyList()
        loadQuestions()
    }
    fun tr(ko: String, en: String) = if (language == "en") en else ko
    fun dailyQuestion(): QuizQuestion? = if (questions.isEmpty()) null else
        questions[Math.floorMod(LocalDate.now().toEpochDay(), questions.size.toLong()).toInt()]
    fun currentQuestion(): QuizQuestion? = questions.firstOrNull { it.id == questionId }
    fun practiceQuestions(): List<QuizQuestion> = questions.filter {
        (difficultyFilter == "all" || it.difficulty == difficultyFilter) &&
            (typeFilter == "all" || it.type == typeFilter)
    }
    fun chooseDifficulty(value: String) { difficultyFilter = value }
    fun chooseType(value: String) { typeFilter = value }
    fun openDailyQuiz() {
        quizMode = "daily"; questionId = dailyQuestion()?.id
        selectedAnswer = null; answerRevealed = false; rewardEarned = false; go(Screen.Quiz)
    }
    fun openPractice() {
        quizMode = "practice"; questionId = practiceQuestions().firstOrNull()?.id
        selectedAnswer = null; answerRevealed = false; rewardEarned = false; go(Screen.Quiz)
    }
    fun nextPractice() {
        val pool = practiceQuestions()
        val index = pool.indexOfFirst { it.id == questionId }
        questionId = pool.getOrNull(index + 1)?.id ?: pool.firstOrNull()?.id
        selectedAnswer = null; answerRevealed = false
    }

    private fun today() = LocalDate.now().toString()
    fun enter() { prefs.edit().putBoolean("started", true).apply(); screen = Screen.Home }
    fun go(next: Screen) { previous = screen; screen = next }
    fun back() {
        screen = when (screen) {
            Screen.Welcome, Screen.Home -> Screen.Home
            Screen.Checkin, Screen.Board, Screen.Profile, Screen.Wallet -> Screen.Home
            Screen.Lesson, Screen.QuizBank, Screen.Result -> Screen.Missions
            Screen.Quiz -> if (quizMode == "practice") Screen.QuizBank else Screen.Missions
            Screen.Invite, Screen.Ranking -> Screen.Crew
            else -> Screen.Home
        }
    }
    fun startSurf() {
        if (surfingSince != 0L) return
        surfingSince = System.currentTimeMillis()
        prefs.edit().putLong("surfing", surfingSince).apply()
        notice = tr("서핑을 시작했어요. 체험 기록은 이 기기에 저장됩니다.", "Surfing started. Your demo activity is stored on this device.")
    }
    fun tick() { surfClock = System.currentTimeMillis() }
    fun surfProgress(): Float = if (surfingSince == 0L) 0f else ((surfClock - surfingSince).toFloat() / 86_400_000f).coerceIn(0f, 1f)
    fun claimSurf() {
        if (surfingSince == 0L || surfProgress() < 1f) return
        points += 120; surfingSince = 0L
        prefs.edit().putInt("points", points).remove("surfing").apply()
        notice = tr("서핑 완료 · 체험 포인트 120P", "Surf complete · 120 demo points")
    }
    fun checkin() {
        if (checkedIn) { notice = tr("오늘 출석은 이미 완료했어요", "You already checked in today"); return }
        checkedIn = true
        points += 10
        prefs.edit().putString("checkin", today()).putInt("points", points).apply()
        notice = tr("체험 포인트 10P를 받았어요", "You earned 10 demo points")
    }
    fun completeLesson() {
        if (!lessonDone) {
            lessonDone = true; points += 30
            prefs.edit().putString("lesson", today()).putInt("points", points).apply()
            notice = tr("학습 완료 · 체험 포인트 30P", "Lesson complete · 30 demo points")
        }
        openDailyQuiz()
    }
    fun answer(id: String) { if (!answerRevealed) selectedAnswer = id }
    fun submitQuiz() {
        val question = currentQuestion() ?: return
        if (selectedAnswer == null) { notice = tr("답을 선택해 주세요", "Choose an answer"); return }
        answerCorrect = selectedAnswer == question.correctOptionId
        answerRevealed = true
        if (quizMode == "daily" && answerCorrect && !quizDone) {
            quizDone = true; points += 20
            rewardEarned = true
            prefs.edit().putString("quiz", today()).putInt("points", points).apply()
        }
    }
    fun upgrade() {
        if (points < 500) { notice = tr("포인트가 부족해요", "Not enough points"); return }
        points -= 500; boardLevel++
        prefs.edit().putInt("points", points).putInt("board", boardLevel).apply()
        notice = tr("보드가 Lv. $boardLevel 로 성장했어요", "Your board reached Lv. $boardLevel")
    }
    fun rewardAd() {
        if (adRewardDone) { notice = tr("오늘의 추가 보상은 이미 받았어요", "You already claimed today's bonus"); return }
        adRewardDone = true; points += 20
        prefs.edit().putString("adreward", today()).putInt("points", points).apply()
        notice = tr("광고 보상 · 체험 포인트 20P", "Ad reward · 20 demo points")
    }
    fun chooseMissionTab(i: Int) { missionTab = i }
    fun chooseWalletTab(i: Int) { walletTab = i }
    fun chooseRankingTab(i: Int) { rankingTab = i }
    fun clearNotice() { notice = "" }
    fun showNotice(message: String) { notice = message }
    fun logout() { prefs.edit().putBoolean("started", false).apply(); screen = Screen.Welcome }
}
