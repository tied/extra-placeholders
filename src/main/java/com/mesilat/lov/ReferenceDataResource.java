package com.mesilat.lov;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.upm.api.license.PluginLicenseManager;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import net.java.ao.DBParam;
import net.java.ao.Query;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/refdata")
@Scanned
public class ReferenceDataResource {
    public static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.lov-placeholder");

    private final ActiveObjects ao;
    private final I18nResolver resolver;
    private final PluginLicenseManager licenseManager;

    @GET
    @Path("/{code}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@PathParam("code") String code){
        return ao.executeInTransaction(()->{
            ReferenceData data = ao.get(ReferenceData.class, code);
            if (data == null){
                return Response.status(Response.Status.NOT_FOUND).build();
            } else {
                Map<String,Object> response = new HashMap<>();
                response.put("code", data.getCode());
                response.put("name", data.getName());
                response.put("data", data.getData());
                response.put("type", data.getType());
                response.put("status", data.getStatus());
                return Response.ok(response).build();
            }
        });
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(){
        return ao.executeInTransaction(()->{
            Query query = Query.select("CODE,NAME").order("NAME");
            ReferenceData[] data = ao.find(ReferenceData.class, query);
            List<Map<String,Object>> result = new ArrayList<>();
            Arrays.asList(data).stream().forEach((d)->{
                Map<String,Object> map = new HashMap<>();
                map.put("code", d.getCode());
                map.put("name", d.getName());
                result.add(map);
            });
            return Response.ok(result).build();
        });
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(ObjectNode data){
        return ao.executeInTransaction(()->{
            boolean isJavascript = isDataJavascript(data.get("data").asText());
            if (isJavascript && !isLicensed()){
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Non-licensed version of the addon cannot be used to provide javascript data sources")
                    .build();
            }
            
            if (data.get("oldCode") != null){
                ReferenceData _data = ao.get(ReferenceData.class, data.get("oldCode").asText());
                if (_data != null){
                    ao.delete(_data);
                }
            }
            ReferenceData _data = ao.get(ReferenceData.class, data.get("code").asText());
            if (_data != null){
                ao.delete(_data);
            }
            _data = ao.create(ReferenceData.class,
                new DBParam("CODE", data.get("code").asText()),
                new DBParam("NAME", data.get("name").asText())
            );
            _data.setType(isJavascript? ReferenceData.TYPE_JAVASCRIPT: ReferenceData.TYPE_LIST_OF_STRINGS);
            _data.setData(data.get("data").asText());
            _data.setStatus(ReferenceData.STATUS_EDITED);
            _data.save();
            return Response.ok(data).build();
        });
    }

    @DELETE
    @Path("/{code}")
    public Response delete(@PathParam("code") String code){
        return ao.executeInTransaction(()->{
            ReferenceData data = ao.get(ReferenceData.class, code);
            if (data == null){
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(resolver.getText("com.mesilat.lov-placeholder.setting.msg.data-not-found"))
                    .build();
            } else {
                ao.delete(data);
                return Response.ok().build();
            }
        });
    }

    private static boolean isDataJavascript(String data){
        data = data.trim();
        if (!data.startsWith("{") || !data.endsWith("}")){
            return false;
        }
        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByName("nashorn");
        if (engine == null){
            try {
                File nashornPath = new File( System.getProperty("java.home") + "/lib/ext/nashorn.jar" );
                if (!nashornPath.exists()){
                    LOGGER.warn("Could not find nashorn script engine library");
                    return false;
                }   String nashornUrl = "jar:file://" + nashornPath.getAbsolutePath() + "!/";
                URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{ new URL(nashornUrl) });
                Class nashornClass = Class.forName("jdk.nashorn.api.scripting.NashornScriptEngineFactory", true, urlClassLoader);
                engineManager.registerEngineName("nashorn", (ScriptEngineFactory)nashornClass.newInstance());
                engine = engineManager.getEngineByName("nashorn");
            } catch (MalformedURLException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                LOGGER.warn("Failed to load nashorn script engine library", ex);
                return false;
            }
        }
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("function getReferenceData(){return ")
            .append(data)
            .append(";}");        
            engine.eval(sb.toString());
            return true; // yes, it's javascript!
        } catch(Throwable t){
            return false;
        }
    }

    protected boolean isLicensed() {
        try {
            return licenseManager.getLicense().get().isValid();
        } catch(Throwable ignore) {
            return false;
        }
    }

    @Inject
    public ReferenceDataResource(
        final @ComponentImport ActiveObjects ao,
        final @ComponentImport I18nResolver resolver,
        final @ComponentImport PluginLicenseManager licenseManager
    ){
        this.ao = ao;
        this.resolver = resolver;
        this.licenseManager = licenseManager;
    }
}