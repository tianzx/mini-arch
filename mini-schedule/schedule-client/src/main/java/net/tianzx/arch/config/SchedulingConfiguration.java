package net.tianzx.arch.config;

import net.tianzx.arch.annotation.Scheduled;
import net.tianzx.arch.domain.ExecOrder;
import net.tianzx.arch.service.ZkCuratorServer;
import net.tianzx.arch.task.CronTaskRegister;
import net.tianzx.arch.task.SchedulingRunnable;
import net.tianzx.arch.utils.Constants;
import net.tianzx.arch.utils.StrUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static net.tianzx.arch.utils.Constants.Global.*;

public class SchedulingConfiguration implements ApplicationContextAware, BeanPostProcessor, ApplicationListener<ContextRefreshedEvent> {
    private Logger logger = LoggerFactory.getLogger(SchedulingConfiguration.class);

    private final Set<Class<?>> nonAnnotatedClasses = Collections.newSetFromMap(new ConcurrentHashMap<>(64));


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Constants.Global.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        try {
            ApplicationContext applicationContext = contextRefreshedEvent.getApplicationContext();
            //1. 初始化配置
            init_config(applicationContext);
            //2. 初始化服务
            init_server(applicationContext);
            //3. 启动任务
            init_task(applicationContext);
//            //4. 挂载节点
//            init_node();
//            //5. 心跳监听
//            HeartbeatService.getInstance().startFlushScheduleStatus();
            logger.info("middleware schedule init config、server、task、node、heart done!");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    //3. 启动任务
    private void init_task(ApplicationContext applicationContext) {
        CronTaskRegister cronTaskRegistrar = applicationContext.getBean("net-tianzx-arch-schedule-cronTaskRegister", CronTaskRegister.class);
        Set<String> beanNames = Constants.execOrderMap.keySet();
        for (String beanName : beanNames) {
            List<ExecOrder> execOrderList = Constants.execOrderMap.get(beanName);
            for (ExecOrder execOrder : execOrderList) {
                if (!execOrder.getAutoStartup()) continue;
                SchedulingRunnable task = new SchedulingRunnable(execOrder.getBean(), execOrder.getBeanName(), execOrder.getMethodName());
                cronTaskRegistrar.addCronTask(task, execOrder.getCron());
            }
        }
    }


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
        if (this.nonAnnotatedClasses.contains(targetClass)) return bean;
        Method[] methods = ReflectionUtils.getAllDeclaredMethods(bean.getClass());
        for (Method method : methods) {
            Scheduled scheduled = AnnotationUtils.findAnnotation(method, Scheduled.class);
            if (null == scheduled || 0 == method.getDeclaredAnnotations().length) continue;
            List<ExecOrder> execOrderList = Constants.execOrderMap.computeIfAbsent(beanName, k -> new ArrayList<>());
            ExecOrder execOrder = new ExecOrder();
            execOrder.setBean(bean);
            execOrder.setBeanName(beanName);
            execOrder.setMethodName(method.getName());
            execOrder.setDesc(scheduled.desc());
            execOrder.setCron(scheduled.cron());
            execOrder.setAutoStartup(scheduled.autoStartup());
            execOrderList.add(execOrder);
        }
        this.nonAnnotatedClasses.add(targetClass);
        return bean;
    }

    //1. 初始化配置
    private void init_config(ApplicationContext applicationContext) {
        try {
            StarterServiceProperties properties = applicationContext.getBean("net-tianzx-arch-schedule-starterAutoConfig", StarterAutoConfig.class).getProperties();
            Constants.Global.zkAddress = properties.getZkAddress();
            schedulerServerId = properties.getSchedulerServerId();
            Constants.Global.schedulerServerName = properties.getSchedulerServerName();
            InetAddress id = InetAddress.getLocalHost();
            Constants.Global.ip = id.getHostAddress();
        } catch (Exception e) {
            logger.error("middleware schedule init config error！", e);
            throw new RuntimeException(e);
        }
    }
    //2. 初始化服务
    private void init_server(ApplicationContext applicationContext) {
        try {
            //获取zk连接
            CuratorFramework client = ZkCuratorServer.getClient(Constants.Global.zkAddress);
            //节点组装
            path_root_server = StrUtil.joinStr(path_root, LINE, "server", LINE, schedulerServerId);
            path_root_server_ip = StrUtil.joinStr(path_root_server, LINE, "ip", LINE, Constants.Global.ip);
            //创建节点&递归删除本服务IP下的旧内容
            ZkCuratorServer.deletingChildrenIfNeeded(client, path_root_server_ip);
            ZkCuratorServer.createNode(client, path_root_server_ip);
            ZkCuratorServer.setData(client, path_root_server, schedulerServerName);
            //添加节点&监听
            ZkCuratorServer.createNodeSimple(client, Constants.Global.path_root_exec);
            ZkCuratorServer.addTreeCacheListener(applicationContext, client, Constants.Global.path_root_exec);
        } catch (Exception e) {
            logger.error("middleware schedule init server error！", e);
            throw new RuntimeException(e);
        }
    }


}
