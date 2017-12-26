package com.mesilat.lov;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import javax.inject.Inject;
import javax.inject.Named;

@ExportAsService ({ReferenceDataService.class})
@Named ("refDataService")
public class ReferenceDataServiceImpl implements ReferenceDataService {
    private final ActiveObjects ao;

    @Override
    public ReferenceData find(String name) {
        return ao.executeInTransaction(()->{
            ReferenceData[] data = ao.find(ReferenceData.class, "NAME = ?", name);
            if (data.length > 0){
                return data[0];
            } else {
                return null;
            }
        });
    }

    @Inject
    public ReferenceDataServiceImpl(final @ComponentImport ActiveObjects ao){
        this.ao = ao;
    }
}