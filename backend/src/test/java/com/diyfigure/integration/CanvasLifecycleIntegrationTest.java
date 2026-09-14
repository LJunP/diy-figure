package com.diyfigure.integration;

import com.diyfigure.canvas.CanvasService;
import com.diyfigure.canvas.dto.CanvasCreateRequest;
import com.diyfigure.common.enums.CanvasStatus;
import com.diyfigure.common.enums.DesignStatus;
import com.diyfigure.common.enums.LotteryResult;
import com.diyfigure.common.enums.Model3dStatus;
import com.diyfigure.common.enums.OrderStatus;
import com.diyfigure.common.enums.OrderType;
import com.diyfigure.common.enums.SpecTier;
import com.diyfigure.common.enums.UserRole;
import com.diyfigure.common.exception.BusinessException;
import com.diyfigure.common.response.ResultCode;
import com.diyfigure.entity.Canvas;
import com.diyfigure.entity.OrderCanvas;
import com.diyfigure.entity.OrderEntity;
import com.diyfigure.entity.Series;
import com.diyfigure.entity.User;
import com.diyfigure.repository.CanvasRepository;
import com.diyfigure.repository.OrderCanvasRepository;
import com.diyfigure.repository.OrderRepository;
import com.diyfigure.repository.SeriesRepository;
import com.diyfigure.repository.UserRepository;
import com.diyfigure.series.SeriesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 画布生命周期测试
 *
 * 覆盖「创建 → 对话 → 定稿(触发 3D) → 重新打开」这条设计链路上,
 * 回归测试里没有专门盯住的部分:
 *
 * 1. 定稿只把 3D 任务入队,不同步调 Meshy
 *    —— 旧实现同步轮询最长 5 分钟,前端 30 秒就超时了。这条测试锁死「必须异步」。
 * 2. 重新打开画布必须清空 3D 状态
 *    —— 否则画布页会一直显示上一版设计的模型,而且状态卡在 SUCCESS 会让重试接口拒调用。
 * 3. 画布名称的落库与兜底(空白名不能写进 NOT NULL 列)
 * 4. 删除保护:锁定画布、已被订单引用的画布都不能删
 *
 * 测试环境把 spring.task.scheduling.enabled 置为 false,所以 Model3dTaskService
 * 的定时任务不会跑,定稿后的状态会稳稳停在 PENDING —— 这正好用来断言"没有同步生成"。
 */
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("画布生命周期测试")
class CanvasLifecycleIntegrationTest {

    @Autowired private CanvasService canvasService;
    @Autowired private SeriesService seriesService;

    @Autowired private UserRepository userRepository;
    @Autowired private SeriesRepository seriesRepository;
    @Autowired private CanvasRepository canvasRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderCanvasRepository orderCanvasRepository;
    @Autowired private org.flywaydb.core.Flyway flyway;
    @Autowired private TransactionTemplate tx;

    private Long userId;
    private Long seriesId;

    @BeforeEach
    void resetAndSeed() {
        flyway.clean();
        flyway.migrate();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User user = userRepository.save(User.builder()
                .username("c" + suffix)
                .email("c" + suffix + "@test.local")
                .passwordHash("not-a-real-hash")
                .role(UserRole.USER)
                .build());
        userId = user.getId();

        seriesId = seriesRepository.save(Series.builder()
                .userId(userId)
                .name("画布测试系列")
                .specTier(SpecTier.LIGHT)
                .sizeTier("STANDARD")
                .designStatus(DesignStatus.DESIGNING)
                .build()).getId();
    }

    // ===== 辅助 =====

    private Canvas newCanvas(String name) {
        CanvasCreateRequest req = new CanvasCreateRequest();
        req.setName(name);
        return canvasService.createCanvas(seriesId, userId, req);
    }

