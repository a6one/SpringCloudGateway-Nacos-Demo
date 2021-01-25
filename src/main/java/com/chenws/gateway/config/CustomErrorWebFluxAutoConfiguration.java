package com.chenws.gateway.config;

import com.chenws.gateway.exception.CustomErrorWebExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义全局异常拦截
 * 参考{@link org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration}
 * @author chenws
 * @date 2019/12/20 15:47:45
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE) //
@ConditionalOnClass(WebFluxConfigurer.class)
@AutoConfigureBefore(WebFluxAutoConfiguration.class)
@EnableConfigurationProperties({ ServerProperties.class, ResourceProperties.class })
public class CustomErrorWebFluxAutoConfiguration {

    private final ServerProperties serverProperties;

    private final ApplicationContext applicationContext;

    private final ResourceProperties resourceProperties;

    private final List<ViewResolver> viewResolvers;

    private final ServerCodecConfigurer serverCodecConfigurer;

    // 技巧1：利用Ioc容器扫描时候，会自动调用构造方法
    public CustomErrorWebFluxAutoConfiguration(ServerProperties serverProperties, //springboot-autoconfigure --web中服务端配置
                                               ResourceProperties resourceProperties, //springboot-autoconfigure --web中服务端配置
                                               ObjectProvider<ViewResolver> viewResolversProvider,//webFlux中的视图解析器
                                               ServerCodecConfigurer serverCodecConfigurer, //编解码器
                                               ApplicationContext applicationContext) {
        this.serverProperties = serverProperties;
        this.applicationContext = applicationContext;
        this.resourceProperties = resourceProperties;
        // 技巧2：使用ObjectProvider.orderedStream(),获取所有的类型
        // 返回符合条件对象的连续的Stream。在标注Spring应用上下文中采用@Order注解或实现Order接口的顺序
        this.viewResolvers = viewResolversProvider.orderedStream().collect(Collectors.toList());
//        viewResolversProvider.orderedStream().forEachOrdered(); 顺序遍历

        this.serverCodecConfigurer = serverCodecConfigurer;
    }

    @Bean
    // 只有当当前目录没有 ErrorWebExceptionHandler 类型的时候才加载
    @ConditionalOnMissingBean(value = ErrorWebExceptionHandler.class, search = SearchStrategy.CURRENT)
    @Order(-3)
    public ErrorWebExceptionHandler errorWebExceptionHandler(ErrorAttributes errorAttributes) {
        // 设置自定义的全局异常处理器
        CustomErrorWebExceptionHandler customErrorWebExceptionHandler = new CustomErrorWebExceptionHandler(
                errorAttributes,
                resourceProperties,
                this.serverProperties.getError(),
                applicationContext);
        customErrorWebExceptionHandler.setViewResolvers(this.viewResolvers);
        customErrorWebExceptionHandler.setMessageWriters(this.serverCodecConfigurer.getWriters());
        customErrorWebExceptionHandler.setMessageReaders(this.serverCodecConfigurer.getReaders());
        return customErrorWebExceptionHandler;
    }

    @Bean
    @ConditionalOnMissingBean(value = ErrorAttributes.class, search = SearchStrategy.CURRENT)
    public DefaultErrorAttributes errorAttributes() {
        return new DefaultErrorAttributes(this.serverProperties.getError().isIncludeException());
    }
}
