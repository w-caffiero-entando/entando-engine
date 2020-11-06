/*
 * Copyright 2020-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.entando.web.analysis;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;

public class AnalysisResponse {

    @JsonValue
    private final Map<String, Map<String, String>> report;

    AnalysisResponse(Map<String, Map<String, String>> report) {
        this.report = report;
    }

    public enum Status {
        NEW, DIFF, EQUAL
    }

    public enum ObjectType {
        widgets, fragments, pages, pageTemplates, //NOSONAR
        categories, groups, labels, languages, resources,   //NOSONAR
    }
}
