/**
 * 控制器层（HTTP API）。
 * <p>
 * 职责边界：
 * <ul>
 *   <li>定义路由与请求/响应模型</li>
 *   <li>进行参数校验（{@code @Valid}/{@code @Validated}）</li>
 *   <li>调用 service 完成业务</li>
 *   <li>统一返回 {@link org.hylee.phms.common.api.ApiResponse}</li>
 * </ul>
 * 不建议在 controller 编写复杂业务逻辑，以保持可测试性与可维护性。
 */
package org.hylee.phms.server.controller;

