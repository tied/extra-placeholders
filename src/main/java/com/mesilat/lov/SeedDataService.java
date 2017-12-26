package com.mesilat.lov;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.tenant.TenantedContainerContext;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.spring.container.AtlassianBeanFactory;
import com.atlassian.spring.container.ContainerManager;
import com.atlassian.spring.container.SpringContainerContext;
import com.mesilat.countries.CountriesService;
import com.mesilat.currencies.CurrenciesService;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.inject.Named;
import net.java.ao.DBParam;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

@Named
public class SeedDataService implements InitializingBean, DisposableBean {
    private static final String SINGLETON_NAME = "com.mesilat:lov-placeholder:seedDataService";
    public static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.lov-placeholder");

    private final ActiveObjects ao;
    private final LocaleManager localeManager;
    private final I18nResolver resolver;
    private final CountriesService countriesService;
    private final CurrenciesService currenciesService;

    @Override
    public void afterPropertiesSet() throws Exception {
        registerBean(SINGLETON_NAME);
        Thread t = new Thread(()->{
            createSeedData();
        });
        t.start();
    }
    @Override
    public void destroy() throws Exception {
        unregisterBean(SINGLETON_NAME);
    }

    private void createSeedData(){
        try {
            ao.moduleMetaData().awaitInitialization();
        } catch (ExecutionException | InterruptedException ex) {
            LOGGER.warn("Failure waiting to init AO", ex);
        }
        ao.executeInTransaction(()->{
            createSeedData("MNTH", "months", false);
            createSeedData("WEEK", "weekDays", false);
            createSeedData("PAGE", "pages");
            //createSeedData("countries");
            //createSeedData("currencies");

            countriesService.registerReferenceData(this);
            currenciesService.registerReferenceData(this);
            return null;
        });
    }
    private void createSeedData(String code, String name, boolean sort) {
        try {
            StringBuilder sb = new StringBuilder();
            try (InputStream in = getResourceStream(name)) {
                Properties props = new Properties();
                props.load(in);
                if (sort){
                    props.entrySet().stream().sorted((a,b)->{
                        String _a = a.getValue().toString();
                        String _b = b.getValue().toString();
                        return _a.compareTo(_b);
                    }).forEach((e)->{
                        sb.append(e.getValue()).append("\n");
                    });
                } else {
                    props.entrySet().stream().sorted((a,b)->{
                        String _a = a.getKey().toString();
                        String _b = b.getKey().toString();
                        return _a.compareTo(_b);
                    }).forEach((e)->{
                        sb.append(e.getValue()).append("\n");
                    });
                }
            }
            ReferenceData data = ao.create(
                ReferenceData.class,
                new DBParam("CODE", code),
                new DBParam("NAME", resolver.getText("com.mesilat.seed-data." + name))
            );
            data.setType(ReferenceData.TYPE_LIST_OF_STRINGS);
            data.setData(sb.toString());
            data.save();
        } catch(Throwable ex){
            LOGGER.warn(String.format("Failed to create reference data: %s", name), ex);
        }
    }
    private InputStream getResourceStream(String name) throws IOException {
        Locale locale = localeManager.getSiteDefaultLocale();
        
        InputStream in = getClass().getClassLoader().getResourceAsStream("/i18n/" + name + "_" + locale.toString() + ".properties");
        if (in != null){
            return in;
        }
        return getClass().getClassLoader().getResourceAsStream("/i18n/" + name + ".properties");
    }
    private void createSeedData(String code, String name){
        try {
            String javascript;
            try (InputStream in = getResourceStream(name)) {
                javascript = IOUtils.toString(in, StandardCharsets.UTF_8.name());
            }
            ReferenceData data = ao.create(
                ReferenceData.class,
                new DBParam("CODE", code),
                new DBParam("NAME", resolver.getText("com.mesilat.seed-data." + name))
            );
            data.setType(ReferenceData.TYPE_JAVASCRIPT);
            data.setData(javascript);
            data.save();
        } catch(Throwable ex){
            LOGGER.warn(String.format("Failed to create reference data: %s", name), ex);
        }
    }
    public void putReferenceData(String code, String name, int type, String data) {
        ao.executeInTransaction(()->{
            ReferenceData _data = ao.get(ReferenceData.class, code);
            if (_data == null){
                _data = ao.create(ReferenceData.class, new DBParam("CODE", code), new DBParam("NAME", name));
                _data.setData(data);
                _data.setType(type);
                _data.setStatus(ReferenceData.STATUS_ACTIVE);
                _data.save();
            } else if (_data.getStatus() == ReferenceData.STATUS_ACTIVE) {
                _data.setName(name);
                _data.setData(data);
                _data.setType(type);
                _data.save();
            }
            return null;
        });
    }

    private void registerBean(String name){
        ContainerManager containerManager = ContainerManager.getInstance();
        TenantedContainerContext containerContext = (TenantedContainerContext)containerManager.getContainerContext();
        SeedDataService service = this;
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            try {
                Field f = SpringContainerContext.class.getDeclaredField("beanFactory");
                f.setAccessible(true);
                AtlassianBeanFactory factory = (AtlassianBeanFactory)f.get(containerContext);
                Method m = AtlassianBeanFactory.class.getMethod("registerSingleton", String.class, Object.class);
                m.invoke(factory, name, service);
            } catch (Throwable ex) {
                LOGGER.warn("Failed to register singleton bean", ex);
            }
            return null;
        });
    }
    private void unregisterBean(String name){
        ContainerManager containerManager = ContainerManager.getInstance();
        TenantedContainerContext containerContext = (TenantedContainerContext)containerManager.getContainerContext();
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            try {
                Field f = SpringContainerContext.class.getDeclaredField("beanFactory");
                f.setAccessible(true);
                AtlassianBeanFactory factory = (AtlassianBeanFactory)f.get(containerContext);
                Method m = AtlassianBeanFactory.class.getMethod("destroySingleton", String.class);
                m.invoke(factory, name);
            } catch (Throwable ex) {
                LOGGER.warn("Failed to unregister singleton bean", ex);
            }
            return null;
        });
    }
    
    @Inject
    public SeedDataService(
        final @ComponentImport ActiveObjects ao,
        final @ComponentImport LocaleManager localeManager,
        final @ComponentImport I18nResolver resolver,
        final @ComponentImport CountriesService countriesService,
        final @ComponentImport CurrenciesService currenciesService
    ){
        this.ao = ao;
        this.localeManager = localeManager;
        this.resolver = resolver;
        this.countriesService = countriesService;
        this.currenciesService = currenciesService;
    }
}