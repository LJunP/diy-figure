<template>
  <div class="terms-page">
    <div class="terms-hero glass-strong" v-reveal>
      <div class="hero-glow"></div>
      <h1 class="terms-title">用户协议与合规说明</h1>
      <p class="terms-sub">定制盲盒手办服务 · 含免责条款</p>
      <div class="terms-meta">
        <el-tag effect="dark" size="small" type="warning">草案 v0.1</el-tag>
        <span>生效日期以正式上线公告为准</span>
      </div>
    </div>

    <!-- 草案提示:法务文本必须经人工审阅,不能当成已生效条款 -->
    <el-alert
      class="draft-alert"
      type="warning"
      :closable="false"
      show-icon
      title="本页为开发阶段的条款草案"
      description="内容依据 01-项目规划书 §5.1 与 02-产品需求说明书 §5 的业务规则编写，覆盖产品已实现的规则。正式对外提供付费服务前，必须经法律顾问审阅并补齐主体信息、争议解决条款与生效日期。"
    />

    <section class="toc glass-card" v-reveal>
      <h2 class="sec-title">目录</h2>
      <ol class="toc-list">
        <li v-for="s in sections" :key="s.id">
          <a :href="`#${s.id}`" @click.prevent="scrollTo(s.id)">{{ s.title }}</a>
        </li>
      </ol>
    </section>

    <section
      v-for="(s, i) in sections"
      :id="s.id"
      :key="s.id"
      class="sec glass-card"
      v-reveal="0.05"
    >
      <h2 class="sec-title">
        <span class="sec-num">{{ i + 1 }}</span>
        {{ s.title }}
      </h2>

      <p v-for="(p, pi) in s.paras" :key="`p${pi}`" class="sec-p">{{ p }}</p>

      <ul v-if="s.items" class="sec-ul">
        <li v-for="(it, ii) in s.items" :key="`i${ii}`">
          <strong v-if="it.k">{{ it.k }}</strong>{{ it.v }}
        </li>
      </ul>

      <div v-if="s.note" class="sec-note">{{ s.note }}</div>
    </section>

    <div class="terms-foot glass-card" v-reveal>
      <p>继续使用本平台即表示你已阅读并同意上述条款。如不同意，请停止使用并注销账号。</p>
      <el-button round class="back-btn" @click="$router.back()">返回上一页</el-button>
    </div>
  </div>
</template>

<script setup>
/**
 * 用户协议与合规说明页
 *
 * 对应 02-产品需求说明书.md §6 页面清单第 14 项「用户协议与合规说明页（含免责条款）」。
 *
 * 内容全部来自项目已锁定的业务规则，不引入任何未定义承诺：
 * - 合规红线与双重拦截 → 01-项目规划书 §5.1、02 §3.3、§5
 * - 定金/尾款、抽奖、设计锁定 → 02 §5
 * - 取消违约金三档 → 02 §4.4、§5
 * - 补购窗口与定价 → 02 §3.8、§5
 * - 通知渠道 → 02 §3.10
 *
 * ★ 免责条款按 01 文档的定位写成「事后追偿的补充依据」，不作为主防线，
 *   主防线是 AI 自审 + 人工终审。不要在文案里把免责当成免责金牌。
 */

