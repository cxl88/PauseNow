import * as React from "react";
import { Header } from "./Header";
import { Footer } from "./Footer";

export function LegalLayout({
  title,
  effective,
  updated,
  children,
}: {
  title: string;
  effective: string;
  updated: string;
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-screen bg-background">
      <Header />
      <main>
        <div className="mx-auto w-full max-w-[900px] px-5 py-14 sm:px-8">
          <h1 className="text-3xl font-bold tracking-tight text-ink sm:text-4xl">
            {title}
          </h1>
          <p className="mt-3 text-sm text-muted-foreground">
            生效日期：{effective} · 更新日期：{updated}
          </p>

          <div className="mt-6 rounded-xl border border-notice/50 bg-notice/40 px-5 py-4 text-sm leading-relaxed text-notice-foreground [overflow-wrap:anywhere]">
            本页面为示例文本，用于展示产品的处理原则与用户权利，不构成法律意见；正式发布前请由法务审阅并根据实际业务调整。
          </div>

          <article className="prose-legal mt-10 space-y-8 text-[15px] leading-[1.85] text-ink/85">
            {children}
          </article>
        </div>
      </main>
      <Footer />
    </div>
  );
}
