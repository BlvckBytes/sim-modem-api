package me.blvckbytes.simmodemapi.rest.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class AsyncConfiguration : WebMvcConfigurer {

  override fun configureAsyncSupport(configurer: AsyncSupportConfigurer) {
    configurer.setDefaultTimeout(10 * 1000)
    super.configureAsyncSupport(configurer)
  }
}