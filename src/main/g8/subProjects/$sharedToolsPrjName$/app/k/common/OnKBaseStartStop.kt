package k.common

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jodd.datetime.JDateTime
import jodd.util.ClassLoaderUtil
import k.common.json.JDateTimeJsonDeserializer
import k.common.json.JDateTimeJsonSerializer
import k.task.PlanTaskService
import play.Application
import play.Configuration
import play.cache.CacheApi
import play.data.FormFactory
import play.inject.ApplicationLifecycle
import play.libs.Json
import java.io.File
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by kk on 16/5/5.
 */

@Singleton
class OnKBaseStartStop
@Inject
constructor(applicationLifecycle: ApplicationLifecycle,
            application: Application,
            configuration: Configuration,
            cacheApi: CacheApi,
            formFactory: FormFactory) {

    private val tasksWhenStop = mutableListOf<Runnable>()

    init {
        // 添加 EbeanConfig 依赖, 确保在此之前, Ebean 已经正常初始化

        Hub.setApplication(application)
        Hub.setConfiguration(configuration)
        Hub.setCacheApi(cacheApi)
        Hub.setFormFactory(formFactory)

        applicationLifecycle.addStopHook {
            OnStop()
            CompletableFuture.completedFuture<Any>(null)
        }

        OnStart()
    }

    private fun OnStart() {
        Helper.DLog(AnsiColor.GREEN, "KBase OnStart() ...")
        RegistJacksonModule()

        if (PlanTaskService.Enabled()) {
            PlanTaskService.Start()
        }
    }

    private fun OnStop() {
        Helper.DLog(AnsiColor.GREEN, "KBase OnStop() ...")
        if (PlanTaskService.Enabled()) {
            PlanTaskService.Stop()
        }
        if (tasksWhenStop.size > 0) {
            Helper.DLog(AnsiColor.GREEN, "KBase run registed stop tasks ...")
            tasksWhenStop.forEach {
                it.run()
            }
        }
    }

    private fun RegistJacksonModule() {
        val module = SimpleModule("CustomTypeModule")
        module.addSerializer(JDateTime::class.java, JDateTimeJsonSerializer())
        module.addDeserializer(JDateTime::class.java, JDateTimeJsonDeserializer())

        Json.mapper().registerModule(module)
        Json.mapper().registerKotlinModule()
    }

    fun RegStopTask(task:Runnable) {
        tasksWhenStop.add(task)
    }
}
