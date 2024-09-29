package com.github.t1;

import com.github.tomakehurst.wiremock.extension.MappingsLoaderExtension;
import com.github.tomakehurst.wiremock.stubbing.StubMappings;
import lombok.extern.jbosslog.JBossLog;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;

@JBossLog
public class NameServiceStandardMappings implements MappingsLoaderExtension {
    @Override
    public String getName() {
        return "name-service-mappings";
    }

    @Override
    public void loadMappingsInto(StubMappings stubMappings) {
        log.info("Loading standard mappings for name service into wiremock");
        stubMappings.addMapping(get("/foo").willReturn(okJson("{\"foo\":\"bar\"}")).build());
    }
}
