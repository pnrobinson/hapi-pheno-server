package starter.mdm;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.mdm.config.MdmConsumerConfig;
import ca.uhn.fhir.jpa.mdm.config.MdmSubmitterConfig;
import ca.uhn.fhir.mdm.api.IMdmSettings;
import ca.uhn.fhir.mdm.rules.config.MdmRuleValidator;
import ca.uhn.fhir.mdm.rules.config.MdmSettings;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import starter.AppProperties;

import java.io.IOException;

@Configuration
@Conditional(MdmConfigCondition.class)
@Import({MdmConsumerConfig.class, MdmSubmitterConfig.class})
public class MdmConfig {

	@Bean
	MdmRuleValidator mdmRuleValidator(FhirContext theFhirContext, ISearchParamRegistry theSearchParamRegistry) {
		return new MdmRuleValidator(theFhirContext, theSearchParamRegistry);
	}

	@Bean
	IMdmSettings mdmSettings(@Autowired MdmRuleValidator theMdmRuleValidator, AppProperties appProperties) throws IOException {
		DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
		Resource resource = resourceLoader.getResource("mdm-rules.json");
		String json = IOUtils.toString(resource.getInputStream(), Charsets.UTF_8);
		return new MdmSettings(theMdmRuleValidator).setEnabled(appProperties.getMdm_enabled()).setScriptText(json);
	}

}