    /** 造一个「已定稿 + 已有概念图」的画布,可以直接走 3D 相关分支 */
    private Canvas finalizedCanvasWithImage() {
        Canvas c = newCanvas("星光精灵");
        c.setConceptImageUrls(List.of("https://cdn.test/concept.png"));
        return canvasRepository.save(c);
    }

    private void assertBusinessError(ResultCode expected, Runnable r) {
        BusinessException ex = assertThrows(BusinessException.class, r::run,
                "期望抛业务异常 " + expected + ",实际没抛");
        assertEquals(expected.getCode(), ex.getCode(),
                "错误码不对,message=" + ex.getMessage());
    }

    // ===== 1. 定稿 → 3D 入队(异步) =====

    @Test
    @DisplayName("定稿:状态转 FINALIZED,3D 任务只入队不阻塞(调度未跑时稳定停在 PENDING)")
    void finalizeEnqueues3dAsynchronously() {
        Canvas canvas = finalizedCanvasWithImage();
        assertEquals(Model3dStatus.NONE, canvas.getModel3dStatus(), "新建画布不应有 3D 任务");

        Canvas finalized = canvasService.finalizeCanvas(canvas.getId(), userId);

        assertEquals(CanvasStatus.FINALIZED, finalized.getStatus());
        assertNotNull(finalized.getFinalizedAt(), "定稿必须记录定稿时间");
        assertEquals(Model3dStatus.PENDING, finalized.getModel3dStatus(),
                "定稿应把 3D 任务置为 PENDING 交给后台调度");
        assertNull(finalized.getModel3dTaskId(),
                "定稿阶段不该已经有外部 taskId —— 有就说明是同步调的 Meshy");

        // 落库状态同样正确(不是只在内存对象上改了)
        Canvas reloaded = canvasRepository.findById(canvas.getId()).orElseThrow();
        assertEquals(CanvasStatus.FINALIZED, reloaded.getStatus());
        assertEquals(Model3dStatus.PENDING, reloaded.getModel3dStatus());
    }

    @Test
    @DisplayName("定稿:没有概念图不能定稿(3D 需要概念图作为输入)")
    void finalizeWithoutConceptImageRejected() {
        Canvas canvas = newCanvas("空概念图角色");
        assertBusinessError(ResultCode.CANVAS_NOT_FINALIZED,
                () -> canvasService.finalizeCanvas(canvas.getId(), userId));

        Canvas reloaded = canvasRepository.findById(canvas.getId()).orElseThrow();
        assertEquals(CanvasStatus.DESIGNING, reloaded.getStatus(), "被拒绝后状态不能变");
    }

    @Test
    @DisplayName("定稿:重复定稿被拒")
    void finalizeTwiceRejected() {
        Canvas canvas = finalizedCanvasWithImage();
        canvasService.finalizeCanvas(canvas.getId(), userId);
        assertBusinessError(ResultCode.BAD_REQUEST,
                () -> canvasService.finalizeCanvas(canvas.getId(), userId));
    }

    @Test
    @DisplayName("定稿:已锁定的画布不能定稿")
    void finalizeLockedCanvasRejected() {
        Canvas canvas = finalizedCanvasWithImage();
        canvas.setLocked(true);
        canvasRepository.save(canvas);
        assertBusinessError(ResultCode.CANVAS_LOCKED,
                () -> canvasService.finalizeCanvas(canvas.getId(), userId));
    }

    // ===== 2. 重新打开 → 清空 3D 状态 =====

