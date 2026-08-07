package com.diyfigure.common.exception;

import com.diyfigure.common.response.ResultCode;

/**
 * 非法状态机转换异常
 * 当订单状态转换不符合 ALLOWED_TRANSITIONS 映射表时抛出
 *
 * 这是状态机正确性的核心保障,任何绕过 transition() 方法直接修改状态的行为
 * 都不应该存在,一旦出现此异常说明代码逻辑有 bug
 */
public class IllegalStateTransitionException extends BusinessException {

    public IllegalStateTransitionException(String fromStatus, String toStatus) {
        super(ResultCode.ILLEGAL_STATE_TRANSITION,
                String.format("非法的状态转换: %s → %s", fromStatus, toStatus));
    }
}
