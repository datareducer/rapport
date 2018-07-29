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
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerConfigurationFactory;
import org.glassfish.jersey.server.mvc.spi.TemplateProcessor;

import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Кастомный шаблонизатор Freemarker с поддержкой StringTemplateLoader
 */
@Provider
class CustomFreemarkerTemplateProcessor implements TemplateProcessor<String> {

    private final FreemarkerConfigurationFactory factory;

    public CustomFreemarkerTemplateProcessor(@Context ServletContext servletContext) {
        this.factory = (FreemarkerConfigurationFactory) servletContext.getAttribute("freemarkerConfigurationFactory");
    }

    @Override
    public String resolve(String path, MediaType mediaType) {
        return path.substring(path.lastIndexOf("/"), path.length());
    }

    @Override
    public void writeTo(String templateReference, Viewable viewable, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream out) throws IOException {

        Template template = factory.getConfiguration().getTemplate(templateReference);

        try {
            Map<String, Object> model;
            if (viewable.getModel() instanceof Map) {
                model = (Map<String, Object>) viewable.getModel();
            } else {
                model = new HashMap<>();
                model.put("model", viewable.getModel());
            }

            template.process(model, new OutputStreamWriter(out, StandardCharsets.UTF_8));
        } catch (TemplateException te) {
            throw new ContainerException(te);
        }
    }

}