    @Test
    @DisplayName("重新打开:回到设计态,并清空上一版定稿的 3D 模型(URL/taskId/状态全部归零)")
    void reopenResetsStale3dState() {
        Canvas canvas = finalizedCanvasWithImage();
        canvasService.finalizeCanvas(canvas.getId(), userId);

        // 模拟后台调度已经把 3D 生成跑完
        tx.executeWithoutResult(s -> {
            Canvas c = canvasRepository.findById(canvas.getId()).orElseThrow();
            c.setModel3dStatus(Model3dStatus.SUCCESS);
            c.setModel3dTaskId("meshy-task-abc");
            c.setModel3dUrl("https://cdn.test/old-model.glb");
            canvasRepository.save(c);
        });

        Canvas reopened = canvasService.reopenCanvas(canvas.getId(), userId);

        assertEquals(CanvasStatus.DESIGNING, reopened.getStatus());
        assertNull(reopened.getFinalizedAt(), "重新打开应清掉定稿时间");
        assertEquals(Model3dStatus.NONE, reopened.getModel3dStatus(),
                "设计要改了,旧 3D 状态必须清空");
        assertNull(reopened.getModel3dUrl(), "旧模型 URL 必须清空,否则画布页会显示上一版设计");
        assertNull(reopened.getModel3dTaskId(), "旧 taskId 必须清空");

        Canvas reloaded = canvasRepository.findById(canvas.getId()).orElseThrow();
        assertEquals(Model3dStatus.NONE, reloaded.getModel3dStatus());
        assertNull(reloaded.getModel3dUrl());
    }

    @Test
    @DisplayName("重新打开:重新定稿能再次入队 3D(清空后不会被 SUCCESS 卡住)")
    void reopenThenFinalizeAgainEnqueuesAgain() {
        Canvas canvas = finalizedCanvasWithImage();
        canvasService.finalizeCanvas(canvas.getId(), userId);
        tx.executeWithoutResult(s -> {
            Canvas c = canvasRepository.findById(canvas.getId()).orElseThrow();
            c.setModel3dStatus(Model3dStatus.SUCCESS);
            c.setModel3dUrl("https://cdn.test/old-model.glb");
            canvasRepository.save(c);
        });

        canvasService.reopenCanvas(canvas.getId(), userId);
        Canvas refinalized = canvasService.finalizeCanvas(canvas.getId(), userId);

        assertEquals(Model3dStatus.PENDING, refinalized.getModel3dStatus(),
                "重新定稿应重新入队,而不是沿用上一轮的 SUCCESS");
    }

    @Test
    @DisplayName("重新打开:未定稿的画布不能重新打开;已锁定的也不能")
    void reopenRejectsWrongState() {
        Canvas designing = newCanvas("还在设计");
        assertBusinessError(ResultCode.BAD_REQUEST,
                () -> canvasService.reopenCanvas(designing.getId(), userId));

        Canvas locked = finalizedCanvasWithImage();
        locked.setLocked(true);
        canvasRepository.save(locked);
        assertBusinessError(ResultCode.CANVAS_LOCKED,
                () -> canvasService.reopenCanvas(locked.getId(), userId));
    }

    // ===== 3. 3D 重试的准入条件 =====

    @Test
    @DisplayName("重试 3D:只有 FAILED 才能重试,SUCCESS/PENDING/NONE 都拒绝")
    void retryModel3dOnlyAfterFailure() {
        Canvas canvas = finalizedCanvasWithImage();

        // NONE:还没定稿过
        assertBusinessError(ResultCode.BAD_REQUEST,
                () -> canvasService.retryModel3d(canvas.getId(), userId));

        // PENDING:定稿后正在排队,不允许重复提交占用配额
        canvasService.finalizeCanvas(canvas.getId(), userId);
        assertBusinessError(ResultCode.BAD_REQUEST,
                () -> canvasService.retryModel3d(canvas.getId(), userId));

        // FAILED:允许重试,回到 PENDING
        tx.executeWithoutResult(s -> {
            Canvas c = canvasRepository.findById(canvas.getId()).orElseThrow();
            c.setModel3dStatus(Model3dStatus.FAILED);
            canvasRepository.save(c);
        });
        canvasService.retryModel3d(canvas.getId(), userId);

        Canvas reloaded = canvasRepository.findById(canvas.getId()).orElseThrow();
        assertEquals(Model3dStatus.PENDING, reloaded.getModel3dStatus());
    }

