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

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.mvc.Viewable;

import com.datareducer.model.ReducerConfiguration;
import com.datareducer.model.Script;
import com.datareducer.model.ScriptException;
import com.datareducer.model.ScriptParameter;
import com.datareducer.model.ScriptResult;
import com.datareducer.model.UndefinedParameterException;

import freemarker.cache.StringTemplateLoader;

@Singleton
@Path("/")
public class IndexResource {
    private final static String ROLE_ADMIN = "DataReducerAdministrator";

    @Context
    ServletContext servletContext;

    private static final Logger log = LogManager.getFormatterLogger(IndexResource.class);

    @GET
    @Produces("text/html")
    public Viewable getIndex(@Context SecurityContext sc) {
        ReducerConfiguration config = (ReducerConfiguration) servletContext.getAttribute("configuration");
        if (config == null) {
            log.warn("Конфигурация приложения не инициализирована");
        }
        boolean isAdmin = sc.isUserInRole(ROLE_ADMIN);
        List<Script> scripts = new ArrayList<>();
        if (config != null) {
            if (isAdmin) {
                scripts.addAll(config.getScripts());
            } else {
                for (Script script : config.getScripts()) {
                    for (String role : script.getSecurityRoles()) {
                        if (!role.isEmpty() && sc.isUserInRole(role)) {
                            scripts.add(script);
                            break;
                        }
                    }
                }
            }
        }
        Map<String, Object> model = new HashMap<>();
        model.put("scripts", scripts);
        
        return new Viewable("/index.ftl", model);
    }

    @GET
    @Path("/{resource}")
    @Produces({ "application/xml", "application/json" })
    public ScriptResult getScriptResult(@Context UriInfo ui, @Context SecurityContext sc,
            @PathParam("resource") String resource) {

        Script script = getScript(resource);
        if (script == null || !script.isWebAccess()) {
            throw new WebApplicationException(NOT_FOUND);
        }

        checkPermissions(sc, script);

        List<ScriptParameter> clientParams = new ArrayList<>();
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        for (String key : queryParams.keySet()) {
            clientParams.add(new ScriptParameter(key, queryParams.getFirst(key), true));
        }
        
        String requestId = Script.generateRequestId(clientParams);
        if (requestId == null) {
            throw new WebApplicationException(INTERNAL_SERVER_ERROR);
        }
        
        ExecutorService executor = (ExecutorService) servletContext.getAttribute("executor");
        if (executor == null) {
            throw new WebApplicationException(INTERNAL_SERVER_ERROR);
        }
        
        ScriptResult scriptResult;
        try {
            scriptResult = script.execute(executor,requestId, clientParams);
        } catch (UndefinedParameterException | ScriptException e) {
            // XXX ThrowableMapper не перехватывает WebApplicationException, для которых установлен entity 
            Response resp;
            if (Boolean.parseBoolean(servletContext.getInitParameter("debugMode"))) {
                resp = Response.status(INTERNAL_SERVER_ERROR).entity(e.getMessage()).type("text/plain; charset=utf-8").build();
            } else {
                resp = Response.status(INTERNAL_SERVER_ERROR).build();
            }
            throw new WebApplicationException(resp); 
        }
        
        return scriptResult;
    }

    @GET
    @Path("/{resource}")
    @Produces("text/html")
    public Viewable getScriptResultViewable(@Context UriInfo ui, @Context SecurityContext sc,
            @PathParam("resource") String resource) {

        Script script = getScript(resource);
        if (script == null || !script.isWebAccess()) {
            throw new WebApplicationException(NOT_FOUND);
        }

        checkPermissions(sc, script);

        List<ScriptParameter> clientParams = new ArrayList<>();
        MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
        for (String key : queryParams.keySet()) {
            clientParams.add(new ScriptParameter(key, queryParams.getFirst(key), true));
        }
        
        String requestId = Script.generateRequestId(clientParams);
        if (requestId == null) {
            throw new WebApplicationException(INTERNAL_SERVER_ERROR);
        }
        
        ExecutorService executor = (ExecutorService) servletContext.getAttribute("executor");
        if (executor == null) {
            throw new WebApplicationException(INTERNAL_SERVER_ERROR);
        }
        
        ScriptResult scriptResult;
        try {
            scriptResult = script.execute(executor,requestId, clientParams);
        } catch (UndefinedParameterException | ScriptException e) {
            // XXX ThrowableMapper не перехватывает WebApplicationException, для которых установлен entity 
            Response resp;
            if (Boolean.parseBoolean(servletContext.getInitParameter("debugMode"))) {
                resp = Response.status(INTERNAL_SERVER_ERROR).entity(e.getMessage()).type("text/plain; charset=utf-8").build();
            } else {
                resp = Response.status(INTERNAL_SERVER_ERROR).build();
            }
            throw new WebApplicationException(resp); 
        }

        Map<String, Object> model = new HashMap<>();

        for (ScriptParameter param : scriptResult.getParameters()) {
            model.put((param.getName()), param.getValue());
        }

        model.put("dataFrame", scriptResult.getDataFrame());

        StringTemplateLoader stringLoader = (StringTemplateLoader) servletContext.getAttribute("stringTemplateLoader");
        String template = script.isUseDefaultTemplate() ? Script.getDefaultTemplate() : script.getTemplate();
        stringLoader.putTemplate(script.getResourceName(), template);

        return new Viewable(script.getResourceName(), model);
    }

    private void checkPermissions(SecurityContext sc, Script script) {
        if (sc.isUserInRole(ROLE_ADMIN)) {
            return;
        }
        boolean isAllowed = false;
        for (String role : script.getSecurityRoles()) {
            if (!role.isEmpty() && sc.isUserInRole(role)) {
                isAllowed = true;
                break;
            }
        }
        if (!isAllowed) {
            throw new WebApplicationException(FORBIDDEN);
        }
    }

    private Script getScript(String resourceName) {
        ReducerConfiguration config = (ReducerConfiguration) servletContext.getAttribute("configuration");
        if (config == null) {
            throw new WebApplicationException(INTERNAL_SERVER_ERROR);
        }
        for (Script script : config.getScripts()) {
            if (script.getResourceName().equalsIgnoreCase(resourceName)) {
                return script;
            }
        }
        return null;
    }

}
