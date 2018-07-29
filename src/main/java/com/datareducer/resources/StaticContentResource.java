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

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.io.InputStream;

import javax.activation.MimetypesFileTypeMap;
import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Singleton
@Path("/")
public class StaticContentResource {
    @Context
    private ServletContext servletContext;

    private final CacheControl cacheControl = new CacheControl();

    public StaticContentResource() {
        // Ответ должен кэшироваться браузером на сутки
        cacheControl.setMaxAge(86400);
    }

    @GET
    @Path("/files/{resourceName}/{requestId}/{file}")
    @Produces("application/octet-stream, text/html, image/*")
    public Response getFile(@Context UriInfo uriInfo, @PathParam("file") String file) {
        InputStream stream = servletContext.getResourceAsStream(uriInfo.getPath());
        if (stream == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        String mt = new MimetypesFileTypeMap().getContentType(file);
        return Response.ok(stream, mt).build();
    }

    @GET
    @Path("/css/{file:[^\\s]+((\\.css)|(\\.css\\.map))$}")
    @Produces("text/css")
    public Response getStylesheet(@PathParam("file") String file) {
        InputStream stream = servletContext.getResourceAsStream("css/" + file);
        if (stream == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        return Response.ok(stream).cacheControl(cacheControl).build();
    }

    @GET
    @Path("/js/{file:[^\\s]+\\.js$}")
    @Produces("application/javascript")
    public Response getJavaScript(@PathParam("file") String file) {
        InputStream stream = servletContext.getResourceAsStream("js/" + file);
        if (stream == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        return Response.ok(stream).cacheControl(cacheControl).build();
    }
    
    @GET
    @Path("/img/{file:[^\\s]+((\\.png)|(\\.jpg)|(\\.gif)|(\\.ico))$}")
    @Produces("image/*")
    public Response getImage(@PathParam("file") String file) {
        InputStream stream = servletContext.getResourceAsStream("img/" + file);
        if (stream == null) {
            throw new WebApplicationException(NOT_FOUND);
        }
        String mt = new MimetypesFileTypeMap().getContentType(file);
        return Response.ok(stream, mt).cacheControl(cacheControl).build();
    }

}
