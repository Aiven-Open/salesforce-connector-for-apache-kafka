package io.aiven.kafka.connect.salesforce.config;

import io.aiven.commons.kafka.config.docs.ConfigDefBean;
import io.aiven.commons.kafka.config.docs.ConfigDefBeanFactory;
import io.aiven.commons.kafka.config.docs.ExtendedConfigKeyBean;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class SalesforceSourceConfigDefTest {

	@Test
	void testBeanDefFactory() {
		ConfigDefBeanFactory factory = new ConfigDefBeanFactory();
		ConfigDefBean<ExtendedConfigKeyBean> configDefBean = factory.open(SalesforceSourceConfigDef.class.getName());
		List<String> lst = configDefBean.configKeys().stream().filter(x -> Objects.nonNull(x.since()))
				.filter(x -> x.since().contains(":")).map(x -> String.format("%s %s", x.getName(), x.since())).toList();
		assertThat(lst).isEmpty();
	}
}
