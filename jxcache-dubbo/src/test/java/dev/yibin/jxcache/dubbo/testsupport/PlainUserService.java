package dev.yibin.jxcache.dubbo.testsupport;

/**
 * 用于验证未声明缓存注解时，包装器会直接回退到原始 Dubbo 代理。
 */
public interface PlainUserService {

    String ping(String userId);
}
