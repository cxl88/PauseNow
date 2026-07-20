import { createFileRoute } from "@tanstack/react-router";
import { Header } from "@/components/site/Header";
import { Footer } from "@/components/site/Footer";
import { Button } from "@/components/site/Button";
import { Pill } from "@/components/site/Pill";
import { Card } from "@/components/site/Card";
import { FaqAccordion } from "@/components/site/FaqAccordion";
import { useDownloadGate } from "@/components/site/useDownloadGate";
import { site } from "@/config/site";
import {
  Pause,
  Wind,
  Target,
  BarChart3,
  ShieldCheck,
  Timer,
  MousePointerClick,
  Eye,
  CheckCircle2,
} from "lucide-react";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "停一下 — 打开短视频前，先停一下" },
      {
        name: "description",
        content:
          "停一下是一款帮助成年人自我管理的安卓工具，在打开短视频前增加几秒缓冲，让习惯性点击重新变成主动选择。",
      },
    ],
  }),
  component: HomePage,
});

function HomePage() {
  const { trigger, modal } = useDownloadGate();
  return (
    <div className="min-h-screen bg-background">
      <Header />
      <main>
        <Hero onDownload={() => trigger(site.androidDownloadUrl)} onChallenge={() => trigger(site.betaSignupUrl)} />
        <HowItWorks />
        <Features />
        <PrivacySection />
        <ChallengeSection onBeta={() => trigger(site.betaSignupUrl)} />
        <FaqSection />
      </main>
      <Footer />
      {modal}
    </div>
  );
}

function Section({
  id,
  children,
  className = "",
}: {
  id?: string;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <section
      id={id}
      className={`mx-auto w-full max-w-[1216px] px-5 sm:px-8 ${className}`}
    >
      {children}
    </section>
  );
}

function Hero({
  onDownload,
  onChallenge,
}: {
  onDownload: () => void;
  onChallenge: () => void;
}) {
  return (
    <Section className="pt-14 pb-20 md:pt-20 md:pb-28">
      <div className="grid gap-12 md:grid-cols-[minmax(0,1.05fr)_minmax(0,0.95fr)] md:items-center">
        <div>
          <Pill>安卓优先 · 成年人自我管理</Pill>
          <h1 className="mt-5 text-4xl font-bold leading-[1.15] tracking-tight text-ink sm:text-5xl md:text-[56px]">
            打开短视频前，
            <br />
            先停一下。
          </h1>
          <p className="mt-6 max-w-xl text-base leading-relaxed text-ink/70 sm:text-lg">
            不是强制锁住，也不是要求彻底戒掉。停一下只在你准备打开短视频时增加几秒缓冲，让习惯性点击重新变成一次主动选择。
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Button size="lg" onClick={onDownload}>
              下载安卓内测版
            </Button>
            <Button size="lg" variant="secondary" onClick={onChallenge}>
              参加 7 天挑战
            </Button>
          </div>
          <ul className="mt-8 flex flex-wrap gap-x-6 gap-y-2 text-sm text-muted-foreground">
            {[
              "不读取聊天内容",
              "权限用途透明",
              "随时可以关闭",
            ].map((t) => (
              <li key={t} className="flex items-center gap-2">
                <CheckCircle2 className="size-4 text-brand" aria-hidden />
                {t}
              </li>
            ))}
          </ul>
        </div>

        <PhoneMock />
      </div>
    </Section>
  );
}

