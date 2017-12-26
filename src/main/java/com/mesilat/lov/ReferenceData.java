package com.mesilat.lov;

import net.java.ao.RawEntity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Unique;

/*
 * AO_38A5E3_REFERENCE_DATA
 */
public interface ReferenceData extends RawEntity<String> {
    public static final int TYPE_LIST_OF_STRINGS = 0;
    public static final int TYPE_JAVASCRIPT = 1;

    public static final int STATUS_DELETED = 0;
    public static final int STATUS_ACTIVE = 1;
    public static final int STATUS_EDITED = 2;

    @NotNull
    @PrimaryKey(value = "CODE")
    public String getCode();
    void setCode(String code);

    @NotNull
    @StringLength(30)
    @Unique
    String getName();
    void setName(String name);

    int getType();
    void setType(int type);

    @StringLength(StringLength.UNLIMITED)
    String getData();
    void setData(String data);

    int getStatus();
    void setStatus(int status);
}