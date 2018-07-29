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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerConfigurationFactory;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerDefaultConfigurationFactory;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature;

import com.datareducer.model.ReducerConfiguration;
import com.datareducer.resources.ConfigurationResource;
import com.datareducer.resources.IndexResource;
import com.datareducer.resources.StaticContentResource;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.StringTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.cache.WebappTemplateLoader;
import freemarker.template.Configuration;

public class ReducerApplication extends ResourceConfig {
    public ReducerApplication(@Context ServletContext servletContext) {
        registerInstances(new IndexResource(), new StaticContentResource(), new ConfigurationResource());

        register(FreemarkerMvcFeature.class);
        register(CustomFreemarkerTemplateProcessor.class);

        register(ReducerConfiguration.getConfigBodyReader());
        register(ReducerConfiguration.getConfigBodyWriter());

        FreemarkerConfigurationFactory configurationFactory = new CustomFreemarkerConfigurationFactory(servletContext);
        servletContext.setAttribute("freemarkerConfigurationFactory", configurationFactory);

        property(MvcFeature.TEMPLATE_BASE_PATH, "/templates");
        property(FreemarkerMvcFeature.TEMPLATE_OBJECT_FACTORY, configurationFactory);
        
        if (!Boolean.parseBoolean(servletContext.getInitParameter("debugMode"))) {
            register(ThrowableMapper.class);
        }
    }

    /**
     * Фабрика кастомной конфигурации Freemarker с поддержкой StringTemplateLoader и установкой кодировки шаблонов.
     */
    private class CustomFreemarkerConfigurationFactory implements FreemarkerConfigurationFactory {
        private final Configuration configuration;

        public CustomFreemarkerConfigurationFactory(@Context ServletContext servletContext) {
            super();

            List<TemplateLoader> loaders = new ArrayList<>();

            // Лоадеры WebappTemplateLoader, ClassTemplateLoader и FileTemplateLoader
            // добавляются для поддержки базовой функциональности Jersey
            loaders.add(new WebappTemplateLoader(servletContext));
            loaders.add(new ClassTemplateLoader(FreemarkerDefaultConfigurationFactory.class, "/"));
            try {
                loaders.add(new FileTemplateLoader(new File("/")));
            } catch (IOException e) {
                e.printStackTrace();
            }

            StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
            servletContext.setAttribute("stringTemplateLoader", stringTemplateLoader);
            
            // Обход бага, в результате которого шаблонизатор в некоторых случаях не находит шаблон index.ftl,
            // когда подключен StringTemplateLoader: "Template not found for name "/index.ftl"".
            String indexFtl = null;
            try (InputStream indexFtlIs = servletContext.getResourceAsStream("WEB-INF/classes/templates/index.ftl")) {
                indexFtl = new BufferedReader(new InputStreamReader(indexFtlIs)).lines().collect(Collectors.joining("\n"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (indexFtl != null) {
                stringTemplateLoader.putTemplate("index.ftl", indexFtl);
            }

            loaders.add(stringTemplateLoader);

            configuration = new Configuration(Configuration.VERSION_2_3_23);
            
            configuration.setTemplateLoader(new MultiTemplateLoader(loaders.toArray(new TemplateLoader[loaders.size()])));

            configuration.setDefaultEncoding("UTF-8");
            configuration.setOutputEncoding("UTF-8");
            configuration.setURLEscapingCharset("UTF-8");
        }

        @Override
        public Configuration getConfiguration() {
            return configuration;
        }
    }

}
