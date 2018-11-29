/*
 * Copyright (c) 2017,2018 Kirill Mikhaylov <admin@datareducer.ru>
 * 
 * Этот файл — часть программы DataReducer <http://datareducer.ru>.
 *
 * Программа DataReducer является свободным программным обеспечением.
 * Вы вправе распространять ее и/или модифицировать в соответствии с условиями версии 2
 * либо, по вашему выбору, с условиями более поздней версии 
 * Стандартной Общественной Лицензии GNU, опубликованной Free Software Foundation. 
 *
 * Программа DataReducer распространяется в надежде, что она будет полезной,
 * но БЕЗО ВСЯКИХ ГАРАНТИЙ, в том числе ГАРАНТИИ ТОВАРНОГО СОСТОЯНИЯ ПРИ ПРОДАЖЕ
 * и ПРИГОДНОСТИ ДЛЯ ИСПОЛЬЗОВАНИЯ В КОНКРЕТНЫХ ЦЕЛЯХ.
 * Подробнее см. в Стандартной Общественной Лицензии GNU.
 *
 * Вы должны были получить копию Стандартной Общественной Лицензии GNU
 * вместе с этой программой. Если это не так, см.
 * <https://www.gnu.org/licenses/>.
 */
package com.datareducer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.datareducer.model.ReducerConfiguration;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;

public class ReducerContextListener implements ServletContextListener {
    private ReducerConfiguration configuration;
    private ExecutorService executor;

    private static final Logger log = LogManager.getFormatterLogger(ReducerContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();

        Properties props = new Properties();
        
        try (InputStream propsStream = context.getResourceAsStream("WEB-INF/datareducer.properties");) {
            if (propsStream != null) {
                props.load(propsStream);
            } else {
                log.error("Не найден файл параметров приложения datareducer.properties");
            }
        } catch (IOException e) {
            log.error("При загрузке параметров приложения", e);
        }

        Map<String, String> applicationParams = new HashMap<>();
        for (String name : props.stringPropertyNames()) {
            applicationParams.put(name, props.getProperty(name));
        }
        
        context.setAttribute("applicationParams", Collections.unmodifiableMap(applicationParams));

        try (InputStream confStream = context.getResourceAsStream("WEB-INF/model/datareducer.xml");) {
            if (confStream != null) {
                JAXBContext jc = ReducerConfiguration.getJaxbContext();
                configuration = (ReducerConfiguration) jc.createUnmarshaller().unmarshal(confStream);
            } else {
                log.error("Не найден файл модели datareducer.xml");
            }
        } catch (JAXBException | IOException e) {
            log.error("При разборе файла модели datareducer.xml", e);
        }

        if (configuration != null) {
            configuration.setApplicationParams(applicationParams);
            context.setAttribute("configuration", configuration);
            log.info("Модель успешно инициализирована");
            
            // Оключаем запуск OrientDB engine, если кэширование не требуется.
            if (configuration.getInfoBases().isEmpty()) {
                OGlobalConfiguration.INIT_IN_SERVLET_CONTEXT_LISTENER.setValue(false);
            }
        }

        executor = Executors.newCachedThreadPool();
        context.setAttribute("executor", executor);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (configuration != null) {
            configuration.close();
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

}
