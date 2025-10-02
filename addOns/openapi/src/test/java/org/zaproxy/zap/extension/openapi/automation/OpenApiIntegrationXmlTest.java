package org.zaproxy.zap.extension.openapi.automation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.parosproxy.paros.Constant;
import org.zaproxy.addon.automation.AutomationProgress;
import org.zaproxy.addon.automation.ContextWrapper;
import org.zaproxy.zap.extension.openapi.ExtensionOpenApi;
import org.zaproxy.zap.extension.openapi.converter.swagger.OperationModel;
import org.zaproxy.zap.extension.openapi.converter.swagger.RequestModelConverter;
import org.zaproxy.zap.extension.openapi.generators.Generators;
import org.zaproxy.zap.model.Context;
import org.zaproxy.zap.testutils.TestUtils;

class OpenApiIntegrationXmlTest extends TestUtils {

    @BeforeEach
    void setUp() {
        mockMessages(new ExtensionOpenApi());
        Constant.messages = null; // leave default initialized by TestUtils/mockMessages
    }

    @Test
    void shouldGenerateXmlRequestBodiesAndNoUnsupportedMessage() throws Exception {
        String defn = IOUtils.toString(
                this.getClass()
                        .getResourceAsStream(
                                "/org/zaproxy/zap/extension/openapi/v3/openapi_xml_integration.yaml"),
                StandardCharsets.UTF_8);

        ParseOptions options = new ParseOptions();
        options.setResolveFully(true);
        OpenAPI openAPI = new OpenAPIV3Parser().readContents(defn, new ArrayList<>(), options).getOpenAPI();

        Generators generators = new Generators(null);
        OperationModel operationModel = new OperationModel("/xml", openAPI.getPaths().get("/xml").getPost(), null);

        RequestModelConverter converter = new RequestModelConverter();
        String body = converter.convert(operationModel, generators).getBody();

        // Body should be non-empty and should look like XML
        org.junit.jupiter.api.Assertions.assertNotNull(body);
        org.junit.jupiter.api.Assertions.assertFalse(body.isEmpty());
        // Quick sanity parse
        javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
        db.parse(new java.io.ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)));

        // There should be no unsupported-content error message for application/xml
        assertThat(
                generators.getErrorMessages().stream()
                        .filter(s -> s.contains("the content type application/xml is not supported"))
                        .toList(),
                empty());

        // The overall error messages list may be empty; we've already asserted the
        // specific
        // unsupported-content message is not present above.
    }
}
