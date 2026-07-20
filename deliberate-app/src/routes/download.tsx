import { createFileRoute } from "@tanstack/react-router";
import { Header } from "@/components/site/Header";
import { Footer } from "@/components/site/Footer";
import { Button } from "@/components/site/Button";
import { Pill } from "@/components/site/Pill";
import { Card } from "@/components/site/Card";
import { useDownloadGate } from "@/components/site/useDownloadGate";
import { site } from "@/config/site";
import { Download, Apple, Smartphone, ShieldCheck, Target, Timer } from "lucide-react";

export const Route = createFileRoute("/download")({
  head: () => ({
    meta: [
      { title: "下载停一下 — 从下一次打开开始改变" },
      {
        name: "description",
        content:
          "停一下安卓版本优先开放内测，iOS 版本可先登记预约。查看安装步骤与系统要求。",
      },
      { property: "og:title", content: "下载停一下" },
      {
        property: "og:description",
        content: "安卓内测优先开放，iOS 预约中。查看安装步骤与系统要求。",
      },
    ],
  }),
  component: DownloadPage,
});

function DownloadPage() {
  const { trigger, modal } = useDownloadGate();
  const steps = [
    {
      icon: Download,
      title: "下载并安装",
      desc: "从内测通道获取最新的 APK 安装包，按系统提示完成安装。",
    },
    {
      icon: Target,
      title: "选择目标应用",
      desc: "挑选你希望减少的短视频或社交应用，其他应用完全不受影响。",
    },
    {
      icon: ShieldCheck,
      title: "开启权限",
      desc: "按引导授予「使用情况访问」与「悬浮窗」权限，缓冲页才能正常出现。",
    },
    {
      icon: Timer,
      title: "开始 7 天挑战",
      desc: "选一个目标应用，跟随停一下完成 7 天，看到自己真实的改变。",
    },
  ];

  return (
    <div className="min-h-screen bg-background">
      <Header />
      <main>
        <section className="mx-auto w-full max-w-[1216px] px-5 pt-14 pb-10 sm:px-8 md:pt-20">
          <Pill>下载</Pill>
          <h1 className="mt-5 text-4xl font-bold tracking-tight text-ink sm:text-5xl">
            从下一次打开开始改变
          </h1>
          <p className="mt-4 max-w-2xl text-base leading-relaxed text-ink/70 sm:text-lg">
            安卓版本优先开放；iOS 版本可先登记预约。
          </p>
        </section>

        <section className="mx-auto w-full max-w-[1216px] px-5 pb-12 sm:px-8">
          <Card className="md:p-10">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div className="flex items-center gap-3">
                <span
                  aria-hidden
                  className="grid size-10 place-items-center rounded-full bg-brand text-brand-foreground font-bold"
                >
                  停
                </span>
                <div>
                  <div className="text-base font-semibold text-ink">
                    停一下 · 安卓内测
                  </div>
                  <div className="text-xs text-muted-foreground">
                    面向成年用户自我管理使用
                  </div>
                </div>
              </div>
              <Pill>内测开放中</Pill>
            </div>

            <dl className="mt-8 grid gap-4 sm:grid-cols-3">
              <MetaBlock label="版本" value={site.version} />
              <MetaBlock label="安装包大小" value={site.apkSize} />
              <MetaBlock label="系统要求" value={site.androidRequirement} />
            </dl>

            <div className="mt-8 flex flex-wrap gap-3">
              <Button
                size="lg"
                onClick={() => trigger(site.androidDownloadUrl)}
              >
                <Smartphone className="size-4" aria-hidden />
                下载安卓 APK
              </Button>
              <Button
                size="lg"
                variant="secondary"
                onClick={() =>
                  trigger(
                    site.iosReserveUrl,
                    "iOS 预约入口尚未开放，请稍后再来查看。",
                  )
                }
              >
                <Apple className="size-4" aria-hidden />
                iOS 预约
              </Button>
            </div>

            <div className="mt-6 rounded-xl border border-notice/50 bg-notice/40 px-4 py-3 text-sm text-notice-foreground [overflow-wrap:anywhere]">
              首次安装可能会被系统提示"来自未知来源"，这是安卓对第三方安装包的默认提醒。请从官方通道下载，安装后如遇问题可通过页脚邮箱联系我们。
            </div>
          </Card>
        </section>

        <section id="download-help" className="mx-auto w-full max-w-[1216px] px-5 pb-24 sm:px-8">
          <h2 className="text-2xl font-bold text-ink sm:text-3xl">安装 4 步走</h2>
          <div className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
            {steps.map((s, i) => (
              <Card key={s.title} className="h-full">
                <div className="flex items-center gap-3">
                  <span className="grid size-9 place-items-center rounded-full bg-brand-soft text-brand text-sm font-semibold">
                    {i + 1}
                  </span>
                  <s.icon className="size-5 text-brand" aria-hidden />
                </div>
                <h3 className="mt-5 text-base font-semibold text-ink">
                  {s.title}
                </h3>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                  {s.desc}
                </p>
              </Card>
            ))}
          </div>
        </section>
      </main>
      <Footer />
      {modal}
    </div>
  );
}

function MetaBlock({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-border bg-background/60 p-4">
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="mt-1 text-sm font-medium text-ink">{value}</dd>
    </div>
  );
}
