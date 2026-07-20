import { createFileRoute } from "@tanstack/react-router";
import { LegalLayout } from "@/components/site/LegalLayout";
import { site } from "@/config/site";

export const Route = createFileRoute("/terms")({
  head: () => ({
    meta: [
      { title: "用户协议 — 停一下" },
      {
        name: "description",
        content:
          "停一下的用户协议：服务内容、账号与权限、用户规范、责任边界与争议解决。",
      },
    ],
  }),
  component: TermsPage,
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

function TermsPage() {
  return (
    <LegalLayout
      title="用户协议"
      effective={site.effectiveDate}
      updated={site.updatedDate}
    >
      <section className="space-y-3">
        <H2>1. 服务内容</H2>
        <P>
          "{site.brand}"是一款帮助成年人自我管理的工具，在你打开选定应用前提供缓冲提示与使用报告，不承诺"戒断"或治疗任何行为习惯。
        </P>
      </section>
      <section className="space-y-3">
        <H2>2. 协议变更</H2>
        <P>
          我们可能根据法律法规或业务发展修改本协议，修改后会在应用与官网发布公告。若你继续使用服务，视为接受更新后的条款。
        </P>
      </section>
      <section className="space-y-3">
        <H2>3. 账号</H2>
        <List
          items={[
            "你可以选择匿名使用本地功能，也可注册账号以启用云同步；",
            "请妥善保管账号凭证，因账号泄露产生的责任由使用者自行承担；",
            "同一自然人只应注册一个账号，禁止倒卖或出借。",
          ]}
        />
      </section>
      <section className="space-y-3">
        <H2>4. 权限限制</H2>
        <P>
          我们仅在《隐私政策》列明的范围内使用系统权限，你可以随时在设备设置中撤销授权，撤销后相关功能可能不可用。
        </P>
      </section>
      <section className="space-y-3">
        <H2>5. 用户规范</H2>
        <List
          items={[
            "不得利用本服务从事违法或侵害他人权益的活动；",
            "不得反向工程、破解或绕过缓冲机制以牟利；",
            "不得干扰服务的正常运行或对基础设施发起攻击。",
          ]}
        />
      </section>
      <section className="space-y-3">
        <H2>6. 付费与订阅</H2>
        <P>
          当前版本以免费方式提供，如后续推出订阅或增值服务，将以清晰的方式告知价格、周期、续订与退款规则，并在收费前获得你的明确同意。
        </P>
      </section>
      <section className="space-y-3">
        <H2>7. 测试版本</H2>
        <P>
          内测版本可能存在功能不稳定或数据丢失的风险，请勿将其作为唯一的效率工具。我们会尽力修复但不对测试版本的可用性作出承诺。
        </P>
      </section>
      <section className="space-y-3">
        <H2>8. 知识产权</H2>
        <P>
          除另有约定外，本服务涉及的软件、界面、文案、图形均为{site.company}
          或相关权利人所有，未经许可不得复制、传播或用于商业用途。
        </P>
      </section>
      <section className="space-y-3">
        <H2>9. 第三方服务</H2>
        <P>
          服务可能引用第三方组件或跳转到第三方站点。相关内容由第三方独立提供，我们不对其准确性、合法性或安全性负责。
        </P>
      </section>
      <section className="space-y-3">
        <H2>10. 服务终止</H2>
        <P>
          你可以随时停止使用并注销账号。若你严重违反本协议，我们可以在事先通知的前提下限制或终止相关功能。
        </P>
      </section>
      <section className="space-y-3">
        <H2>11. 责任边界</H2>
        <P>
          在法律允许的范围内，我们不对以下情况承担责任：因不可抗力导致的服务中断；因你自身操作或第三方原因造成的损失；因将本服务用于非自我管理目的而产生的后果。
        </P>
      </section>
      <section className="space-y-3">
        <H2>12. 未成年人</H2>
        <P>
          本服务不面向未成年人。若发现未成年人在未取得监护人同意的情况下使用服务，请联系我们协助处理。
        </P>
      </section>
      <section className="space-y-3">
        <H2>13. 通知</H2>
        <P>
          我们通过应用内消息、官网公告或你预留的联系方式向你发送通知，通知于发送成功后视为送达。
        </P>
      </section>
      <section className="space-y-3">
        <H2>14. 争议解决</H2>
        <P>
          本协议的订立、履行、解释与争议解决适用中华人民共和国法律。因本协议产生的争议，双方应先友好协商解决；协商不成的，任何一方可向运营主体所在地有管辖权的人民法院提起诉讼。
        </P>
      </section>
      <section className="space-y-3">
        <H2>15. 运营主体</H2>
        <P>
          本服务由{site.company}运营。备案信息：{site.icp}。
        </P>
      </section>
    </LegalLayout>
  );
}
