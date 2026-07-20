import { createFileRoute } from "@tanstack/react-router";
import { LegalLayout } from "@/components/site/LegalLayout";
import { site } from "@/config/site";

export const Route = createFileRoute("/privacy")({
  head: () => ({
    meta: [
      { title: "隐私政策 — 停一下" },
      {
        name: "description",
        content:
          "停一下的隐私政策：我们如何处理你的信息、使用哪些权限、如何行使你的权利。",
      },
    ],
  }),
  component: PrivacyPage,
});

function H2({ children }: { children: React.ReactNode }) {
  return (
    <h2 className="text-xl font-semibold text-ink sm:text-2xl">{children}</h2>
  );
}
function P({ children }: { children: React.ReactNode }) {
  return <p className="[overflow-wrap:anywhere]">{children}</p>;
}
function List({ items }: { items: React.ReactNode[] }) {
  return (
    <ul className="list-disc space-y-2 pl-6 marker:text-brand">
      {items.map((it, i) => (
        <li key={i} className="[overflow-wrap:anywhere]">
          {it}
        </li>
      ))}
    </ul>
  );
}

function Table({
  head,
  rows,
}: {
  head: string[];
  rows: string[][];
}) {
  return (
    <div className="overflow-x-auto rounded-xl border border-border">
      <table className="w-full min-w-[520px] text-left text-sm">
        <thead className="bg-brand-soft/60 text-ink">
          <tr>
            {head.map((h) => (
              <th key={h} className="px-4 py-3 font-medium">
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-border">
          {rows.map((r, i) => (
            <tr key={i}>
              {r.map((c, j) => (
                <td key={j} className="px-4 py-3 align-top text-ink/80">
                  {c}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function PrivacyPage() {
  return (
    <LegalLayout
      title="隐私政策"
      effective={site.effectiveDate}
      updated={site.updatedDate}
    >
      <section className="space-y-3">
        <H2>1. 适用范围</H2>
        <P>
          本政策适用于{site.company}（下称"我们"）通过"{site.brand}"移动应用及{site.domain}
          官网提供的产品与服务。若你使用第三方渠道分发的版本，仍以本政策为准。
        </P>
      </section>

      <section className="space-y-3">
        <H2>2. 我们处理的信息</H2>
        <P>为向你提供"打开前缓冲"等核心功能，我们仅处理最少必要的信息：</P>
        <Table
          head={["信息类别", "示例", "用途"]}
          rows={[
            ["应用使用统计", "被选为目标的应用启动时间与时长", "生成使用报告、触发缓冲页"],
            ["设备基础信息", "系统版本、设备型号（去标识化）", "适配兼容性、崩溃排查"],
            ["账号信息（可选）", "手机号或邮箱（用于登录/预约）", "登录、找回设置、发送通知"],
          ]}
        />
        <P>我们不收集聊天内容、通讯录、位置轨迹、照片视频等与本服务无关的信息。</P>
      </section>

      <section className="space-y-3">
        <H2>3. 权限说明</H2>
        <Table
          head={["权限", "用途", "是否必需"]}
          rows={[
            ["使用情况访问", "识别你选择的目标应用是否被启动", "是"],
            ["悬浮窗", "在目标应用启动时显示缓冲页面", "是"],
            ["通知", "发送使用报告与提醒", "否，可关闭"],
            ["网络", "同步设置与获取更新", "是"],
          ]}
        />
      </section>

      <section className="space-y-3">
        <H2>4. 处理的法律基础</H2>
        <List
          items={[
            "履行你与我们之间的服务合同所必需；",
            "在你明确同意的范围内处理可选信息；",
            "为遵守法律法规或保障合法权益的必要处理。",
          ]}
        />
      </section>

      <section className="space-y-3">
        <H2>5. 存储与保留</H2>
        <P>
          使用统计数据默认保存在你的设备本地，仅在你开启云同步时才会加密上传。服务器端数据在你注销账号后 30 天内被彻底删除。
        </P>
      </section>

      <section className="space-y-3">
        <H2>6. 共享与第三方</H2>
        <P>
          我们不会出售或将个人信息用于第三方广告。仅在以下情形共享必要数据：云服务托管、崩溃分析、登录短信服务。所有合作方均签署数据处理协议。
        </P>
        <Table
          head={["合作方类型", "共享信息", "目的"]}
          rows={[
            ["云服务", "加密后的备份数据", "为你保存设置"],
            ["崩溃分析", "去标识的崩溃日志", "定位并修复问题"],
            ["短信/邮件服务", "手机号或邮箱", "发送验证码或预约通知"],
          ]}
        />
      </section>

      <section className="space-y-3">
        <H2>7. Cookie 与本地存储</H2>
        <P>
          官网使用必要的本地存储保存你的语言与主题偏好，不用于追踪广告。移动端不使用第三方 Cookie。
        </P>
      </section>

      <section className="space-y-3">
        <H2>8. 你的权利</H2>
        <List
          items={[
            "随时查看、导出或删除你的本地数据；",
            "撤回任何可选授权；",
            "注销账号并请求删除服务器端数据；",
            "对个人信息处理行为提出投诉或申诉。",
          ]}
        />
      </section>

      <section className="space-y-3">
        <H2>9. 安全</H2>
        <P>
          我们采用行业通行的加密传输与访问控制，并对内部人员实行最小权限原则。若发生数据事件，我们会依法在合理期限内告知你。
        </P>
      </section>

      <section className="space-y-3">
        <H2>10. 未成年人</H2>
        <P>
          停一下面向成年人自我管理，不面向 14 周岁以下未成年人。若你是监护人并发现未成年人使用本服务，可联系我们协助处理。
        </P>
      </section>

      <section className="space-y-3">
        <H2>11. 政策更新</H2>
        <P>
          我们可能根据业务或法规调整本政策。重大变更将通过应用内公告或官网提示告知你，并在必要时重新获取你的同意。
        </P>
      </section>

      <section className="space-y-3">
        <H2>12. 联系我们</H2>
        <P>
          如需行使权利或进行咨询，可通过{site.supportEmail || "官网页脚公示的邮箱"}
          与我们联系。运营主体：{site.company}。
        </P>
      </section>
    </LegalLayout>
  );
}
