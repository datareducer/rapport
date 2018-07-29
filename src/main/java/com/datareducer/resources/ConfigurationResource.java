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
package com.datareducer.resources;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.datareducer.model.ReducerConfiguration;

@Singleton
@Path("/configuration")
public class ConfigurationResource {
    @Context
    ServletContext servletContext;
    
    private static final Logger log = LogManager.getFormatterLogger(ConfigurationResource.class);
    
    @PUT
    public Response updateConfiguration(ReducerConfiguration newConfig) {
        if (newConfig == null) {
            throw new WebApplicationException(BAD_REQUEST);
        }
        
        String webInfPath = servletContext.getRealPath("WEB-INF/model");
        File file = new File(webInfPath + "/datareducer.xml");

        try {
            Marshaller jm = ReducerConfiguration.getJaxbContext().createMarshaller();
            jm.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jm.marshal(newConfig, file);
        } catch (JAXBException e) {
            log.error("Не удалось записать модель", e);
            throw new WebApplicationException(INTERNAL_SERVER_ERROR); 
        }
        
        ReducerConfiguration currentConf = (ReducerConfiguration) servletContext.getAttribute("configuration");
        if (currentConf != null) {
            currentConf.close();
        }
        
        Map<String, String> appParams = (Map<String, String>) servletContext.getAttribute("applicationParams");
        newConfig.setApplicationParams(appParams);
        servletContext.setAttribute("configuration", newConfig);
        
        log.info("Модель успешно обновлена");

        return Response.accepted().build();
    }
    
    @GET
    public ReducerConfiguration getConfiguration() {
        ReducerConfiguration config = (ReducerConfiguration) servletContext.getAttribute("configuration");
        if (config == null) {
            log.warn("Не обнаружена модель приложения");
            throw new WebApplicationException(NOT_FOUND); 
        }
        return config;
    }
    
}