    // ===== 4. 画布名称 =====

    @Test
    @DisplayName("画布名称:创建时去掉首尾空白并落库")
    void canvasNameTrimmedOnCreate() {
        Canvas canvas = newCanvas("  月光剑士  ");
        assertEquals("月光剑士", canvas.getName());
        assertEquals("月光剑士",
                canvasRepository.findById(canvas.getId()).orElseThrow().getName());
    }

    @Test
    @DisplayName("画布名称:空白名兜底为「未命名角色」,不能写进 NOT NULL 列")
    void canvasNameBlankFallback() {
        assertEquals("未命名角色", newCanvas("").getName());
        assertEquals("未命名角色", newCanvas("   ").getName());
        assertEquals("未命名角色", newCanvas(null).getName());
    }

    // ===== 5. 删除保护 =====

    @Test
    @DisplayName("删除保护:锁定画布不可删除")
    void lockedCanvasCannotBeDeleted() {
        Canvas canvas = newCanvas("已锁定的角色");
        canvas.setLocked(true);
        canvasRepository.save(canvas);

        assertBusinessError(ResultCode.CANVAS_LOCKED,
                () -> canvasService.deleteCanvas(canvas.getId(), userId));
        assertTrue(canvasRepository.findById(canvas.getId()).isPresent(), "被拒绝后画布必须还在");
    }

    @Test
    @DisplayName("删除保护:已被订单引用的画布不可删除(否则订单详情会指向不存在的角色)")
    void referencedCanvasCannotBeDeleted() {
        Canvas canvas = newCanvas("已被订单引用");
        Long orderId = orderRepository.save(OrderEntity.builder()
                .userId(userId).seriesId(seriesId).orderType(OrderType.MAIN)
                .status(OrderStatus.DRAFT_SUBMIT_PENDING)
                .expectedDeliveryDate(LocalDate.now().plusDays(30))
                .build()).getId();
        orderCanvasRepository.save(OrderCanvas.builder()
                .orderId(orderId).canvasId(canvas.getId())
                .lotteryResult(LotteryResult.SELECTED).build());

        assertBusinessError(ResultCode.CONFLICT,
                () -> canvasService.deleteCanvas(canvas.getId(), userId));
        assertTrue(canvasRepository.findById(canvas.getId()).isPresent(), "被拒绝后画布必须还在");
    }

    @Test
    @DisplayName("删除:未被引用的画布可以正常删除")
    void unreferencedCanvasCanBeDeleted() {
        Canvas canvas = newCanvas("没人用我");
        canvasService.deleteCanvas(canvas.getId(), userId);
        assertTrue(canvasRepository.findById(canvas.getId()).isEmpty(), "画布应已被删除");
    }

    // ===== 6. 系列设计状态联动 =====

    @Test
    @DisplayName("系列联动:定稿满档位要求后系列进入 READY_FOR_QUOTE,重新打开后退回")
    void seriesDesignStatusFollowsCanvasFinalize() {
        // LIGHT 档要求 6 个已定稿
        List<Long> ids = new java.util.ArrayList<>();
        for (int i = 0; i < SpecTier.LIGHT.getDesignCount(); i++) {
            Canvas c = finalizedCanvasWithImage();
            ids.add(c.getId());
            canvasService.finalizeCanvas(c.getId(), userId);
        }
        assertEquals(DesignStatus.READY_FOR_QUOTE,
                seriesRepository.findById(seriesId).orElseThrow().getDesignStatus(),
                "已定稿数量达到档位要求,系列应可报价");

        // 撤回一个:数量不再满足,应退回设计中
        canvasService.reopenCanvas(ids.get(ids.size() - 1), userId);

        assertEquals(DesignStatus.DESIGNING,
                seriesRepository.findById(seriesId).orElseThrow().getDesignStatus(),
                "画布数量不足后系列应退回设计中");
    }
}
