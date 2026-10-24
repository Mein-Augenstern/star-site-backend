package com.xp.starsite.member.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.xp.starsite.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 微信 jscode2session 网关（infrastructure 层）：登录用例与微信外部系统的唯一边界。
 * <p>
 * 双通道：配置 wx.mp.secret 走真实 jscode2session；未配置走 mock（code 即 openid），
 * 开发态不依赖真 secret 即可全链路跑通。session_key 仅随结果返回、由上层落库，永不下发。
 *
 * @author muyi
 * @since 2026-10
 */
@Slf4j
@Component
public class WxSessionGateway {

    /**
     * @param openid    本端唯一标识
     * @param sessionKey 微信会话密钥（敏感，仅落库）
     * @param unionid   跨端归一标识，可能为 null
     */
    public record WxSession(String openid, String sessionKey, String unionid) {
    }

    @Value("${wx.mp.appid:}")
    private String appid;

    @Value("${wx.mp.secret:}")
    private String secret;

    /** 复用 RestClient（资源规约：避免每请求新建连接栈） */
    private final RestClient restClient = RestClient.create();

    public WxSession fetch(String code) {
        if (secret == null || secret.isBlank()) {
            // mock 通道：code 即 openid，开发态全链路不依赖真 secret
            log.info("wx-login mock 通道（未配置 wx.mp.secret）: code={}", code);
            return new WxSession(code, "sk-mock-" + code.hashCode(), null);
        }
        JsonNode resp;
        try {
            resp = restClient.get()
                    .uri("https://api.weixin.qq.com/sns/jscode2session?appid={a}&secret={s}&js_code={c}&grant_type=authorization_code",
                            appid, secret, code)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.warn("jscode2session 调用失败: {}", e.getMessage());
            throw new BizException(503, "微信登录服务暂不可用，请稍后再试");
        }
        if (resp == null || resp.path("errcode").asInt(0) != 0) {
            int err = resp == null ? -1 : resp.path("errcode").asInt(-1);
            log.warn("jscode2session 返回错误: {}", resp);
            if (err == 40029 || err == 40163) {
                throw new BizException(400, "登录凭证无效，请重试");
            }
            throw new BizException(503, "微信登录失败，请稍后再试");
        }
        return new WxSession(resp.path("openid").asText(),
                resp.path("session_key").asText(null),
                resp.hasNonNull("unionid") ? resp.path("unionid").asText() : null);
    }
}