const sections = [
  {
    id: 'service',
    title: '服务说明',
    paras: [
      '本平台提供 AI 对话式手办/盲盒定制服务。你以「系列」为单位与 AI 协作设计原创角色形象，' +
        '平台负责报价审核、组织生产、质检与发货，最终向你交付实体定制盲盒手办。',
      'V1 版本不支持国际发货，也不支持中国大陆以外的支付方式。'
    ],
    items: [
      { k: '规格档位：', v: '轻量（6 选 4）/ 经典（9 选 6）/ 收藏（12 选 8），保留比例约 2/3。' },
      { k: '尺寸档位：', v: '对应不同大小的包装盒，盒身外观统一设计，仅尺寸不同。' },
      { k: '交付形态：', v: '抽签决定实际生产的角色子集，未中签角色不生产实物，但可在补购窗口内另行下单。' }
    ]
  },
  {
    id: 'compliance',
    title: '原创性与合规红线',
    paras: [
      '本平台只接受原创角色设计。禁止设计任何模仿真实明星、公众人物或已有版权 IP 角色的形象。',
      '由于平台深度参与设计工具提供、生产组织与销售环节，依据《民法典》第一千一百九十七条' +
        '「知道或应当知道」规则，平台不能仅凭用户免责声明规避连带责任。' +
        '因此平台设置双重拦截：'
    ],
    items: [
      { k: '第一道 · AI 自审：', v: '对话与出图的提示词层面即约束禁止生成可辨识的真实人物或已有 IP 形象，触发时引导你转向原创方向，而非直接出图。' },
      { k: '第二道 · 人工终审：', v: '你提交报价申请后，运营必须查看该系列全部定稿设计并人工判定是否存在侵权风险；判定侵权一律拒绝，不因商业损失而放水。' }
    ],
    note:
      '你需要保证：上传的参考图、文字描述及最终设计不侵犯任何第三方的著作权、商标权、肖像权或其他权利。' +
      '若因你的设计内容引发第三方主张权利，由你承担相应责任；平台的免责条款仅作为事后追偿的补充依据，不构成平台放弃审核义务的理由。'
  },
  {
    id: 'order',
    title: '定制商品特性与交易流程',
    paras: [
      '定制商品一旦投入生产即无法转卖，属于《消费者权益保护法》规定的「根据消费者要求定作」的商品。' +
        '请在下单前充分确认设计稿与规格档位。'
    ],
    items: [
      { k: '提交报价申请：', v: '系列内「已定稿」画布数量达到所选档位要求的设计总数后才可提交。' },
      { k: '人工终审：', v: '运营审核全部定稿设计。不通过时会注明原因，你可修改画布后重新提交。' },
      { k: '人工报价：', v: '终审通过后由运营填写价格与预计交期，一并反馈给你，你决定接受或拒绝。' },
      { k: '抽选：', v: '接受报价后由你手动触发抽选，决定实际生产的角色子集，结果不可更改。' },
      { k: '定金：', v: '订单总额的 50%，接受报价并完成抽选后收取。定金支付成功后设计即锁定，不可再修改。' },
      { k: '尾款：', v: '订单总额的 50%，厂家生产完成且平台质检通过后收取，收到尾款后才安排发货。' },
      { k: '质检：', v: '货物先回平台质检，质检通过才收尾款、才转发给你；不通过则返工重做。' }
    ],
    note: '设计锁定范围：定金支付成功后，已中签与未中签画布全部锁定。未中签画布代表已过审的潜在补购品，允许修改会重新触发合规审核链路。'
  },
  {
    id: 'cancel',
    title: '取消与违约金',
    paras: [
      '违约金基数均为你已实际支付的定金，而非订单总价。分三档计算：'
    ],
    items: [
      { k: '未支付定金：', v: '无损失，扣款 0，全额无需退款。' },
      { k: '已支付定金、厂家未开工：', v: '扣除定金的 30%，退还定金的 70%。' },
      { k: '已开工（含生产、质检、发货阶段）：', v: '定金不予退还。' }
    ],
    note: '开工时点以运营录入的实际开工记录为准，不以支付时间推算。V1 的真实退款通道尚未接入，退款需人工处理。'
  },
  {
    id: 'refill',
    title: '补购规则',
    paras: [
      '主订单发货后开启 60 天补购窗口，你可对未中签角色另行下单补购。'
    ],
    items: [
      { k: '补购窗口：', v: '主订单发货后 60 天内有效，逾期即不可补购。' },
      { k: '补购定价：', v: '（系列套餐总价 ÷ 中签数量）× 1.3。' },
      { k: '订单关系：', v: '补购订单独立于主订单，状态互不影响。' }
    ]
  },
  {
    id: 'ai',
    title: 'AI 生成内容免责',
    paras: [
      '平台使用第三方 AI 服务生成对话内容与概念图，并使用第三方服务生成 3D 参考模型。'
    ],
    items: [
      { k: '不作承诺：', v: '平台不承诺 AI 生成结果在风格、配色、比例上完全符合你的预期，也不承诺多轮生成之间保持完全一致。' },
      { k: '3D 模型定位：', v: '3D 参考模型仅用于辅助确认造型与体量关系，不是最终实物的精确尺寸或材质承诺。' },
      { k: '服务可用性：', v: '第三方 AI 服务可能因限流、故障或策略调整而暂时不可用，平台会保留失败提示与重试入口，但不承担因此产生的间接损失。' },
      { k: '内容审核：', v: 'AI 生成内容仍需经过人工终审，AI 输出不构成平台的合规认可。' }
    ]
  },
  {
    id: 'liability',
    title: '责任限制',
    paras: [
      '平台按「现状」提供服务。在法律允许的最大范围内，平台不对以下情形承担责任：' +
        '因你提供的设计内容或参考素材引发的第三方权利主张（详见第 2 节）；' +
        '因第三方 AI、支付、物流、云服务故障导致的延迟或不可用；' +
        '因不可抗力导致的生产或交付延迟。',
      '平台的赔偿责任总额以该笔订单你实际支付的金额为上限。'
    ]
  },
  {
    id: 'notice',
    title: '通知方式',
    paras: [
      '站内消息覆盖订单全生命周期的每一次状态变化；邮件仅在报价确认、需付尾款、已发货三个关键节点发送。',
      '未配置邮件服务时，所有通知降级为站内消息。请留意站内消息中心的未读提醒，因未及时查看通知导致的逾期后果由你自行承担。'
    ]
  },
  {
    id: 'change',
    title: '协议变更与生效',
    paras: [
      '平台可能根据法律法规或业务调整更新本协议，更新后会在本页公示并更新版本号。',
      '若变更涉及你的重要权益，平台会通过站内消息另行提示。变更生效后你继续使用本平台，即视为接受更新后的条款。'
    ],
    note: '本页当前为草案 v0.1，主体信息、争议解决方式与管辖法院、正式生效日期待法务审阅后补齐。'
  }
]

