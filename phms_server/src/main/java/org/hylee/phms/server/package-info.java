/**
 * 后端服务主包。
 * <p>
 * 包结构约定（分层清晰、便于维护）：
 * <ul>
 *   <li>controller：HTTP 接口层（入参校验、返回包装）</li>
 *   <li>service：业务服务层（业务规则、聚合编排）</li>
 *   <li>mapper：MyBatis 数据访问接口（对应 resources/mapper/*.xml）</li>
 *   <li>persistence：数据库持久化对象（DO）</li>
 *   <li>dto：请求入参对象</li>
 *   <li>vo：响应展示对象</li>
 *   <li>config：配置与拦截器/初始化器</li>
 *   <li>exception：业务异常与全局异常处理</li>
 * </ul>
 */
package org.hylee.phms.server;

