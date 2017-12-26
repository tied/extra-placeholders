package com.mesilat.lov;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.message.I18nResolver;
import javax.inject.Inject;

@Scanned
public class SettingsAction extends ConfluenceActionSupport {
    private final I18nResolver resolver;

    public String getPageTitle(){
        return resolver.getText("com.mesilat.lov-placeholder.settings.title");
    }
    public String getBaseUrl(){
        return settingsManager.getGlobalSettings().getBaseUrl();
    }

    @Override
    public String execute() throws Exception {
        ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        if (!permissionManager.hasPermission(user, Permission.ADMINISTER, PermissionManager.TARGET_APPLICATION)) {
            return "error";
        } else {
            return "success";
        }
    }

    @Inject
    public SettingsAction(final @ComponentImport I18nResolver resolver){
        this.resolver = resolver;
    }
}