function scrollTo(id) {
  const el = document.getElementById(id)
  if (!el) return
  const top = el.getBoundingClientRect().top + window.scrollY - 90
  window.scrollTo({ top, behavior: 'smooth' })
}
</script>

<style scoped>
.terms-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px;
}

/* ===== 头部 ===== */
.terms-hero {
  position: relative;
  overflow: hidden;
  padding: 40px 32px;
  border-radius: var(--radius-lg);
  margin-bottom: 20px;
}

.hero-glow {
  position: absolute;
  top: -60%;
  left: 50%;
  transform: translateX(-50%);
  width: 80%;
  height: 200%;
  background: radial-gradient(ellipse at center, rgba(109, 124, 255, 0.14), transparent 70%);
  pointer-events: none;
}

.terms-title {
  position: relative;
  font-size: 30px;
  font-weight: 800;
  color: var(--text-1);
  letter-spacing: 0.5px;
  margin: 0;
}

.terms-sub {
  position: relative;
  color: var(--text-3);
  font-size: 14px;
  margin: 10px 0 16px;
}

.terms-meta {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--text-4);
}

.draft-alert {
  margin-bottom: 20px;
  border-radius: var(--radius-md);
}

/* ===== 目录 ===== */
.toc {
  padding: 22px 26px;
  border-radius: var(--radius-md);
  margin-bottom: 20px;
}

.toc-list {
  margin: 0;
  padding-left: 20px;
  columns: 2;
  column-gap: 32px;
}

.toc-list li {
  margin: 7px 0;
  break-inside: avoid;
}

.toc-list a {
  color: var(--text-2);
  font-size: 14px;
  transition: color 0.2s ease;
}

.toc-list a:hover {
  color: var(--brand-cyan);
}

/* ===== 章节 ===== */
.sec {
  padding: 26px 28px;
  border-radius: var(--radius-md);
  margin-bottom: 16px;
  scroll-margin-top: 90px;
}

.sec-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 700;
  color: var(--text-1);
  margin: 0 0 14px;
}

.sec-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  border-radius: 7px;
  font-size: 13px;
  font-weight: 700;
  color: #fff;
  background: var(--gradient-brand);
}

.sec-p {
  color: var(--text-2);
  font-size: 14px;
  line-height: 1.85;
  margin: 0 0 12px;
}

.sec-ul {
  margin: 4px 0 12px;
  padding-left: 20px;
}

.sec-ul li {
  color: var(--text-2);
  font-size: 14px;
  line-height: 1.85;
  margin: 7px 0;
}

.sec-ul strong {
  color: var(--text-1);
  font-weight: 600;
}

.sec-note {
  margin-top: 12px;
  padding: 12px 16px;
  border-left: 3px solid var(--brand-cyan);
  border-radius: 0 8px 8px 0;
  background: rgba(34, 211, 238, 0.06);
  color: var(--text-2);
  font-size: 13.5px;
  line-height: 1.8;
}

/* ===== 页脚 ===== */
.terms-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
  padding: 22px 28px;
  border-radius: var(--radius-md);
  margin-top: 4px;
}

.terms-foot p {
  color: var(--text-3);
  font-size: 13.5px;
  margin: 0;
}

.back-btn {
  background: var(--gradient-brand) !important;
  border: none !important;
  color: #fff !important;
  box-shadow: 0 2px 16px rgba(109, 124, 255, 0.3);
}

@media (max-width: 720px) {
  .terms-page { padding: 20px 16px; }
  .toc-list { columns: 1; }
  .terms-title { font-size: 24px; }
}
</style>
