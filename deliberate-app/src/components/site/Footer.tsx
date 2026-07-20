import { Link } from "@tanstack/react-router";
import { site } from "@/config/site";

export function Footer() {
  return (
    <footer className="mt-24 border-t border-border/70 bg-background">
      <div className="mx-auto grid w-full max-w-[1216px] gap-8 px-5 py-12 sm:px-8 md:grid-cols-[minmax(0,1fr)_auto_auto_auto]">
        <div>
          <div className="flex items-center gap-2 font-semibold text-ink">
            <span
              aria-hidden
              className="grid h-8 w-8 place-items-center rounded-full bg-brand text-brand-foreground text-sm font-bold"
            >
              停
            </span>
            <span>{site.brand}</span>
          </div>
          <p className="mt-3 max-w-sm text-sm text-muted-foreground">
            在你准备打开短视频时增加几秒缓冲，让习惯性点击重新变成一次主动选择。
          </p>
        </div>
        <FooterCol title="产品">
          <a href="/#how">工作方式</a>
          <a href="/#features">功能</a>
          <Link to="/download">下载</Link>
        </FooterCol>
        <FooterCol title="法律">
          <Link to="/privacy">隐私政策</Link>
          <Link to="/terms">用户协议</Link>
        </FooterCol>
        <FooterCol title="联系">
          <span className="text-muted-foreground">
            {site.supportEmail || "客服邮箱：待配置"}
          </span>
          <span className="text-muted-foreground">{site.domain}</span>
        </FooterCol>
      </div>
      <div className="border-t border-border/70">
        <div className="mx-auto flex w-full max-w-[1216px] flex-col gap-2 px-5 py-5 text-xs text-muted-foreground sm:flex-row sm:items-center sm:justify-between sm:px-8">
          <span>
            © {new Date().getFullYear()} {site.company}
          </span>
          <span>{site.icp}</span>
        </div>
      </div>
    </footer>
  );
}

function FooterCol({
  title,
  children,
}: {
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="text-sm">
      <div className="mb-3 font-medium text-ink">{title}</div>
      <div className="flex flex-col gap-2 text-ink/70 [&_a:hover]:text-brand">
        {children}
      </div>
    </div>
  );
}
