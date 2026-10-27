<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'

const asset = (name) => `${import.meta.env.BASE_URL}${name}`
const whitepaperUrl = 'https://whitepaper.surfx-protocol.com/'
const lang = ref(localStorage.getItem('surfx-language') === 'en' ? 'en' : 'ko')
const route = ref(window.location.hash.startsWith('#/whitepaper') ? 'whitepaper' : 'home')
const menuOpen = ref(false)
const openFaq = ref(0)

const content = {
  ko: {
    nav: ['소개', '경험', '앱 미리보기', '백서'],
    explore: 'SurfX 알아보기',
    eyebrow: 'LEARN · EARN · SURF',
    hero1: '작은 도전,', hero2: '새로운 파도.',
    heroBody: '매일 배우고, 가볍게 도전하고, 나만의 성장을 기록하세요. SurfX와 함께 오늘의 파도를 시작해 보세요.',
    heroCta: '경험 살펴보기', paperCta: '백서 읽기',
    heroNote: 'SurfX 모바일 앱 컨셉트',
    strip: ['매일의 출석', '한 문제의 배움', '함께하는 크루', '나만의 보드'],
    aboutKicker: 'THE SURFX EXPERIENCE', aboutTitle: '매일 돌아오고 싶은\n작은 경험들',
    aboutBody: 'SurfX는 학습, 미션, 커뮤니티를 하나의 흐름으로 연결합니다. 하루에 몇 분씩 쌓아가는 활동이 더 큰 여정의 시작이 됩니다.',
    steps: [
      { n: '01', icon: '◷', title: '매일 접속하기', body: '출석 체크와 24시간 서핑으로 나만의 리듬을 만들어요.' },
      { n: '02', icon: '✳', title: '배우고 도전하기', body: '블록체인 퀴즈와 짧은 학습으로 새로운 개념을 익혀요.' },
      { n: '03', icon: '↗', title: '함께 성장하기', body: '크루와 연결하고 포인트를 모아 나만의 보드를 키워요.' },
    ],
    featureKicker: 'ONE DAY, ONE WAVE', featureTitle: '하루를 더 흥미롭게\n만드는 방법',
    featureBody: '출석, 서핑, 퀴즈와 미션. 앱 안의 작은 행동이 한눈에 보이는 진행 상황으로 이어집니다.',
    featureItems: ['일일 출석과 연속 기록', '배우면서 푸는 블록체인 퀴즈', '친구와 함께하는 크루', '포인트로 성장하는 보드'],
    demo: '현재 앱의 포인트와 보상은 기기에 저장되는 체험 데이터입니다.',
    appKicker: 'IN YOUR HANDS', appTitle: '파도를 손안에.',
    appBody: '오늘의 서핑을 시작하고, 미션을 고르고, 크루와 함께하세요. SurfX의 모바일 경험을 미리 만나보세요.',
    screens: ['오늘의 홈', '미션과 학습', '함께하는 크루'],
    faqKicker: 'GOOD TO KNOW', faqTitle: '자주 묻는 질문',
    faqs: [
      { q: 'SurfX는 어떤 서비스인가요?', a: 'SurfX는 매일 출석, 서핑, 학습, 퀴즈, 크루 활동을 통해 참여 경험을 제공하는 모바일 앱 프로젝트입니다.' },
      { q: '포인트를 현금이나 토큰으로 바꿀 수 있나요?', a: '현재 앱의 포인트와 보상은 기기 안에서 동작하는 체험 데이터입니다. 현금화, 토큰 교환, 온체인 지급 기능은 제공하지 않습니다.' },
      { q: '한국어와 영어를 지원하나요?', a: '네. 웹사이트와 모바일 앱 모두 한국어와 영어를 지원합니다. 앱의 퀴즈 문제도 두 언어로 제공됩니다.' },
      { q: '백서에는 무엇이 담겨 있나요?', a: 'SurfX의 경험 설계, 현재 앱 기능, 데이터 범위와 향후 구현 시 고려할 사항을 정리했습니다.' },
    ],
    ctaKicker: 'READY TO RIDE?', ctaTitle: '다음 파도를\n함께 만들어가요.', ctaBody: 'SurfX가 그리고 있는 경험과 현재 구현 범위를 백서에서 자세히 살펴보세요.',
    footerDesc: '매일 배우고, 도전하고, 나만의 파도를 만드는 모바일 경험.',
    top: '맨 위로',
    paper: {
      label: 'SURFX DOCUMENTATION', title: 'SurfX 백서', intro: 'SurfX의 제품 경험과 현재 구현 범위를 설명하는 문서입니다. 이 문서는 앱의 상태를 기준으로 작성된 제품 개요이며, 투자 제안이나 토큰 발행 계획이 아닙니다.',
      sections: [
        { id: 'overview', title: '01. 개요', paragraphs: ['SurfX는 매일 반복할 수 있는 작은 참여 경험을 중심에 둔 모바일 앱 프로젝트입니다. 서핑 타이머, 출석 체크, 학습과 퀴즈, 크루와 보드 성장을 하나의 여정으로 묶었습니다.', '목표는 복잡한 개념을 가볍게 배우고, 일상의 활동을 시각적인 성장 경험으로 연결하는 것입니다.'] },
        { id: 'experience', title: '02. 사용자 경험', paragraphs: ['사용자는 하루의 서핑을 시작하고, 출석을 완료하고, 블록체인 학습과 퀴즈에 참여할 수 있습니다. 미션, 크루, 랭킹, 보드 화면은 일일 참여와 진행 상황을 한눈에 보여줍니다.', '앱은 한국어와 영어를 지원하며, 언어별 블록체인 퀴즈 문제 은행을 내장하고 있습니다.'] },
        { id: 'rewards', title: '03. 포인트와 보드', paragraphs: ['현재 앱의 포인트, 출석, 학습, 퀴즈 결과와 보드 레벨은 기기에 저장되는 데모 상태입니다. 일일 보상은 같은 날 반복 수령되지 않도록 설계되어 있습니다.', '포인트는 앱 안에서 보드 성장 체험에 사용됩니다. 실제 가치, 현금화, 토큰 교환 또는 블록체인 자산을 의미하지 않습니다.'] },
        { id: 'architecture', title: '04. 현재 구현 범위', paragraphs: ['Android 앱은 Jetpack Compose로 구현되어 있으며 환영, 홈, 출석, 미션, 학습, 퀴즈, 크루, 초대, 지갑, 보드, 랭킹, 프로필 화면을 포함합니다.', '현재 서버 계정, 실시간 보상 동기화, 토큰 지갑 연결, 온체인 보상 지급은 구현되지 않았습니다. 이 기능을 제공하려면 인증, 데이터 저장, 부정 사용 방지와 운영 정책을 별도로 설계해야 합니다.'] },
        { id: 'principles', title: '05. 제품 원칙', paragraphs: ['짧고 명확한 일일 활동, 이해하기 쉬운 진행 상황, 두 언어에서 일관된 경험을 제품 설계의 기준으로 삼습니다.', '보상이나 수치가 예시인 경우 이를 명확히 표시하고, 실제 기능의 범위를 과장하지 않습니다.'] },
        { id: 'faq', title: '06. 안내', paragraphs: ['이 문서는 SurfX 모바일 앱과 웹사이트의 제품 소개를 위한 초안입니다. 기능, 화면, 정책은 개발과 검증 과정에서 달라질 수 있습니다.', '문의와 협업 관련 정보는 프로젝트 GitHub 저장소를 통해 확인할 수 있습니다.'] },
      ],
    },
  },
  en: {
    nav: ['About', 'Experience', 'App preview', 'Whitepaper'], explore: 'Explore SurfX',
    eyebrow: 'LEARN · EARN · SURF', hero1: 'Small steps.', hero2: 'New waves.',
    heroBody: 'Learn something new, take on a simple challenge, and see your progress grow. Start your wave with SurfX today.',
    heroCta: 'Explore the experience', paperCta: 'Read the whitepaper', heroNote: 'SurfX mobile app concept',
    strip: ['Daily check-ins', 'A little learning', 'A crew to share with', 'Your own board'],
    aboutKicker: 'THE SURFX EXPERIENCE', aboutTitle: 'Small experiences\nworth returning to',
    aboutBody: 'SurfX brings learning, missions, and community into one flow. A few minutes of activity each day can be the start of a bigger journey.',
    steps: [
      { n: '01', icon: '◷', title: 'Show up daily', body: 'Build your own rhythm with check-ins and a 24-hour surf.' },
      { n: '02', icon: '✳', title: 'Learn and play', body: 'Explore new ideas through short lessons and blockchain quizzes.' },
      { n: '03', icon: '↗', title: 'Grow together', body: 'Connect with a crew, collect points, and level up your board.' },
    ],
    featureKicker: 'ONE DAY, ONE WAVE', featureTitle: 'A more rewarding\ndaily rhythm',
    featureBody: 'Check-ins, surfing, quizzes, and missions turn small actions into visible progress inside the app.',
    featureItems: ['Daily check-ins and streaks', 'Learn through blockchain quizzes', 'A crew to ride with', 'A board that grows with points'],
    demo: 'Points and rewards in the current app are demo data stored on your device.',
    appKicker: 'IN YOUR HANDS', appTitle: 'A wave in your pocket.',
    appBody: 'Start today’s surf, choose a mission, and connect with your crew. Take a look at the SurfX mobile experience.',
    screens: ['Your daily home', 'Missions and learning', 'Your crew'],
    faqKicker: 'GOOD TO KNOW', faqTitle: 'Frequently asked questions',
    faqs: [
      { q: 'What is SurfX?', a: 'SurfX is a mobile app project built around daily check-ins, surfing, learning, quizzes, and crew activities.' },
      { q: 'Can points be redeemed for cash or tokens?', a: 'No. Points and rewards in the current app are demo data stored on your device. Cash redemption, token swaps, and on-chain payouts are not available.' },
      { q: 'Is SurfX available in Korean and English?', a: 'Yes. Both the website and mobile app support Korean and English. The app also includes quiz banks in both languages.' },
      { q: 'What is in the whitepaper?', a: 'It outlines the SurfX experience, current app features, data boundaries, and considerations for future implementation.' },
    ],
    ctaKicker: 'READY TO RIDE?', ctaTitle: 'Let’s make the\nnext wave together.', ctaBody: 'Explore the SurfX product experience and its current scope in the whitepaper.',
    footerDesc: 'A mobile experience for learning, exploring, and making your own wave every day.', top: 'Back to top',
    paper: {
      label: 'SURFX DOCUMENTATION', title: 'SurfX Whitepaper', intro: 'A guide to the SurfX product experience and its current implementation. This is a product overview based on the app’s current state, not an investment offer or token issuance plan.',
      sections: [
        { id: 'overview', title: '01. Overview', paragraphs: ['SurfX is a mobile app project built around small, repeatable daily experiences. It brings a surf timer, check-ins, learning and quizzes, crews, and board progression into one journey.', 'Its goal is to make new concepts approachable and turn everyday participation into a visible sense of progress.'] },
        { id: 'experience', title: '02. User experience', paragraphs: ['Users can start a daily surf, complete a check-in, and take part in blockchain lessons and quizzes. Missions, crew, ranking, and board screens bring activities and progress together.', 'The app supports Korean and English and bundles a blockchain quiz bank for each language.'] },
        { id: 'rewards', title: '03. Points and boards', paragraphs: ['Points, check-ins, lesson and quiz outcomes, and board levels are currently demo state saved on the device. Daily rewards are designed to prevent repeated claims on the same day.', 'Points are used for board progression inside the app. They do not represent real value, cash redemption, token conversion, or blockchain assets.'] },
        { id: 'architecture', title: '04. Current implementation', paragraphs: ['The Android app is built with Jetpack Compose and includes welcome, home, check-in, missions, learning, quiz, crew, invite, wallet, board, ranking, and profile screens.', 'Server accounts, live reward synchronization, token wallet connections, and on-chain payouts are not implemented. Those features would require separate authentication, storage, abuse prevention, and operating policies.'] },
        { id: 'principles', title: '05. Product principles', paragraphs: ['Short, clear daily actions, understandable progress, and a consistent experience across both languages guide the product design.', 'Example rewards and figures should be identified clearly, and the scope of live functionality should be described accurately.'] },
        { id: 'faq', title: '06. Notes', paragraphs: ['This document introduces the SurfX mobile app and website. Features, screens, and policies may change during development and validation.', 'For project information or collaboration, visit the GitHub repository.'] },
      ],
    },
  },
}