function PhoneMock() {
  return (
    <div className="mx-auto w-full max-w-[360px]">
      <div className="rounded-[42px] border border-border bg-ink/95 p-3 shadow-2xl">
        <div className="relative overflow-hidden rounded-[32px] bg-background aspect-[9/19]">
          {/* Status bar */}
          <div className="flex items-center justify-between px-6 pt-4 text-[11px] font-medium text-ink/70">
            <span>9:41</span>
            <span className="flex items-center gap-1">
              <span>停一下</span>
            </span>
            <span>100%</span>
          </div>

          <div className="flex h-[calc(100%-2.25rem)] flex-col items-center px-6 pt-6 text-center">
            <div className="grid size-16 place-items-center rounded-full bg-brand-soft text-brand">
              <Pause className="size-8" />
            </div>
            <h3 className="mt-5 text-lg font-semibold text-ink">
              你真的想打开吗？
            </h3>
            <p className="mt-2 text-sm text-muted-foreground">
              先呼吸 5 秒，再决定是否继续。
            </p>

            <div className="mt-6 w-full">
              <div className="h-1.5 w-full overflow-hidden rounded-full bg-brand-soft">
                <div className="h-full w-2/3 rounded-full bg-brand transition-all" />
              </div>
              <div className="mt-2 flex justify-between text-[11px] text-muted-foreground">
                <span>缓冲中</span>
                <span>剩余 2 秒</span>
              </div>
            </div>

            <div className="mt-auto mb-6 flex w-full flex-col gap-2 pt-6">
              <button
                type="button"
                className="h-11 rounded-full bg-brand text-sm font-medium text-brand-foreground"
              >
                先不看了
              </button>
              <button
                type="button"
                className="h-11 rounded-full border border-border bg-transparent text-sm font-medium text-ink/80"
              >
                继续打开
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function HowItWorks() {
  const steps = [
    {
      icon: Target,
      title: "选择目标应用",
      desc: "挑选那些你希望减少的短视频或社交应用，其他应用完全不受影响。",
    },
    {
      icon: Wind,
      title: "打开前暂停几秒",
      desc: "点开应用的一瞬间，出现一个缓冲页面，让你有机会重新决定。",
    },
    {
      icon: BarChart3,
      title: "查看真实改变",
      desc: "每周看到点击被中断、时间被节省，改变是可以被度量的。",
    },
  ];
  return (
    <Section id="how" className="py-20">
      <SectionHead
        eyebrow="工作方式"
        title="不和意志力硬碰硬"
        desc="停一下不是把你锁在应用之外，而是在你即将开始的那一刻，多给你一次机会。"
      />
      <div className="mt-12 grid gap-6 md:grid-cols-3">
        {steps.map((s) => (
          <Card key={s.title}>
            <div className="grid size-11 place-items-center rounded-xl bg-brand-soft text-brand">
              <s.icon className="size-5" />
            </div>
            <h3 className="mt-5 text-lg font-semibold text-ink">{s.title}</h3>
            <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
              {s.desc}
            </p>
          </Card>
        ))}
      </div>
    </Section>
  );
}

function Features() {
  const items = [
    {
      icon: MousePointerClick,
      title: "打开前干预",
      desc: "在应用启动前插入短暂缓冲，配合呼吸提示，减少无意识点击。",
    },
    {
      icon: Eye,
      title: "使用目的确认",
      desc: "打开前主动填写你想做什么，让每一次使用都带着明确的目的。",
    },
    {
      icon: Timer,
      title: "时间预算",
      desc: "为每个应用设定每日预算，超过时给出温和提醒，而不是强行关闭。",
    },
    {
      icon: BarChart3,
      title: "趋势报告",
      desc: "每周汇总被中断的点击次数、节省的时间与你的目标完成情况。",
    },
  ];
  return (
    <Section id="features" className="py-20">
      <SectionHead
        eyebrow="功能"
        title="只做真正影响行为的功能"
        desc="不做花哨的排行榜、不做社交比拼，只保留经过验证能改变行为的能力。"
      />
      <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
        {items.map((it) => (
          <Card key={it.title} className="h-full">
            <div className="grid size-11 place-items-center rounded-xl bg-brand-soft text-brand">
              <it.icon className="size-5" />
            </div>
            <h3 className="mt-5 text-base font-semibold text-ink">
              {it.title}
            </h3>
            <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
              {it.desc}
            </p>
          </Card>
        ))}
      </div>
    </Section>
  );
}

function PrivacySection() {
  const items = [
    "不读取任何聊天内容、账号信息或应用内数据。",
    "不出售、不共享个人信息用于广告或第三方营销。",
    "每一项系统权限都注明用途，且可随时关闭。",
    "支持一键导出或删除本地数据，随时可注销账号。",
  ];
  return (
    <Section id="privacy" className="py-20">
      <Card tone="brand" className="md:p-12">
        <div className="grid gap-10 md:grid-cols-[minmax(0,1fr)_auto] md:items-start">
          <div>
            <Pill
              tone="brand"
              className="!bg-white/15 !text-brand-foreground"
            >
              隐私原则
            </Pill>
            <h2 className="mt-4 text-3xl font-bold leading-tight sm:text-4xl">
              管理手机，不等于监视手机。
            </h2>
            <p className="mt-4 max-w-xl text-brand-foreground/85">
              我们只处理"你什么时候打开了哪个应用、停留了多久"这类数据，用于生成你自己的报告。剩下的一切，我们不需要知道。
            </p>
          </div>
          <div className="grid size-14 place-items-center rounded-2xl bg-white/15 text-brand-foreground">
            <ShieldCheck className="size-7" />
          </div>
        </div>
        <ul className="mt-8 grid gap-4 sm:grid-cols-2">
          {items.map((t) => (
            <li
              key={t}
              className="flex items-start gap-3 rounded-xl bg-white/10 p-4 text-sm text-brand-foreground/95"
            >
              <CheckCircle2 className="mt-0.5 size-4 shrink-0" aria-hidden />
              <span>{t}</span>
            </li>
          ))}
        </ul>
      </Card>
    </Section>
  );
}

function ChallengeSection({ onBeta }: { onBeta: () => void }) {
  return (
    <Section className="py-20">
      <Card tone="soft" className="md:p-12">
        <div className="grid gap-8 md:grid-cols-[minmax(0,1fr)_auto] md:items-center">
          <div>
            <Pill>限时挑战</Pill>
            <h2 className="mt-4 text-3xl font-bold leading-tight text-ink sm:text-4xl">
              7 天少刷挑战
            </h2>
            <p className="mt-4 max-w-xl text-ink/70">
              选择一个你最想减少的短视频应用，跟着停一下走完 7 天。第 7 天，你会拿到一份属于自己的"点击拦截报告"。
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <Button size="lg" onClick={onBeta}>
              申请内测
            </Button>
            <Button size="lg" variant="secondary" onClick={() => {
              const el = document.getElementById("download-help");
              if (el) el.scrollIntoView({ behavior: "smooth" });
              else window.location.href = "/download";
            }}>
              查看下载说明
            </Button>
          </div>
        </div>
      </Card>
    </Section>
  );
}

function FaqSection() {
  const items = [
    {
      q: "停一下会封锁短视频吗？",
      a: "不会。我们只在你打开的那一刻插入几秒缓冲，你依然可以继续使用，只是多了一次主动确认。",
    },
    {
      q: "为什么需要一些系统权限？",
      a: "为了在应用启动前显示缓冲页面，我们需要「使用情况访问」和「悬浮窗」权限。除此之外的权限都不是必需，可以在系统设置里逐项关闭。",
    },
    {
      q: "会读取我的聊天或个人内容吗？",
      a: "不会。停一下只知道「你打开了哪个应用、什么时间、停留多久」，不会读取任何应用内的具体内容。",
    },
    {
      q: "这是医疗工具或成瘾治疗方案吗？",
      a: "不是。停一下是一款自我管理的效率工具，不能替代任何医学诊断或治疗。如你有严重困扰，建议寻求专业帮助。",
    },
    {
      q: "iPhone 用户什么时候能用？",
      a: "iOS 版本正在评估中，可以在下载页登记预约，我们上线时会第一时间通知你。",
    },
  ];
  return (
    <Section id="faq" className="py-20">
      <SectionHead
        eyebrow="常见问题"
        title="关于停一下，你可能想问"
      />
      <div className="mt-10">
        <FaqAccordion items={items} />
      </div>
    </Section>
  );
}

function SectionHead({
  eyebrow,
  title,
  desc,
}: {
  eyebrow: string;
  title: string;
  desc?: string;
}) {
  return (
    <div className="max-w-2xl">
      <Pill tone="muted">{eyebrow}</Pill>
      <h2 className="mt-4 text-3xl font-bold leading-tight tracking-tight text-ink sm:text-4xl">
        {title}
      </h2>
      {desc && (
        <p className="mt-4 text-base leading-relaxed text-ink/70">{desc}</p>
      )}
    </div>
  );
}