const t = computed(() => content[lang.value])
function changeLanguage(value) { lang.value = value; localStorage.setItem('surfx-language', value) }
function syncRoute() { route.value = window.location.hash.startsWith('#/whitepaper') ? 'whitepaper' : 'home'; menuOpen.value = false; window.scrollTo({ top: 0, behavior: 'instant' }) }
function navigate(target) { window.location.hash = target; syncRoute() }
function goTo(id) {
  if (route.value !== 'home') { navigate('/'); setTimeout(() => document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' }), 40) }
  else document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
  menuOpen.value = false
}
function scrollPaper(id) { document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' }) }
watch(lang, value => { document.documentElement.lang = value; document.title = value === 'ko' ? 'SurfX — 작은 도전, 새로운 파도' : 'SurfX — Small steps. New waves.' })
onMounted(() => { window.addEventListener('hashchange', syncRoute); document.documentElement.lang = lang.value })
onUnmounted(() => window.removeEventListener('hashchange', syncRoute))
</script>

<template>
  <div class="site-shell">
    <header class="site-header" :class="{ 'paper-header': route === 'whitepaper' }">
      <a class="brand" href="#/" @click="syncRoute" aria-label="SurfX home"><span class="brand-mark">∿</span><span>Surf<span class="brand-x">X</span></span></a>
      <nav class="desktop-nav" :aria-label="lang === 'ko' ? '주 메뉴' : 'Main navigation'">
        <button @click="goTo('about')">{{ t.nav[0] }}</button>
        <button @click="goTo('experience')">{{ t.nav[1] }}</button>
        <button @click="goTo('preview')">{{ t.nav[2] }}</button>
        <a :href="whitepaperUrl">{{ t.nav[3] }}</a>
      </nav>
      <div class="header-actions">
        <div class="language-switch" :aria-label="lang === 'ko' ? '언어 선택' : 'Choose language'">
          <button :class="{ active: lang === 'ko' }" @click="changeLanguage('ko')" :aria-pressed="lang === 'ko'">KR</button><span>/</span><button :class="{ active: lang === 'en' }" @click="changeLanguage('en')" :aria-pressed="lang === 'en'">EN</button>
        </div>
        <button class="menu-toggle" @click="menuOpen = !menuOpen" :aria-expanded="menuOpen" :aria-label="lang === 'ko' ? '메뉴 열기' : 'Open menu'">{{ menuOpen ? '✕' : '☰' }}</button>
      </div>
      <nav v-if="menuOpen" class="mobile-nav">
        <button @click="goTo('about')">{{ t.nav[0] }}</button><button @click="goTo('experience')">{{ t.nav[1] }}</button><button @click="goTo('preview')">{{ t.nav[2] }}</button><a :href="whitepaperUrl">{{ t.nav[3] }}</a>
      </nav>
    </header>

    <main v-if="route === 'home'">
      <section class="hero" id="top">
        <div class="hero-glow"></div>
        <div class="hero-content container">
          <div class="hero-copy">
            <p class="eyebrow"><span class="eyebrow-line"></span>{{ t.eyebrow }}</p>
            <h1>{{ t.hero1 }}<br><em>{{ t.hero2 }}</em></h1>
            <p class="hero-body">{{ t.heroBody }}</p>
            <div class="button-row"><button class="btn btn-primary" @click="goTo('about')">{{ t.heroCta }} <span>↗</span></button><a class="btn btn-text" :href="whitepaperUrl">{{ t.paperCta }} <span>↗</span></a></div>
          </div>
          <div class="hero-art" aria-hidden="true"><div class="hero-orbit orbit-one"></div><div class="hero-orbit orbit-two"></div><img :src="asset('wave.webp')" alt=""><span class="art-glint glint-one">✳</span><span class="art-glint glint-two">✦</span></div>
        </div>
        <div class="hero-bottom container"><span>SCROLL TO EXPLORE ↓</span><span>{{ t.heroNote }} · 2026</span></div>
      </section>

      <div class="ticker"><div class="ticker-inner"><template v-for="n in 2" :key="n"><span v-for="item in t.strip" :key="`${n}-${item}`">✳ &nbsp; {{ item }}</span></template></div></div>

      <section class="about section-pad container" id="about">
        <div class="section-heading"><p class="kicker">{{ t.aboutKicker }}</p><h2 class="multiline">{{ t.aboutTitle }}</h2><p>{{ t.aboutBody }}</p></div>
        <div class="step-grid"><article v-for="step in t.steps" :key="step.n" class="step-card"><div class="step-top"><span>{{ step.n }} / 03</span><span class="step-icon">{{ step.icon }}</span></div><div><h3>{{ step.title }}</h3><p>{{ step.body }}</p></div></article></div>
      </section>

      <section class="feature-section" id="experience"><div class="container feature-layout"><div class="feature-visual"><div class="feature-halo"></div><img :src="asset('screen-home.png')" :alt="t.screens[0]"><span class="visual-caption">DAILY SURF / 01</span></div><div class="feature-copy"><p class="kicker">{{ t.featureKicker }}</p><h2 class="multiline">{{ t.featureTitle }}</h2><p class="feature-lead">{{ t.featureBody }}</p><ul><li v-for="item in t.featureItems" :key="item"><span>✦</span>{{ item }}</li></ul><p class="fine-print">{{ t.demo }}</p></div></div></section>

      <section class="preview section-pad container" id="preview"><div class="preview-heading"><div><p class="kicker">{{ t.appKicker }}</p><h2>{{ t.appTitle }}</h2></div><p>{{ t.appBody }}</p></div><div class="screen-grid"><figure v-for="(screen, i) in ['screen-home.png', 'screen-missions.png', 'screen-crew.png']" :key="screen"><div class="screen-frame"><img :src="asset(screen)" :alt="t.screens[i]" loading="lazy"></div><figcaption><span>0{{ i + 1 }}</span> {{ t.screens[i] }}</figcaption></figure></div></section>

      <section class="faq-section" id="faq"><div class="container faq-layout"><div><p class="kicker">{{ t.faqKicker }}</p><h2>{{ t.faqTitle }}</h2></div><div class="faq-list"><div v-for="(faq, i) in t.faqs" :key="i" class="faq-item"><button @click="openFaq = openFaq === i ? -1 : i" :aria-expanded="openFaq === i"><span>{{ faq.q }}</span><span class="faq-sign">{{ openFaq === i ? '−' : '+' }}</span></button><p v-if="openFaq === i">{{ faq.a }}</p></div></div></div></section>

      <section class="closing"><div class="container closing-inner"><p class="kicker">{{ t.ctaKicker }}</p><h2 class="multiline">{{ t.ctaTitle }}</h2><p>{{ t.ctaBody }}</p><a class="btn btn-primary" :href="whitepaperUrl">{{ t.paperCta }} <span>↗</span></a><div class="closing-wave" aria-hidden="true">∿</div></div></section>
    </main>

    <main v-else class="paper-main"><div class="container paper-intro"><p class="kicker">{{ t.paper.label }}</p><h1>{{ t.paper.title }}</h1><p>{{ t.paper.intro }}</p></div><div class="container paper-layout"><aside class="paper-sidebar"><span class="sidebar-title">CONTENTS</span><button v-for="section in t.paper.sections" :key="section.id" @click="scrollPaper(section.id)">{{ section.title }}</button></aside><div class="paper-articles"><section v-for="section in t.paper.sections" :id="section.id" :key="section.id"><h2>{{ section.title }}</h2><p v-for="paragraph in section.paragraphs" :key="paragraph">{{ paragraph }}</p></section><a class="repo-link" href="https://github.com/neocodekr/SurfX" target="_blank" rel="noopener noreferrer">GitHub repository ↗</a></div></div></main>

    <footer class="footer"><div class="container footer-top"><div><a class="brand" href="#/" @click="syncRoute"><span class="brand-mark">∿</span><span>Surf<span class="brand-x">X</span></span></a><p>{{ t.footerDesc }}</p></div><div class="footer-links"><button @click="goTo('about')">{{ t.nav[0] }}</button><button @click="goTo('experience')">{{ t.nav[1] }}</button><a :href="whitepaperUrl">{{ t.nav[3] }}</a><a href="https://github.com/neocodekr/SurfX" target="_blank" rel="noopener noreferrer">GitHub ↗</a></div></div><div class="container footer-bottom"><span>© 2026 SurfX. All rights reserved.</span><button @click="goTo('top')">{{ t.top }} ↑</button></div></footer>
  </div>
</template>
